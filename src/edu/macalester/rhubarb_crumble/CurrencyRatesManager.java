package edu.macalester.rhubarb_crumble;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
// The CurrencyRatesManager class is responsible for creating a thread which
// manages providing requested currency rates to the CurrencyConverterActivity.
// This includes downloading new currency rates when needed, as well as keeping
// the local SQLite database updated.
//
public class CurrencyRatesManager implements java.lang.Runnable, Handler.Callback {
	private static final String[] currency_abbreviations = {
		"AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN",
		"BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL",
		"BSD", "BTN", "BWP", "BYR", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY",
		"COP", "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD",
		"EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GGP", "GHS",
		"GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF",
		"IDR", "ILS", "IMP", "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "JOD",
		"JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT",
		"LAK", "LBP", "LKR", "LRD", "LSL", "LTL", "LVL", "LYD", "MAD", "MDL",
		"MGA", "MKD", "MMK", "MNT", "MOP", "MRO", "MUR", "MVR", "MWK", "MXN",
		"MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB",
		"PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB",
		"RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLL", "SOS",
		"SPL", "SRD", "STD", "SVC", "SYP", "SZL", "THB", "TJS", "TMT", "TND",
		"TOP", "TRY", "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU",
		"UZS", "VEF", "VND", "VUV", "WST", "XAF", "XCD", "XDR", "XOF", "XPF",
		"YER", "ZAR", "ZMK", "ZWD",
	};

	static {
		java.util.Arrays.sort(currency_abbreviations);
	}

	private static final int SECONDS_PER_AUTOMATIC_UPDATE = 3600;
	private static final String YAHOO_URL = "http://download.finance.yahoo.com/d/quotes.csv";
	private static final int URL_CONNECTION_CONNECT_TIMEOUT_MILLISECONDS = 3000;
	private static final int URL_CONNECTION_READ_TIMEOUT_MILLISECONDS = 3000;
	private static final String TAG = "CurrencyRatesManager";

	public static final int MSG_TIME_TO_DOWNLOAD = 0;
	public static final int MSG_NEED_CURRENCY_RATE = 1;
	public static final int MSG_HAVE_CURRENCY_RATE = 2;

	private Map<String, ExchangeRate> exchange_rates;
	private Context ctx;
	private boolean thread_started;
	private Object thread_started_cond;
	private Handler handler;
	private Handler activity_handler;
	private SQLiteDatabase db;
	private CurrencyDBOpenHelper db_helper;

