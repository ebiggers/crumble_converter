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

import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class CurrencyConverterActivity extends Activity
			 implements OnItemSelectedListener, TextWatcher, Handler.Callback {

	private CurrencyRatesManager rates_manager;
	private int from_currency_idx;
	private int to_currency_idx;

	private ExchangeRate from_exchange_rate;
	private ExchangeRate to_exchange_rate;

	private double cur_amount;
	private boolean cur_amount_valid;

	private String[] currency_abbrevs;
	private final String TAG = "CurrencyConverterActivity";

	private TextView[] recent_currency_textviews;

	private static class RecentCurrency {
		public String abbrev;
		public int abbrev_idx;
		public ExchangeRate rate;

		public RecentCurrency() {
		}

		public RecentCurrency(String abbrev, int abbrev_idx) {
			this.abbrev       = abbrev;
			this.abbrev_idx   = abbrev_idx;
			this.rate         = null;
		}
	};

	private ArrayList<RecentCurrency> recent_currencies;

	private static final int NUM_RECENT_CURRENCIES = 3;

	private static final int NEED_RATE_FOR_CONVERSION = 0;
	private static final int NEED_RATE_FOR_LAST_CURRENCY_LIST = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the view to currency_converter.xml
		setContentView(R.layout.currency_converter);

		// Start a new thread that will be responsible for managing currency
		// exchange rates.
		Handler activity_handler = new Handler(this);
		this.rates_manager = new CurrencyRatesManager(this, activity_handler);

		// Get the list of currency names
		this.currency_abbrevs = rates_manager.getCurrencyAbbreviations();
		int num_currencies = currency_abbrevs.length;
		String[] currency_choices = load_currency_names(this.currency_abbrevs);

		// Set callbacks on the from-currency and to-currency spinners
		ArrayAdapter adapter = new ArrayAdapter(this,
												android.R.layout.simple_spinner_item,
												currency_choices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner from_spinner = (Spinner)findViewById(R.id.from_currency_spinner);
		Spinner to_spinner = (Spinner)findViewById(R.id.to_currency_spinner);

		int usd_idx = java.util.Arrays.binarySearch(currency_abbrevs, "USD");

		from_spinner.setAdapter(adapter);
		from_spinner.setOnItemSelectedListener(this);
		if (usd_idx != -1)
			from_spinner.setSelection(usd_idx);
		to_spinner.setAdapter(adapter);
		to_spinner.setOnItemSelectedListener(this);
		if (usd_idx != -1)
			to_spinner.setSelection(usd_idx);

		// Initialize callback for the numberic input field
		EditText edit_text = (EditText)findViewById(R.id.currency_converter_edit_text);
		edit_text.addTextChangedListener(this);
		this.from_currency_idx = -1;
		this.to_currency_idx = -1;
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
			editor.apply();
		}
	}

	// Load the recently converted currencies from the preferences.
	private void load_recent_currencies() {
		this.recent_currency_textviews = new TextView[NUM_RECENT_CURRENCIES];
		
		this.recent_currency_textviews[0] = (TextView)findViewById(R.id.recent_currency_1);
		this.recent_currency_textviews[1] = (TextView)findViewById(R.id.recent_currency_2);
		this.recent_currency_textviews[2] = (TextView)findViewById(R.id.recent_currency_3);

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		this.recent_currencies = new ArrayList<RecentCurrency>();
		
		for (int i = 0; i < NUM_RECENT_CURRENCIES; i++) {
			String abbrev = prefs.getString("recent_currency_" + i, null);
			if (abbrev != null) {
				int abbrev_idx = -1;
				for (int j = 0; j < currency_abbrevs.length; j++) {
					if (currency_abbrevs[j].equals(abbrev)) {
						Log.d(TAG, "Loaded recent currency " + abbrev);
						abbrev_idx = j;
						break;
					}
				}
				if (abbrev_idx != -1) {
					this.recent_currencies.add(new RecentCurrency(abbrev, abbrev_idx));
				}
			}
		}

		for (RecentCurrency cur : recent_currencies) {
			Message msg = Message.obtain();
			msg.obj = cur.abbrev;
			msg.what = CurrencyRatesManager.MSG_NEED_CURRENCY_RATE;
			msg.arg1 = cur.abbrev_idx;
			msg.arg2 = NEED_RATE_FOR_LAST_CURRENCY_LIST;
			rates_manager.getHandler().sendMessage(msg);
		}

		update_recent_rates();
	}

	// Update the TextViews that show the rates of the recently converted
	// currencies.
	private void update_recent_rates() {
		for (int i = 0; i < NUM_RECENT_CURRENCIES; i++) {
			String txt;
			if (i < recent_currencies.size()) {
				RecentCurrency cur = recent_currencies.get(i);
				txt = "USD per " + cur.abbrev + ": ";

				if (cur.rate == null)
					txt += "waiting...";
				else
					txt += cur.rate.usd_equivalent;
			} else {
				txt = "";
			}
			recent_currency_textviews[i].setText(txt);
		}
	}


	private void push_recent_rate(String abbrev, int abbrev_idx) {

		// If the currency is already in the recent currencies list, move it to
		// the front of the list.
		for (int i = 0; i < recent_currencies.size(); i++) {
			RecentCurrency cur = recent_currencies.get(i);
			if (cur.abbrev.equals(abbrev)) {
				Log.d(TAG, abbrev + " is already in the recent currencies list");
				if (i != 0) {
					Log.d(TAG, "Moving currency " + abbrev + " from slot " + i + " to "
							   + "slot 0 in the rucent currencies list");
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

		Message msg = Message.obtain();
		msg.obj = abbrev;
		Log.d(TAG, "Sending message MSG_NEED_CURRENCY_RATE (" + abbrev + ")");
		msg.what = CurrencyRatesManager.MSG_NEED_CURRENCY_RATE;
		msg.arg1 = abbrev_idx;
		msg.arg2 = NEED_RATE_FOR_LAST_CURRENCY_LIST;
		rates_manager.getHandler().sendMessage(msg);
		update_recent_rates();
	}

	// Given a list of currency abbreviations, load their full localized names.
	private String[] load_currency_names(String[] currency_abbrevs) {
		int num_currencies = currency_abbrevs.length;
		String[] currency_choices = new String[num_currencies];
		Resources resources = getResources();
		for (int i = 0; i < num_currencies; i++) {
			String abbrev = currency_abbrevs[i];
			String name;

			int id = resources.getIdentifier(currency_abbrevs[i], "string",
											 this.getPackageName());
			if (id == 0) {
				Log.w(TAG, "Cannot find full name for currency " + abbrev);
				name = abbrev;
			} else {
				try {
					name = abbrev + " (" + resources.getString(id) + ")";
				} catch (Resources.NotFoundException e) {
					Log.w(TAG, "Cannot find full name for currency " + abbrev, e);
					name = abbrev;
				}
			}
			currency_choices[i] = name;
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
			double to_from_ratio = to_exchange_rate.usd_equivalent /
								   from_exchange_rate.usd_equivalent;
			double new_amount = amount * to_from_ratio;
			Log.d(TAG, "Converted " + amount + " " + from_exchange_rate.abbrev +
				  " to " + new_amount + " " + to_exchange_rate.abbrev + " at " +
				  to_from_ratio + " " + to_exchange_rate.abbrev + " per " +
				  from_exchange_rate.abbrev);
			result = Double.toString(new_amount);
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
			Log.d(TAG, "Sending message MSG_NEED_CURRENCY_RATE (" +
						currency_abbrevs[from_currency_idx] + ")");
			msg = Message.obtain();
			msg.what = CurrencyRatesManager.MSG_NEED_CURRENCY_RATE;
			msg.arg1 = from_currency_idx;
			msg.arg2 = NEED_RATE_FOR_CONVERSION;
			msg.obj = currency_abbrevs[from_currency_idx];
			rates_manager.getHandler().sendMessage(msg);
			conversion_possible_now = false;
		}
		if (to_exchange_rate == null || to_exchange_rate.is_out_of_date()) {
			Log.d(TAG, "Sending message MSG_NEED_CURRENCY_RATE (" +
						currency_abbrevs[to_currency_idx] + ")");
			msg = Message.obtain();
			msg.what = CurrencyRatesManager.MSG_NEED_CURRENCY_RATE;
			msg.arg1 = to_currency_idx;
			msg.arg2 = NEED_RATE_FOR_CONVERSION;
			msg.obj = currency_abbrevs[to_currency_idx];
			rates_manager.getHandler().sendMessage(msg);
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
