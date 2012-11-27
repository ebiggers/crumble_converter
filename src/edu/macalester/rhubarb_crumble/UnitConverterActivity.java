package edu.macalester.rhubarb_crumble;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class UnitConverterActivity extends Activity implements
			OnItemSelectedListener, TextWatcher, OnClickListener
{
	private static final String TAG = "UnitConverterActivity";

	private ArrayList<Unit> units;
	private ArrayList<Unit> unitSubset;
	private String[] unitAbbrevs;

	private ArrayAdapter unitAdapter;
	private ArrayAdapter unitSubsetAdapter;
	private Spinner inputSpinner;
	private Spinner outputSpinner;

	private double inputAmount;
	private boolean inputValid;

	private int unitInputIndex1;
	private int unitInputIndex2;

	private double inputRate1;
	private double inputRate2;

	private boolean additional_units_shown;

	String category;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the view to unit_converter.xml
		setContentView(R.layout.unit_converter);

		// Get the selected category from the UnitCategoryChooser and get the units associated with that category
		this.category = getIntent().getStringExtra("category");

		this.units = UnitManager.getUnits(this.category, this, 1);
		String[] unitNames = new String[this.units.size()];
		for (int i = 0; i < this.units.size(); i++)
			unitNames[i] = units.get(i).getLocalizedName();

		this.unitAbbrevs = new String[this.units.size()];

		this.unitSubset = UnitManager.getUnits(this.category, this, 0);
		String[] unitSubsetNames = new String[this.unitSubset.size()];
		for (int i = 0; i < this.unitSubset.size(); i++)
			unitSubsetNames[i] = unitSubset.get(i).getLocalizedName();

		//Initialize the unit selector spinners
		unitAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames);
		unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		unitSubsetAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, unitSubsetNames);
		unitSubsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		inputSpinner = (Spinner) findViewById(R.id.unitInput1);
		outputSpinner = (Spinner) findViewById(R.id.unitInput2);

		inputSpinner.setAdapter(unitSubsetAdapter);
		outputSpinner.setAdapter(unitSubsetAdapter);

		inputSpinner.setOnItemSelectedListener(this);
		outputSpinner.setOnItemSelectedListener(this);

		// Initialize callback for the numeric input field
		EditText edit_text = (EditText)findViewById(R.id.unitAmount);
		edit_text.addTextChangedListener(this);

		// Initialize text output
		setConversionOutput("");
		this.inputValid = false;


		//Setup checkbox listener
		CheckBox addUnits = (CheckBox) findViewById(R.id.addUnitsCheckBox);
		addUnits.setChecked(false);
		additional_units_shown = false;
		addUnits.setOnClickListener(this);

		//Initialize conversion rates
		this.unitInputIndex1 = inputSpinner.getSelectedItemPosition();
		this.unitInputIndex2 = outputSpinner.getSelectedItemPosition();
		this.inputRate1 = getUnit(this.unitInputIndex1).getNormalizedValue();
		this.inputRate2 = getUnit(this.unitInputIndex2).getNormalizedValue();
	}

	// Set the text in the converter output field.
	private void setConversionOutput(String s) {
		TextView v = (TextView)findViewById(R.id.unit_conversion_output);
		v.setText(s);
	}

	public void afterTextChanged(Editable amount) {
		if (amount.length() == 0) {
			this.inputValid = false;
		} else {
			String text = amount.toString();
			try {
				this.inputAmount = Double.parseDouble(text);
				this.inputValid = true;
				Log.d(TAG, "Current amount updated to: " + this.inputAmount);
				doConversion();
			} catch (NumberFormatException e) {
				this.inputValid = false;
				Log.d(TAG, text + " could not be converted to a double");
			}
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

	private Unit getUnit(int index)
	{
		if (additional_units_shown)
			return units.get(index);
		else
			return unitSubset.get(index);
	}

	private int subsetToAdditionalIndex(int idx) {
		if (idx == -1)
			return -1;
		Unit u = this.unitSubset.get(idx);
		for (int i = 0; i < this.units.size(); i++)
			if (u.equals(this.units.get(i)))
				return i;
		return -1;
	}

	private int additionalToSubsetIndex(int idx) {
		if (idx == -1)
			return -1;
		Unit u = this.units.get(idx);
		for (int i = 0; i < this.unitSubset.size(); i++)
			if (u.equals(this.unitSubset.get(i)))
					return i;
		return -1;
	}

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.addUnitsCheckBox:
			if (((CheckBox)v).isChecked()) {
				if (!additional_units_shown) {
					additional_units_shown = true;
					this.unitInputIndex1 = subsetToAdditionalIndex(this.unitInputIndex1);
					this.unitInputIndex2 = subsetToAdditionalIndex(this.unitInputIndex2);
					inputSpinner.setAdapter(unitAdapter);
					outputSpinner.setAdapter(unitAdapter);
					inputSpinner.setSelection(unitInputIndex1);
					outputSpinner.setSelection(unitInputIndex2);
				}
			} else {
				if (additional_units_shown) {
					additional_units_shown = false;
					this.unitInputIndex1 = additionalToSubsetIndex(this.unitInputIndex1);
					this.unitInputIndex2 = additionalToSubsetIndex(this.unitInputIndex2);
					inputSpinner.setAdapter(unitSubsetAdapter);
					outputSpinner.setAdapter(unitSubsetAdapter);
					if (this.unitInputIndex1 == -1)
						inputSpinner.setSelection(0);
					else
						inputSpinner.setSelection(unitInputIndex1);

					if (this.unitInputIndex2 == -1)
						outputSpinner.setSelection(0);
					else
						outputSpinner.setSelection(unitInputIndex2);
				}
			}
		}
	}


	public void onItemSelected(AdapterView<?> parent, View view,
							   int position, long id)
	{
		switch (parent.getId()) {
		case R.id.unitInput1:
			Log.d(TAG, "Selected \"from\" unit index " + position + " (" +
						unitAbbrevs[position] + ")");
			unitInputIndex1 = position;
			inputRate1 = getUnit(position).getNormalizedValue();
			break;
		case R.id.unitInput2:
			Log.d(TAG, "Selected \"to\" unit idx " + position + " (" +
						unitAbbrevs[position] + ")");
			unitInputIndex2 = position;
			inputRate2 = getUnit(unitInputIndex2).getNormalizedValue();
			break;
		}
		if (this.unitInputIndex1 != -1 && this.unitInputIndex2 != -1)
			doConversion();
	}

	public void doConversion() {
		Double amount;
		if (this.inputValid)
			amount = this.inputAmount;
		else
			amount = 1.0;

		String unit1 = getUnit(unitInputIndex1).getLocalizedName();
		String unit2 = getUnit(unitInputIndex2).getLocalizedName();

		Log.d(TAG, "Converting " + amount + " " + unit1 + " to " + unit2);

		Double resultAmount = 0.0;

		if (this.category.equalsIgnoreCase("temperature")) {
			if (unit1.equalsIgnoreCase(unit2)) {
				resultAmount = amount;
			} else if (unit1.equalsIgnoreCase("fahrenheit")) {
				if (unit2.equalsIgnoreCase("celsius")) {
					resultAmount = (amount - 32) * (5/9.0);
				} else if (unit2.equalsIgnoreCase("kelvin")) {
					resultAmount = (amount - 32) * (5/9.0) + 273.15;
				}
			} else if (unit1.equalsIgnoreCase("celsius")) {
				if (unit2.equalsIgnoreCase("fahrenheit")) {
					resultAmount = (amount * (9/5.0)) + 32;
				} else if (unit2.equalsIgnoreCase("kelvin")) {
					resultAmount = amount + 273.15;
				}
			} else if (unit1.equalsIgnoreCase("kelvin")) {
				if (unit2.equalsIgnoreCase("fahrenheit")) {
					resultAmount = ((amount - 273.15) * 1.8) + 32;
				} else if (unit2.equalsIgnoreCase("celsius")) {
					resultAmount = amount - 273.15;
				}
			}
		} else {
			resultAmount = amount * (this.inputRate1 / this.inputRate2);
		}

		String result = Double.toString(resultAmount);
		setConversionOutput(result);
	}

	public void onNothingSelected(AdapterView<?> parent) {
		switch (parent.getId()) {
		case R.id.unitInput1:
			this.unitInputIndex1 = -1;
			this.inputRate1 = -1.0;
			break;
		case R.id.unitInput2:
			this.unitInputIndex2 = -1;
			this.inputRate2 = -1.0;
			break;
		}
	}
}
