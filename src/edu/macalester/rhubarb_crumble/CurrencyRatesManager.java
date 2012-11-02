package edu.macalester.rhubarb_crumble;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;
import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CurrencyRatesManager implements java.lang.Runnable {
	private final String[] currency_abbreviations = {
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

	private static final int SECONDS_PER_AUTOMATIC_UPDATE = 3600;
	private static final String YAHOO_URL = "http://download.finance.yahoo.com/d/quotes.csv";
	private static final int URL_CONNECTION_CONNECT_TIMEOUT_MILLISECONDS = 3000;
	private static final int URL_CONNECTION_READ_TIMEOUT_MILLISECONDS = 3000;
	private static final String TAG = "CurrencyRatesManager";
	private static final String ACTIVITY_TAG = "CurrencyConverterActivity";

	private Map<String, ExchangeRate> exchange_rates;
	private Context ctx;
	private boolean thread_started;
    private Object thread_started_cond;
	private boolean update_done;

	private static class CurrencyDBOpenHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "currencies.db";
		private static final String CURRENCY_TABLE = "currency";
		CurrencyDBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE currency (abbreviation CHAR(3), "
						+ "usd_equivalent DOUBLE, last_updated BIGINT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS currency;");
			onCreate(db);
		}
	};

	public CurrencyRatesManager(Context ctx) {
		this.ctx = ctx;
		this.thread_started = false;
        this.thread_started_cond = new Object();
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
	}

	public final String[] getCurrencyAbbreviations() {
		return currency_abbreviations;
	}

	public ExchangeRate getExchangeRate(String currency_abbrev) {
		ExchangeRate r;
		long now = new Date().getTime();
		long outdated_time;
		Log.d(ACTIVITY_TAG, "Getting exchange rate for currency " + currency_abbrev);
		synchronized(this) {
			r = exchange_rates.get(currency_abbrev);
			if ((now - r.last_updated) / 1000 >= SECONDS_PER_AUTOMATIC_UPDATE) {
				Log.d(ACTIVITY_TAG, "Exchange rate for " + currency_abbrev +
									" is out of date " + "by " +
									(now - r.last_updated) / 1000 + " seconds!");
				outdated_time = r.last_updated;
				r = null;
				update_done = false;
				this.notify();

				while (!update_done) {
					try {
						Log.d(ACTIVITY_TAG, "Waiting for currency rates to be downloaded...");
						this.wait();
					} catch (InterruptedException e) {
					}
				}
				r = exchange_rates.get(currency_abbrev);
				r.is_outdated = r.last_updated == outdated_time;
				if (r.is_outdated)
					Log.d(ACTIVITY_TAG, "Exchange rate for " + currency_abbrev +
										" is still outdated!");
				else
					Log.d(ACTIVITY_TAG, "Exchange rate for " + currency_abbrev +
										" was updated");
			} else {
				Log.d(ACTIVITY_TAG, "Exchange rate for " + currency_abbrev +
									" is up to date");
				r.is_outdated = false;
			}
		}
		if (r.last_updated == 0) {
			Log.w(ACTIVITY_TAG, "No exchange rate available for " + currency_abbrev);
			return null;
		} else {
			return r;
		}
	}

	private void load_rates_from_db(SQLiteDatabase db) {
		//Cursor cur = db.query("currency", null, null, null, null, null, null);
		Cursor cur = db.rawQuery("SELECT * FROM currency;", null);
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
                Log.d(TAG, "Loaded (" + abbreviation + ", " + usd_equivalent +
                      ", " + last_updated + ")");
				ExchangeRate r = new ExchangeRate(usd_equivalent, last_updated, true);
				exchange_rates.put(abbreviation, r);
			}
		} else {
			Log.i(TAG, "No currency exchange rates found in local SQLite database!");
		}
	}

	private void download_rates() throws MalformedURLException, IOException {
		String url_str = YAHOO_URL + "?";
		for (String abbrev : this.currency_abbreviations) {
			url_str += "s=" + abbrev + "USD=X&";
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

	private boolean any_rates_outdated() {
		long now = new Date().getTime();
		long oldest_update = now;
        String oldest = null;
		for (Map.Entry<String, ExchangeRate> entry : exchange_rates.entrySet()) {
            String s = entry.getKey();
            ExchangeRate r = entry.getValue();
            if (r.last_updated < oldest_update) {
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

    private void update_db(Map<String, ExchangeRate> exchange_rates,
                           SQLiteDatabase db) {
        for (Map.Entry<String, ExchangeRate> entry : exchange_rates.entrySet()) {
            String abbrev = entry.getKey();
            ExchangeRate r = entry.getValue();
            try {
                if (r.is_in_db) {
                    db.execSQL("UPDATE currency SET usd_equivalent=" +
                                r.usd_equivalent + ", last_updated=" +
                                r.last_updated + " WHERE " +
                                "abbreviation = '" + abbrev + "';");
                } else {
                    db.execSQL("INSERT INTO currency VALUES ("
                               + "'" + abbrev + "', " +
                               r.usd_equivalent + ", " +
                               r.last_updated + ");");
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "Error updating exchange rate database", e);
            }
        }
    }

	public void run() {
		synchronized(this) {
            synchronized(this.thread_started_cond) {
                this.thread_started = true;
                thread_started_cond.notify();
            }
			this.exchange_rates = new HashMap<String, ExchangeRate>();
			for (int i = 0; i < currency_abbreviations.length; i++) {
				this.exchange_rates.put(currency_abbreviations[i],
										new ExchangeRate());
			}
			Log.d(TAG, "Creating CurrencyDBOpenHelper");
			CurrencyDBOpenHelper helper = new CurrencyDBOpenHelper(ctx);
			SQLiteDatabase db;
			try {
				Log.d(TAG, "Calling SQLiteOpenHelper.getWritableDatabase()");
				db = helper.getWritableDatabase();
				load_rates_from_db(db);
			} catch (Exception e) {
				Log.e(TAG, "Error opening database", e);
				return;
			}

			while (true) {
				if (any_rates_outdated()) {
					Log.d(TAG, "One or more exchange rates is more than " +
						  SECONDS_PER_AUTOMATIC_UPDATE + " seconds old");
					try {
						download_rates();
                        update_db(exchange_rates, db);
					} catch (Exception e) {
						Log.e(TAG, "Error downloading currency exchange rates", e);
					}
					this.update_done = true;
					this.notify();
				} else {
					Log.d(TAG, "No exchange rates are more than " +
							   SECONDS_PER_AUTOMATIC_UPDATE + " seconds old");
				}
				try {
					Log.d(TAG, "Sleeping for at most " + SECONDS_PER_AUTOMATIC_UPDATE +
							   " seconds");
					this.wait(SECONDS_PER_AUTOMATIC_UPDATE * 1000);
				} catch (InterruptedException e) {
                    Log.e(TAG, "Thread interrupted!");
				}
			}
		}
	}
}
