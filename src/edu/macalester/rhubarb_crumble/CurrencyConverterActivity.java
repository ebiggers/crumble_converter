package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

// The activity for the currency converter.
public class CurrencyConverterActivity extends Activity
			 implements OnItemSelectedListener, TextWatcher, Handler.Callback {

	// The instance of CurrencyRatesManager that is in charge of managing the
	// currency exchange rates.
	private CurrencyRatesManager rates_manager;

	// 0-based indices (in the currency abbreviations array) of the currently
	// selected "to" and "from" currencies, or -1 if nothing is currently
	// selected.
	private int from_currency_idx;
	private int to_currency_idx;

	// Exchange rates for the currently selected "to" and "from" currencies, or
	// null if nothing selected or rate not available yet.
	private ExchangeRate from_exchange_rate;
	private ExchangeRate to_exchange_rate;

	// Amount currently entered in the input field, if cur_amount_valid is true.
	private double cur_amount;
	private boolean cur_amount_valid;

	// Sorted array of currency abbreviations (gotten from the
	// CurrencyRatesManager class)
	private String[] currency_abbrevs;

	// Adapter for the currency selection spinners
	private ArrayAdapter adapter;

	// Represents a currency that was recently converted.
	private static class RecentCurrency {
		public String abbrev;
		public int abbrev_idx;
		public ExchangeRate rate;

		public RecentCurrency() {
		}

		public RecentCurrency(String abbrev, int abbrev_idx) {
			this.abbrev = abbrev;
			this.abbrev_idx = abbrev_idx;
			this.rate = null;
		}
	};

	// TextViews that show exchange rates for the recently converted currencies
	private TextView[] recent_currency_textviews;
	private TextView[] last_updated_textviews;

	// List of recently converted currencies
	private ArrayList<RecentCurrency> recent_currencies;

	// Maximum number of recently converted currencies
	private static final int NUM_RECENT_CURRENCIES = 3;

	// Distinguish rates that were required for a user-requested version, versus
	// being shown on the list of exchange rates of recent currencies
	private static final int NEED_RATE_FOR_CONVERSION = 0;
	private static final int NEED_RATE_FOR_LAST_CURRENCY_LIST = 1;

	// Logging message tag for this class.
	private static final String TAG = "CurrencyConverterActivity";

	// List of important currencies that will appear at the top of the list, in
	// addition to in alphabetical order.
	private static final String[] important_currencies =
		{"USD", "EUR", "GBP", "JPY", "CAD", "MXN", "HKD", "CNY"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the view to currency_converter.xml
		setContentView(R.layout.currency_converter);

		// Start a new thread that will be responsible for managing currency
		// exchange rates.
		Handler activity_handler = new Handler(this);
		this.rates_manager = new CurrencyRatesManager(this, activity_handler);

		// Get the currency abbreviations
		this.currency_abbrevs = rates_manager.getCurrencyAbbreviations();

		// Load the list of long names that correspond to the currency
		// abbreviations
		ArrayList<String> currency_choices = load_currency_names(this.currency_abbrevs);

		// Initialize an ArrayAdapter from the currency long names, and set both
		// spinners to be backed by the ArrayAdapter.
		this.adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
										currency_choices);

		this.adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner from_spinner = (Spinner)findViewById(R.id.from_currency_spinner);
		Spinner to_spinner = (Spinner)findViewById(R.id.to_currency_spinner);

		add_important_currencies_to_adapter();

		// Set callbacks on the from-currency and to-currency spinners
		from_spinner.setAdapter(adapter);
		from_spinner.setOnItemSelectedListener(this);
		from_spinner.setSelection(0);
		to_spinner.setAdapter(adapter);
		to_spinner.setOnItemSelectedListener(this);
		to_spinner.setSelection(0);

		// Initialize callback for the numeric input field
		EditText edit_text = (EditText)findViewById(R.id.currency_converter_edit_text);
		edit_text.addTextChangedListener(this);

		// Inialize the variables for the current user-entered information.
		this.from_currency_idx = -1;
		this.to_currency_idx = -1;
		this.from_exchange_rate = null;
		this.to_exchange_rate = null;
		this.cur_amount_valid = false;

		// Show the exchange rates for the most recently converted currencies
		load_recent_currencies();
	}

	// Pause the activity, saving the list of recent currencies.
	@Override
	protected void onPause() {
		super.onPause();
		if (recent_currencies != null) {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.clear();
			for (int i = 0; i < recent_currencies.size(); i++) {
				String key = "recent_currency_" + i;
				RecentCurrency cur = recent_currencies.get(i);
				editor.putString(key, cur.abbrev);
			}
			editor.commit();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putDouble("cur_amount", cur_amount);
		savedInstanceState.putBoolean("cur_amount_valid", cur_amount_valid);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		cur_amount = savedInstanceState.getDouble("cur_amount");
		cur_amount_valid = savedInstanceState.getBoolean("cur_amount_valid");
		if (cur_amount_valid)
			maybe_do_conversion();
	}

	// Make the spinners show some important currencies at the very top of the
	// list, so the user doesn't need to scroll down to find them.
	private void add_important_currencies_to_adapter() {
		for (int i = 0; i < important_currencies.length; i++) {
			String abbrev = important_currencies[important_currencies.length - 1 - i];
			int abbrev_idx = Arrays.binarySearch(currency_abbrevs, abbrev);
			String fullName = (String)this.adapter.getItem(abbrev_idx + i);
			this.adapter.insert(fullName, 0);
		}
	}

	// Ask the CurrencyRatesManager for the exchange rate for a currency.
	private void request_exchange_rate(int abbrev_idx, int reason) {
		String abbrev = currency_abbrevs[abbrev_idx];
		Log.d(TAG, "Sending message MSG_NEED_CURRENCY_RATE (" + abbrev + ")");
		Message msg = Message.obtain();
		msg.obj = currency_abbrevs[abbrev_idx];
		msg.what = CurrencyRatesManager.MSG_NEED_CURRENCY_RATE;
		msg.arg1 = abbrev_idx;
		msg.arg2 = reason;
		rates_manager.getHandler().sendMessage(msg);
	}

	// Load the recently converted currencies from the preferences.
	private void load_recent_currencies() {
		this.recent_currency_textviews = new TextView[NUM_RECENT_CURRENCIES];
		this.last_updated_textviews = new TextView[NUM_RECENT_CURRENCIES];

		this.recent_currency_textviews[0] = (TextView)findViewById(R.id.recent_currency_1);
		this.recent_currency_textviews[1] = (TextView)findViewById(R.id.recent_currency_2);
		this.recent_currency_textviews[2] = (TextView)findViewById(R.id.recent_currency_3);

		this.last_updated_textviews[0] = (TextView)findViewById(R.id.last_updated_1);
		this.last_updated_textviews[1] = (TextView)findViewById(R.id.last_updated_2);
		this.last_updated_textviews[2] = (TextView)findViewById(R.id.last_updated_3);

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		this.recent_currencies = new ArrayList<RecentCurrency>();

		for (int i = 0; i < NUM_RECENT_CURRENCIES; i++) {
			String abbrev = prefs.getString("recent_currency_" + i, null);
			if (abbrev != null) {
				int abbrev_idx = Arrays.binarySearch(currency_abbrevs, abbrev);
				if (abbrev_idx != -1) {
					Log.d(TAG, "Loaded recent currency " + abbrev);
					this.recent_currencies.add(new RecentCurrency(abbrev, abbrev_idx));
				}
			}
		}

		for (RecentCurrency cur : recent_currencies) {
			request_exchange_rate(cur.abbrev_idx, NEED_RATE_FOR_LAST_CURRENCY_LIST);
		}

		update_recent_rates();
	}

	private String prettyTimeString(long seconds) {
		final long SECONDS_PER_MINUTE = 60;
		final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
		final long SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;
		final long SECONDS_PER_YEAR = SECONDS_PER_DAY * 365;

		long amount;
		String unit;
		String abbrev = null;

		if (seconds < SECONDS_PER_MINUTE) {
			amount = seconds;
			unit = "second";
			abbrev = "sec.";
		} else if (seconds < SECONDS_PER_HOUR) {
			amount = seconds / SECONDS_PER_MINUTE;
			unit = "minute";
			abbrev = "min.";
		} else if (seconds < SECONDS_PER_DAY) {
			amount = seconds / SECONDS_PER_HOUR;
			unit = "hour";
		} else if (seconds < SECONDS_PER_YEAR) {
			amount = seconds / SECONDS_PER_DAY;
			unit = "day";
		} else {
			amount = seconds / SECONDS_PER_YEAR;
			unit = "year";
		}
		if (amount == 1)
			return "1 " + unit;
		else if (amount >= 10 && abbrev != null) {
			return amount + " " + abbrev;
		} else {
			return amount + " " + unit + "s";
		}
	}

	// Update the TextViews that show the rates of the recently converted
	// currencies.
	private void update_recent_rates() {
		for (int i = 0; i < NUM_RECENT_CURRENCIES; i++) {
			String txt = "";
			String txt2 = "";
			if (i < recent_currencies.size()) {
				RecentCurrency cur = recent_currencies.get(i);

				if (cur.rate == null) {
					txt = String.format("waiting for %s to USD rate...", cur.abbrev);
				} else {
					long now = new Date().getTime();
					long seconds_outdated = (now - cur.rate.last_updated) / 1000;
					if (cur.rate.last_updated == 0) {
						txt = String.format("%s to USD", cur.abbrev);
						txt2 = "not available";
					} else {
						txt = String.format("1 %s = %.3f USD", cur.abbrev, cur.rate.usd_equivalent);
						txt2 = "updated " + prettyTimeString(seconds_outdated) + " ago";
					}
					if (seconds_outdated >= 3600) {
						last_updated_textviews[i].setTextColor(
									getResources().getColor(R.color.red));
					} else {
						last_updated_textviews[i].setTextColor(
									getResources().getColor(R.color.white));
					}
				}
			}
			recent_currency_textviews[i].setText(txt);
			last_updated_textviews[i].setText(txt2);
		}
	}


	private void push_recent_rate(String abbrev, int abbrev_idx) {

		if (abbrev.equals("USD"))
			return;
		// If the currency is already in the recent currencies list, move it to
		// the front of the list.
		for (int i = 0; i < recent_currencies.size(); i++) {
			RecentCurrency cur = recent_currencies.get(i);
			if (cur.abbrev.equals(abbrev)) {
				Log.d(TAG, abbrev + " is already in the recent currencies list");
				if (i != 0) {
					Log.d(TAG, "Moving currency " + abbrev + " from slot " + i + " to "
							   + "slot 0 in the recent currencies list");
					recent_currencies.remove(i);
					recent_currencies.add(0, cur);
					update_recent_rates();
				}
				return;
			}
		}

		// Currency is not in the recent currencies list.  Add it to the list,
		// possibly after deleting the last currency in the list.

		RecentCurrency oldest;
		if (NUM_RECENT_CURRENCIES == recent_currencies.size()) {
			oldest = recent_currencies.get(NUM_RECENT_CURRENCIES - 1);
			Log.d(TAG, "Deleting currency " + oldest.abbrev + " from the recent currencies list");
			recent_currencies.remove(NUM_RECENT_CURRENCIES - 1);
		} else {
			oldest = new RecentCurrency();
		}
		Log.d(TAG, "Adding currency " + abbrev + " to the front of the recent currencies list");

		oldest.abbrev = abbrev;
		oldest.abbrev_idx = abbrev_idx;
		oldest.rate = null;
		recent_currencies.add(0, oldest);

		request_exchange_rate(abbrev_idx, NEED_RATE_FOR_LAST_CURRENCY_LIST);

		update_recent_rates();
	}

	// Given a list of currency abbreviations, load their full localized names.
	private ArrayList<String> load_currency_names(String[] currency_abbrevs) {
		int num_currencies;
		ArrayList<String> currency_choices;
		Resources resources;

		num_currencies = currency_abbrevs.length;
		currency_choices = new ArrayList<String>(num_currencies);
		resources = getResources();
		for (int i = 0; i < num_currencies; i++) {
			String abbrev = currency_abbrevs[i];
			String name;

			int id = resources.getIdentifier(currency_abbrevs[i], "string",
											 this.getPackageName());
			try {
				name = abbrev + " (" + resources.getString(id) + ")";
			} catch (Resources.NotFoundException e) {
				Log.w(TAG, "Cannot find full name for currency " + abbrev, e);
				name = abbrev;
			}
			currency_choices.add(name);
		}
		return currency_choices;
	}

	// Set the text in the converter output field.
	private void setConversionOutput(String s) {
		TextView v = (TextView)findViewById(R.id.currency_conversion_output);
		v.setText(s);
	}

	// Process a message received from the CurrencyRatesManager thread.
	public boolean handleMessage(Message msg) {
		int currency_idx;
		switch (msg.what) {
		case CurrencyRatesManager.MSG_HAVE_CURRENCY_RATE:
			ExchangeRate r = (ExchangeRate)msg.obj;
			Log.d(TAG, "Received message MSG_HAVE_CURRENCY_RATE (" + r.abbrev + ")");
			switch (msg.arg2) {
			case NEED_RATE_FOR_CONVERSION:
				// Received a currency rate (it may or may not be outdated.)
				// Possibly do a conversion.
				currency_idx = msg.arg1;
				if (currency_idx == from_currency_idx) {
					from_exchange_rate = r;
				}
				if (currency_idx == to_currency_idx) {
					to_exchange_rate = r;
				}
				maybe_do_outdated_conversion();
				break;
			case NEED_RATE_FOR_LAST_CURRENCY_LIST:
				// Received a currency rate for something in the recent
				// currencies last.
				currency_idx = msg.arg1;
				for (RecentCurrency cur : recent_currencies) {
					if (cur.abbrev_idx == currency_idx) {
						cur.rate = r;
						update_recent_rates();
						break;
					}
				}
				break;
			}
			break;
		}
		return true;
	}

	// Perform a currency conversion and set the output text.
	private void do_conversion() {
		assert(from_exchange_rate != null && to_exchange_rate != null);
		assert(from_currency_idx != -1 && to_currency_idx != -1);
		assert(from_exchange_rate.abbrev.equals(currency_abbrevs[from_currency_idx])
			   && to_exchange_rate.abbrev.equals(currency_abbrevs[to_currency_idx]));

		double amount;
		if (cur_amount_valid)
			amount = cur_amount;
		else
			amount = 1.0;
		String result;

		if (to_exchange_rate.usd_equivalent <= 0.0) {
			Log.e(TAG, "Error: Currency " + from_exchange_rate.abbrev + " is worth 0 or less!");
			result = "Rate for " + to_exchange_rate.abbrev + " not available";
		} else if (from_exchange_rate.usd_equivalent <= 0.0) {
			Log.e(TAG, "Error: Currency " + from_exchange_rate.abbrev + " is worth 0 or less!");
			result = "Rate for " + from_exchange_rate.abbrev + " not available";
		} else {
			double to_from_ratio = from_exchange_rate.usd_equivalent
								   / to_exchange_rate.usd_equivalent;
			double new_amount = amount * to_from_ratio;
			Log.d(TAG, "Converted " + amount + " " + from_exchange_rate.abbrev +
				  " to " + new_amount + " " + to_exchange_rate.abbrev + " at " +
				  to_from_ratio + " " + to_exchange_rate.abbrev + " per " +
				  from_exchange_rate.abbrev);
			result = String.format("%.2f", new_amount);
		}
		setConversionOutput(result);
	}

	// Do a conversion, but only if both exchange rates are available.  It
	// doesn't matter if they are outdated or not.
	private void maybe_do_outdated_conversion() {
		if (from_exchange_rate != null && to_exchange_rate != null) {
			do_conversion();
		}
	}

	// Do a conversion if both exchange rates are up to date.  Otherwise, send a
	// message to the CurrencyRatesManager thread saying we need a new exchange
	// rate.
	private void maybe_do_conversion() {
		Message msg;
		boolean conversion_possible_now = true;

		if (from_currency_idx == -1 || to_currency_idx == -1)
			return;

		if (from_exchange_rate == null || from_exchange_rate.is_out_of_date()) {
			request_exchange_rate(from_currency_idx, NEED_RATE_FOR_CONVERSION);
			conversion_possible_now = false;
		}
		if (to_exchange_rate == null || to_exchange_rate.is_out_of_date()) {
			request_exchange_rate(to_currency_idx, NEED_RATE_FOR_CONVERSION);
			conversion_possible_now = false;
		}

		if (conversion_possible_now) {
			do_conversion();
		} else {
			// XXX
			setConversionOutput("Waiting for exchange rates to be updated");
		}
	}

	// Callback when a "from" or "to" currency is selected.
	public void onItemSelected(AdapterView<?> parent, View view,
							   int position, long id) {

		if (position >= important_currencies.length) {
			position -= important_currencies.length;
		} else {
			position = Arrays.binarySearch(currency_abbrevs,
										   important_currencies[position]);
		}
		push_recent_rate(currency_abbrevs[position], position);
		if (parent.getId() == R.id.from_currency_spinner) {
			Log.d(TAG, "Selected \"from\" currency idx " + position + " (" +
						currency_abbrevs[position] + ")");
			if (from_currency_idx != position) {
				from_currency_idx = position;
				if (from_currency_idx == to_currency_idx)
					from_exchange_rate = to_exchange_rate;
				else
					from_exchange_rate = null;
			}
		} else {
			Log.d(TAG, "Selected \"to\" currency idx " + position + " (" +
						currency_abbrevs[position] + ")");
			if (to_currency_idx != position) {
				to_currency_idx = position;
				if (to_currency_idx == from_currency_idx)
					to_exchange_rate = from_exchange_rate;
				else
					to_exchange_rate = null;
			}
		}
		maybe_do_conversion();
	}

	// Callback when a "from" or "to" currency is deselected
	public void onNothingSelected(AdapterView<?> parent) {
		if (parent.getId() == R.id.from_currency_spinner) {
			Log.d(TAG, "Deselected \"from\" currency");
			from_currency_idx = -1;
			from_exchange_rate = null;
		} else {
			Log.d(TAG, "Deselected \"to\" currency");
			to_currency_idx = -1;
			to_exchange_rate = null;
		}
		setConversionOutput("");
	}

	// Callback when the text in the numeric input field is edited
	public void afterTextChanged(Editable ed) {
		if (ed.length() == 0) {
			cur_amount_valid = false;
		} else {
			String s = ed.toString();
			try {
				cur_amount = Double.parseDouble(s);
				cur_amount_valid = true;
				Log.d(TAG, "Updated currency amount to " + cur_amount);
			} catch (NumberFormatException e) {
				cur_amount_valid = false;
				Log.d(TAG, "Failed to convert \"" + s + "\" into a double");
			}
		}
		maybe_do_conversion();
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
								  int after) {
		// Handled in afterTextChanged()
	}

	public void onTextChanged(CharSequence s, int start, int before,
							  int count) {
		// Handled in afterTextChanged()
	}
}
