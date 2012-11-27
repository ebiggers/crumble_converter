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

public class UnitConverterActivity extends Activity implements OnItemSelectedListener, TextWatcher {

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
		
		//Initialize conversion rates
		this.unitInputIndex1 = inputSpinner.getSelectedItemPosition();
		this.unitInputIndex2 = outputSpinner.getSelectedItemPosition();
		this.inputRate1 = this.units.get(this.unitInputIndex1).getNormalizedValue();
		this.inputRate2 = this.units.get(this.unitInputIndex2).getNormalizedValue();
		
		//Setup checkbox listener
		CheckBox addUnits = (CheckBox) findViewById(R.id.addUnitsCheckBox);
		addUnits.setChecked(false);
		addUnits.setOnClickListener(checkBoxListener);
	}
	
	View.OnClickListener checkBoxListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch(v.getId()) {
			case R.id.addUnitsCheckBox:
				if (((CheckBox)v).isChecked()) {
					inputSpinner.setAdapter(unitAdapter);
					outputSpinner.setAdapter(unitAdapter);
				} else {
					inputSpinner.setAdapter(unitSubsetAdapter);
					outputSpinner.setAdapter(unitSubsetAdapter);
				}
			}		
		}
	};
	
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

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {		
		if (parent.getId() == R.id.unitInput1) {
			Log.d(TAG, "Selected \"from\" unit index " + position + " (" +
						unitAbbrevs[position] + ")");
			if (unitInputIndex1 != position) {
				unitInputIndex1 = position;
				this.inputRate1 = this.units.get(unitInputIndex1).getNormalizedValue();
			} else {
				Log.d(TAG, unitAbbrevs[position] + " already selected; no conversion done.");
				return;
			}
		} else {
			Log.d(TAG, "Selected \"to\" unit idx " + position + " (" +
						unitAbbrevs[position] + ")");
			if (unitInputIndex2 != position) {
				unitInputIndex2 = position;
				this.inputRate2 = this.units.get(unitInputIndex2).getNormalizedValue();
			} else {
				Log.d(TAG, unitAbbrevs[position] + " already selected; no conversion done.");
				return;
			}
		}
		doConversion();
	}
	
	public void doConversion() {
		Double amount;
		if (this.inputValid)
			amount = this.inputAmount;
		else
			amount = 1.0;
		
		String unit1 = this.units.get(unitInputIndex1).getLocalizedName();
		String unit2 = this.units.get(unitInputIndex2).getLocalizedName();
		
		Log.d(TAG, "Converting " + amount + " from " + unit1 + " to " + unit2);
		
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

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
