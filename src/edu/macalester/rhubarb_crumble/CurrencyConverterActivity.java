package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
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
									   implements OnItemSelectedListener,
												  TextWatcher {

	private CurrencyRatesManager rates_manager;
	private int from_currency_idx = -1;
	private int to_currency_idx = -1;
	private double cur_amount;
	private boolean cur_amount_valid;

	private String[] currency_abbrevs;
	private final String TAG = "CurrencyConverterActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currency_converter);

		this.rates_manager = new CurrencyRatesManager(this);
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
					name = abbrev + "(" + resources.getString(id) + ")";
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

	private void maybe_do_conversion() {
		String result;

		if (from_currency_idx != -1 && to_currency_idx != -1) {
			double amount;
			if (cur_amount_valid)
				amount = cur_amount;
			else
				amount = 1.0;
			String from_currency_abbrev = currency_abbrevs[from_currency_idx];
			String to_currency_abbrev = currency_abbrevs[to_currency_idx];
			ExchangeRate from_rate = rates_manager.getExchangeRate(from_currency_abbrev);
			ExchangeRate to_rate = rates_manager.getExchangeRate(to_currency_abbrev);
			if (from_rate.is_outdated || to_rate.is_outdated) {
				Log.w(TAG, "Unable to perform currency conversion " + from_currency_abbrev +
					  " => " + to_currency_abbrev);
				result = "OUTDATED"; // XXX
			} else {
				if (to_rate.usd_equivalent <= 0.0) {
					Log.e(TAG, "Error: Currency " + to_currency_abbrev + " is worth 0 or less!");
					//
					result = "DIVIDE BY ZERO";
				} else if (from_rate.usd_equivalent <= 0.0) {
					Log.e(TAG, "Error: Currency " + from_currency_abbrev + " is worth 0 or less!");
					//
					result = "DIVIDE BY ZERO";
				}
				double to_from_ratio = to_rate.usd_equivalent / from_rate.usd_equivalent;
				double new_amount = amount * to_from_ratio;
				Log.d(TAG, "Converted " + amount + " " + from_currency_abbrev +
					  " to " + new_amount + " " + to_currency_abbrev + " at " +
					  to_from_ratio + " " + to_currency_abbrev + " per " +
					  from_currency_abbrev);
				result = Double.toString(new_amount);
			}
		} else {
			result = "";
		}
		TextView v = (TextView)findViewById(R.id.currency_conversion_output);
		v.setText(result);
	}

	public void onItemSelected(AdapterView<?> parent, View view,
							   int position, long id) {
		if (parent.getId() == R.id.from_currency_spinner) {
			Log.d(TAG, "Selected \"from\" currency idx " + position + " (" +
						currency_abbrevs[position] + ")");
			from_currency_idx = position;
		} else {
			Log.d(TAG, "Selected \"to\" currency idx " + position + " (" +
						currency_abbrevs[position] + ")");
			to_currency_idx = position;
		}
		maybe_do_conversion();
	}

	public void onNothingSelected(AdapterView<?> parent) {
		if (parent.getId() == R.id.from_currency_spinner) {
			Log.d(TAG, "Deselected \"from\" currency");
			from_currency_idx = -1;
		} else {
			Log.d(TAG, "Deselected \"to\" currency");
			to_currency_idx = -1;
		}
		maybe_do_conversion();
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