	// Helper class to create the database if it doesn't already exist.
	private static class CurrencyDBOpenHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 12;
		private static final String DATABASE_NAME = "currencies.db";
		private static final String CURRENCY_TABLE = "currency";
		CurrencyDBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE currency (abbreviation CHAR(3), "
						+ "usd_equivalent DOUBLE, last_updated BIGINT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS currency");
			onCreate(db);
		}
	};

	// Launch the CurrencyRatesManager in a new thread, wait for it to start,
	// then send it a message telling it to download new currency exchange
	// rates.
	public CurrencyRatesManager(Context ctx, Handler activity_handler) {
		this.ctx = ctx;
		this.thread_started = false;
		this.thread_started_cond = new Object();
		this.activity_handler = activity_handler;
		synchronized(thread_started_cond) {
			new java.lang.Thread(this).start();
			while (!this.thread_started) {
				try {
					Log.d(TAG, "Waiting for CurrencyRatesManager thread to start...");
					thread_started_cond.wait();
					Log.d(TAG, "CurrencyRatesManager thread has been started.");
				} catch (InterruptedException e) {
				}
			}
		}
		Message msg = Message.obtain();
		msg.what = MSG_TIME_TO_DOWNLOAD;
		this.handler.sendMessage(msg);
	}

	// Close the database.
	public void finalize() {
		if (this.db_helper != null) {
			this.db_helper.close();
			this.db_helper = null;
		}
	}

	// Retrieves an android.os.Handler instance that can be used to send a
	// message to the CurrencyRatesManager thread.
	public Handler getHandler() {
		return this.handler;
	}

	// Returns an array containing the list of supported currency abbreviations.
	public final String[] getCurrencyAbbreviations() {
		return currency_abbreviations;
	}

	// Loads currency exchange rates from the local SQLite database.
	private void load_rates_from_db(SQLiteDatabase db) {
		Cursor cur = db.rawQuery("SELECT * FROM currency", null);
		if (cur.moveToFirst()) {
			int count = cur.getCount();
			Log.i(TAG, "Loading " + count + " exchange rates from local SQLite database");
			int abbreviation_idx = cur.getColumnIndex("abbreviation");
			int usd_equivalent_idx = cur.getColumnIndex("usd_equivalent");
			int last_updated_idx = cur.getColumnIndex("last_updated");
			for (int i = 0; i < count; i++) {
				String abbreviation = cur.getString(abbreviation_idx);
				double usd_equivalent = cur.getDouble(usd_equivalent_idx);
				long last_updated = cur.getLong(last_updated_idx);
				if (!abbreviation.equals("USD")) {
					Log.d(TAG, "Loaded (" + abbreviation + ", " + usd_equivalent +
						  ", " + last_updated + ")");
					ExchangeRate r = new ExchangeRate(usd_equivalent, last_updated, true);
					exchange_rates.put(abbreviation, r);
				}
				cur.moveToNext();
			}
		} else {
			Log.i(TAG, "No currency exchange rates found in local SQLite database!");
		}
		cur.close();
	}

	// Downloads new currency exchange rates from Yahoo.
	private void download_rates() throws MalformedURLException, IOException {
		String url_str = YAHOO_URL + "?";
		for (String abbrev : this.currency_abbreviations) {
			if (!abbrev.equals("USD")) {
				url_str += "s=" + abbrev + "USD=X&";
			}
		}
		// format = symbol, last trade
		url_str += "f=sl1";
		URL url = new URL(url_str);
		Log.i(TAG, "Downloading currency exchange rates from " + YAHOO_URL);

		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(URL_CONNECTION_CONNECT_TIMEOUT_MILLISECONDS);
		connection.setReadTimeout(URL_CONNECTION_READ_TIMEOUT_MILLISECONDS);
		BufferedReader in = new BufferedReader(
								new InputStreamReader(
										connection.getInputStream()));
		Log.d(TAG, "Created BufferedReader to read from HttpURLConnection");
		long now = new Date().getTime();
		Pattern pat = Pattern.compile("^\\s*\"?([A-Z]{3})USD=X\"?\\s*,\\s*([^\\s,]+)\\s*$");
		long num_rates = 0;
		String line;
		while ((line = in.readLine()) != null) {
			//Log.d(TAG, line);
			Matcher m = pat.matcher(line);
			if (m.matches()) {
				String abbrev = m.group(1);
				double usd_equivalent;
				try {
					usd_equivalent = Double.parseDouble(m.group(2));
				} catch (NumberFormatException e) {
					Log.w(TAG, "Can't parse string as double: \"" + m.group(2) +
							   "\" from line \"" + line + "\"");
					continue;
				}
				ExchangeRate r;
				if ((r = exchange_rates.get(abbrev)) != null) {
					exchange_rates.put(abbrev, new ExchangeRate(usd_equivalent, now, r.is_in_db));
					num_rates++;
				} else {
					Log.w(TAG, "Unknown currency abbreviation \"" + abbrev + "\"");
					continue;
				}
			} else {
				Log.w(TAG, "Cannot parse exchange rates from line: \"" + line + "\"");
			}
		}
		Log.i(TAG, "Updated exchange rates for " + num_rates + " currencies");
	}

	// Returns true iff any currency exchange rates are missing or more than
	// SECONDS_PER_AUTOMATIC_UPDATE seconds old.
	private boolean any_rates_outdated() {
		long now = new Date().getTime();
		long oldest_update = now;
		String oldest = null;
		for (Map.Entry<String, ExchangeRate> entry : exchange_rates.entrySet()) {
			String s = entry.getKey();
			ExchangeRate r = entry.getValue();
			if (r.last_updated < oldest_update && !s.equals("USD")) {
				oldest_update = r.last_updated;
				oldest = s;
			}
		}
		if (oldest == null || now - oldest_update <=
							  1000 * SECONDS_PER_AUTOMATIC_UPDATE) {
			return false;
		} else {
			Log.d(TAG, "Exchange rate of " + oldest + " is more than " +
				  (now - oldest_update) / 1000 + " seconds old");
			return true;
		}
	}

	// Saves the currency exchange rates into the local SQLite database.
	private void update_db(Map<String, ExchangeRate> exchange_rates,
						   SQLiteDatabase db)
	{
		DatabaseUtils.InsertHelper ih = new DatabaseUtils.InsertHelper(db, "currency");
		final int abbreviation_col = ih.getColumnIndex("abbreviation");
		final int usd_equivalent_col = ih.getColumnIndex("usd_equivalent");
		final int last_updated_col = ih.getColumnIndex("last_updated");

		try {
			db.beginTransaction();
			for (Map.Entry<String, ExchangeRate> entry : exchange_rates.entrySet()) {
				String abbrev = entry.getKey();
				ExchangeRate r = entry.getValue();
				if (!abbrev.equals("USD")) {
					ih.prepareForReplace();
					ih.bind(abbreviation_col, abbrev);
					ih.bind(usd_equivalent_col, r.usd_equivalent);
					ih.bind(last_updated_col, r.last_updated);

					Log.d(TAG, "Insert: (abbreviation = " + abbrev + ", usd_equivalent = " +
						  r.usd_equivalent + ", last_updated = " + r.last_updated + ")");

					ih.execute();
				}
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (SQLiteException e) {
			Log.e(TAG, "Error updating exchange rate database", e);
		}
	}

	// Downloads new currency exchange rates, then saves them into the local
	// SQLite database.
	private void download_and_update_rates() {
		try {
			download_rates();
			if (this.db != null)
				update_db(this.exchange_rates, this.db);
		} catch (Exception e) {
			Log.e(TAG, "Error downloading currency exchange rates", e);
		}
	}

	// Callback when a message is sent to us by the CurrencyConverterActivity
	// thread.
	public boolean handleMessage(Message msg) {
		Message nmsg;
		switch (msg.what) {
		case MSG_TIME_TO_DOWNLOAD:
			// Time to download new currency exchange rates!
			Log.d(TAG, "Received message MSG_TIME_TO_DOWNLOAD");
			if (any_rates_outdated()) {
				Log.d(TAG, "One or more exchange rates is more than " +
					  SECONDS_PER_AUTOMATIC_UPDATE + " seconds old");
				download_and_update_rates();
			} else {
				Log.d(TAG, "No exchange rates are more than " +
						   SECONDS_PER_AUTOMATIC_UPDATE + " seconds old");
			}
			Log.d(TAG, "Sending message MSG_TIME_TO_DOWNLOAD in " +
					SECONDS_PER_AUTOMATIC_UPDATE + " seconds");
			nmsg = Message.obtain();
			nmsg.what = MSG_TIME_TO_DOWNLOAD;
			nmsg.arg1 = msg.arg1;
			nmsg.arg2 = msg.arg2;
			// In SECONDS_PER_AUTOMATIC_UPDATE seconds, do this again.
			this.handler.sendMessageDelayed(nmsg, SECONDS_PER_AUTOMATIC_UPDATE * 1000);
			break;
		case MSG_NEED_CURRENCY_RATE:
			// Need to get a specific currency exchange rate.  If it's up to
			// date, it's immediately sent back.  Otherwise, currency exchange
			// rates are downloaded.  It's possible for the rate to still be out
			// of date if the download fails, but a message is sent back to the
			// activity thread in any case.
			Log.d(TAG, "Received message MSG_NEED_CURRENCY_RATE");
			String currency_abbrev = (String)msg.obj;
			long now = new Date().getTime();
			Log.d(TAG, "Getting exchange rate for currency " + currency_abbrev);
			ExchangeRate r = exchange_rates.get(currency_abbrev);
			if ((now - r.last_updated) / 1000 >= SECONDS_PER_AUTOMATIC_UPDATE
				&& !currency_abbrev.equals("USD"))
			{
				Log.d(TAG, "Exchange rate for " + currency_abbrev +
						   " is out of date " + "by " +
						   (now - r.last_updated) / 1000 + " seconds!");
				long outdated_time = r.last_updated;

				download_and_update_rates();

				r = exchange_rates.get(currency_abbrev);
				r.is_outdated = r.last_updated == outdated_time;
				if (r.is_outdated)
					Log.d(TAG, "Exchange rate for " + currency_abbrev +
							   " is still outdated!");
				else
					Log.d(TAG, "Exchange rate for " + currency_abbrev +
							   " was updated");
			} else {
				Log.d(TAG, "Exchange rate for " + currency_abbrev +
						   " is up to date");
				r.is_outdated = false;
			}
			nmsg = Message.obtain();
			nmsg.what = MSG_HAVE_CURRENCY_RATE;
			r.abbrev = currency_abbrev;
			nmsg.arg1 = msg.arg1;
			nmsg.arg2 = msg.arg2;
			nmsg.obj = r;
			Log.d(TAG, "Sending message MSG_HAVE_CURRENCY_RATE (" +
						currency_abbrev + ", usd_equivalent=" +
						r.usd_equivalent + ")");
			activity_handler.sendMessage(nmsg);
			break;
		}
		return true;
	}

	// Entry point for the CurrencyRatesManager thread
	public void run() {
		synchronized(this.thread_started_cond) {
			// Set up a message queue, then notify the CurrencyConverterActivity
			// thread that this thread has been started.
			Looper.prepare();
			this.handler = new Handler(this);
			this.thread_started = true;
			this.thread_started_cond.notify();
		}

		// Load dummy exchange rates into the exchange_rates map.
		this.exchange_rates = new HashMap<String, ExchangeRate>();
		for (int i = 0; i < this.currency_abbreviations.length; i++) {
			this.exchange_rates.put(this.currency_abbreviations[i],
									new ExchangeRate());
		}
		ExchangeRate usd = exchange_rates.get("USD");
		usd.usd_equivalent = 1.0;
		usd.last_updated = new Date().getTime();

		// Open/create the database and load the exchange rates.
		try {
			Log.d(TAG, "Creating CurrencyDBOpenHelper");
			this.db_helper = new CurrencyDBOpenHelper(ctx);
			Log.d(TAG, "Calling SQLiteOpenHelper.getWritableDatabase()");
			this.db = this.db_helper.getWritableDatabase();
			load_rates_from_db(this.db);
		} catch (Exception e) {
			Log.e(TAG, "Error opening, creating, or reading database", e);
			this.db = null;
		}
		// Enter the message loop.  Next function called will be
		// handleMessage().
		Looper.loop();
	}
}
