package edu.macalester.rhubarb_crumble;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class UnitConverterActivity extends Activity implements OnItemSelectedListener, TextWatcher {

	private static final String TAG = "UnitConverterActivity";
	
	private ArrayList<Unit> units;
	private String[] unitAbbrevs;
	
	private double inputAmount;
	private boolean inputValid;
	
	private int unitInputIndex1;
	private int unitInputIndex2;
	
	private double inputRate1;
	private double inputRate2;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the view to unit_converter.xml
		setContentView(R.layout.unit_converter);
		
		// Get the selected category from the UnitCategoryChooser and get the units associated with that category
		String category = getIntent().getStringExtra("category");
		
		this.units = UnitManager.getUnits(category, this, 1);
		String[] unitNames = new String[this.units.size()];
		this.unitAbbrevs = new String[this.units.size()];
		
		int i = 0;
		for(Unit unit : this.units) {
			unitNames[i] = unit.getLocalizedName();
			this.unitAbbrevs[i] = unit.getLocalizedAbbreviation();
			i++;
		}
		
		//Initialize the unit selector spinners
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		Spinner inputSpinner = (Spinner) findViewById(R.id.unitInput1);
		Spinner outputSpinner = (Spinner) findViewById(R.id.unitInput2);
		
		inputSpinner.setAdapter(adapter);
		inputSpinner.setOnItemSelectedListener(this);
		outputSpinner.setAdapter(adapter);
		outputSpinner.setOnItemSelectedListener(this);
		
		// Initialize callback for the numeric input field
		EditText edit_text = (EditText)findViewById(R.id.unitAmount);
		edit_text.addTextChangedListener(this);
		
		// Initialize text output
		setConversionOutput("");
		this.inputValid = false;
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
			Log.d(TAG, "Selected \"to\" currency idx " + position + " (" +
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
		Log.d(TAG, "Input rate 1: " + this.inputRate1);
		Log.d(TAG, "Input rate 2: " + this.inputRate2);
		
		Double resultAmount = amount * (this.inputRate1 / this.inputRate2);
		Log.d(TAG, "Input*: " + Double.toString(this.inputRate1 / this.inputRate2));
		
		String result = Double.toString(resultAmount);
		
		setConversionOutput(result);
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
