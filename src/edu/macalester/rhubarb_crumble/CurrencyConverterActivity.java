package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
	private int from_currency_idx = -1;
	private int to_currency_idx = -1;

	private ExchangeRate from_exchange_rate;
	private ExchangeRate to_exchange_rate;

	private double cur_amount;
	private boolean cur_amount_valid;

	private String[] currency_abbrevs;
	private final String TAG = "CurrencyConverterActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currency_converter);

		Handler activity_handler = new Handler(this);
		this.rates_manager = new CurrencyRatesManager(this, activity_handler);
		cur_amount_valid = false;

		EditText edit_text = (EditText)findViewById(R.id.currency_converter_edit_text);
		edit_text.addTextChangedListener(this);

		currency_abbrevs = rates_manager.getCurrencyAbbreviations();
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
		ArrayAdapter adapter = new ArrayAdapter(this,
												android.R.layout.simple_spinner_item,
												currency_choices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner from_spinner = (Spinner)findViewById(R.id.from_currency_spinner);
		Spinner to_spinner = (Spinner)findViewById(R.id.to_currency_spinner);

		from_spinner.setAdapter(adapter);
		from_spinner.setOnItemSelectedListener(this);
		to_spinner.setAdapter(adapter);
		to_spinner.setOnItemSelectedListener(this);
	}

	private void setConversionOutput(String s) {
		TextView v = (TextView)findViewById(R.id.currency_conversion_output);
		v.setText(s);
	}

	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case CurrencyRatesManager.MSG_HAVE_CURRENCY_RATE:
			ExchangeRate r = (ExchangeRate)msg.obj;
			Log.d(TAG, "Received message MSG_HAVE_CURRENCY_RATE (" + r.abbrev + ")");
			int currency_idx = msg.arg1;
			if (currency_idx == from_currency_idx) {
				from_exchange_rate = r;
			}
			if (currency_idx == to_currency_idx) {
				to_exchange_rate = r;
			}
			maybe_do_outdated_conversion();
			break;
		}
		return true;
	}

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
			// XXX
			result = "DIVIDE BY ZERO";
		} else if (from_exchange_rate.usd_equivalent <= 0.0) {
			Log.e(TAG, "Error: Currency " + to_exchange_rate.abbrev + " is worth 0 or less!");
			// XXX
			result = "DIVIDE BY ZERO";
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

	private void maybe_do_outdated_conversion() {
		if (from_exchange_rate != null && to_exchange_rate != null) {
			do_conversion();
		}
	}

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

	public void onItemSelected(AdapterView<?> parent, View view,
							   int position, long id) {
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
	}

	public void onTextChanged(CharSequence s, int start, int before,
							  int count) {
	}
}
