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

// The activity that runs when the user is converting units within a specific
// unit category.
public class UnitConverterActivity extends Activity implements
			OnItemSelectedListener, TextWatcher, OnClickListener
{
	private static final String TAG = "UnitConverterActivity";

	// The full list of units, including those that are shown when the "Show
	// additional units" box is checked.
	private ArrayList<Unit> units;

	// The list of units that are shown when the "Show additional units" box is
	// not checked.
	private ArrayList<Unit> unitSubset;

	// Adapter to provide a Spinner with the full list of units.
	private ArrayAdapter unitAdapter;

	// Adapter to provide a Spinner with only the units in @unitSubset.
	private ArrayAdapter unitSubsetAdapter;

	// Spinner for the input unit.
	private Spinner inputSpinner;

	// Spinner for the output unit.
	private Spinner outputSpinner;

	// The current amount entered in the unit amount input field, and whether it
	// is currently valid or not.
	private double inputAmount;
	private boolean inputValid;

	// true when the "Show Additional Units" box is checked.
	private boolean additional_units_shown;

	// The index of the "from" unit that is currently selected, within @units
	// (when additional_units_shown) or within @unitSubset (when
	// !additional_units_shown).  -1 if no unit currently selected.
	private int unitInputIndex1;

	// The index of the "to" unit that is currently selected, within @units
	// (when additional_units_shown) or within @unitSubset (when
	// !additional_units_shown).  -1 if no unit currently selected.
	private int unitInputIndex2;

	// The normalized value (relative to some canonical unit) of the currently
	// selected "from" unit, or -1.0 if no "from" unit is currently selected.
	private double inputRate1;

	// The normalized value (relative to some canonical unit) of the currently
	// selected "to" unit, or -1.0 if no "to" unit is currently selected.
	private double inputRate2;

	// The category of units being converted (such as "length" or "temperature")
	String category;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the view to unit_converter.xml
		setContentView(R.layout.unit_converter);

		// Get the selected category from the UnitCategoryChooser and get the
		// units associated with that category
		this.category = getIntent().getStringExtra("category");

		// Set up the full list of units and their names, and an adapter for
		// showing them
		this.units = UnitManager.getUnits(this.category, this, 1);
		String[] unitNames = new String[this.units.size()];
		for (int i = 0; i < this.units.size(); i++)
			unitNames[i] = units.get(i).getLocalizedName();
		unitAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames);
		unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Set up the subset of more common units and their namnes, and an
		// adapter for showing them
		this.unitSubset = UnitManager.getUnits(this.category, this, 0);
		String[] unitSubsetNames = new String[this.unitSubset.size()];
		for (int i = 0; i < this.unitSubset.size(); i++)
			unitSubsetNames[i] = unitSubset.get(i).getLocalizedName();
		unitSubsetAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, unitSubsetNames);
		unitSubsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Initialize the "from" and "to" unit selector spinners.  They
		// initially show only the subset of more common units.
		inputSpinner = (Spinner) findViewById(R.id.unitInput1);
		outputSpinner = (Spinner) findViewById(R.id.unitInput2);
		inputSpinner.setAdapter(unitSubsetAdapter);
		outputSpinner.setAdapter(unitSubsetAdapter);
		inputSpinner.setOnItemSelectedListener(this);
		outputSpinner.setOnItemSelectedListener(this);

		// Initialize the callback for when the numeric input field is edited.
		EditText edit_text = (EditText)findViewById(R.id.unitAmount);
		edit_text.addTextChangedListener(this);
		this.inputValid = false;

		// Initially, no output is shown.
		setConversionOutput("");

		// Initially, additional units are not shown; set up the checkbox
		// listener so that the user can change this.
		CheckBox addUnits = (CheckBox) findViewById(R.id.addUnitsCheckBox);
		addUnits.setChecked(false);
		additional_units_shown = false;
		addUnits.setOnClickListener(this);

		// Initially, no units are selected (actually, the spinner will default
		// to the first selection, but this will be handled in the
		// onItemSelected() callback.)
		this.unitInputIndex1 = -1;
		this.unitInputIndex2 = -1;
		this.inputRate1 = -1.0;
		this.inputRate2 = -1.0;

		// Hide the additional units checkbox if no additional units are available
		if (this.units.size() == this.unitSubset.size()) {
			addUnits.setVisibility(View.GONE);
		}
	}

	// Save the activity state
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("additional_units_shown", additional_units_shown);
		savedInstanceState.putInt("unitInputIndex1", unitInputIndex1);
		savedInstanceState.putInt("unitInputIndex2", unitInputIndex2);
		savedInstanceState.putDouble("inputAmount", inputAmount);
		savedInstanceState.putBoolean("inputValid", inputValid);
		savedInstanceState.putDouble("inputRate1", inputRate1);
		savedInstanceState.putDouble("inputRate2", inputRate2);
	}

	// Restore the activity state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		unitInputIndex1 = savedInstanceState.getInt("unitInputIndex1");
		unitInputIndex2 = savedInstanceState.getInt("unitInputIndex2");
		inputAmount = savedInstanceState.getDouble("inputAmount");
		inputValid = savedInstanceState.getBoolean("inputValid");
		inputRate1 = savedInstanceState.getDouble("inputRate1");
		inputRate2 = savedInstanceState.getDouble("inputRate2");
		if (savedInstanceState.getBoolean("additional_units_shown") &&
			!additional_units_shown)
		{
			changeUnitsShown(false);
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	// Set the text in the converter output field.
	private void setConversionOutput(String s) {
		TextView v = (TextView)findViewById(R.id.unit_conversion_output);
		v.setText(s);
	}

	// Called after the text in the "from" unit amount field is edited.
	public void afterTextChanged(Editable amount) {
		if (amount.length() == 0) {
			// If no text is entered, the input is invalid.
			this.inputValid = false;
		} else {
			// If text is entered, it is valid only if it can be interpreted as
			// a double.
			String text = amount.toString();
			try {
				this.inputAmount = Double.parseDouble(text);
				this.inputValid = true;
				Log.d(TAG, "Current amount updated to: " + this.inputAmount);
				maybeDoConversion();
			} catch (NumberFormatException e) {
				this.inputValid = false;
				Log.d(TAG, text + " could not be converted to a double");
			}
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	// Return the unit at index @index, taking into account whether additional
	// units are currently shown or not.
	private Unit getUnit(int index)
	{
		if (additional_units_shown)
			return units.get(index);
		else
			return unitSubset.get(index);
	}

	// Translates an index into the unit subset into an index into the list of
	// full units.
	private int subsetToAdditionalIndex(int idx) {
		if (idx == -1)
			return -1;
		Unit u = this.unitSubset.get(idx);
		for (int i = 0; i < this.units.size(); i++)
			if (u.equals(this.units.get(i)))
				return i;
		return -1;
	}

	// Translates an index into the list of full units into an index into the
	// list of units in the unit subset.  Return -1 if the unit is not available
	// in the subset.
	private int additionalToSubsetIndex(int idx) {
		if (idx == -1)
			return -1;
		Unit u = this.units.get(idx);
		for (int i = 0; i < this.unitSubset.size(); i++)
			if (u.equals(this.unitSubset.get(i)))
					return i;
		return -1;
	}

	private void changeUnitsShown(boolean reindex) {
		if (additional_units_shown) {
			// Hide the additional units.  Keep each spinner on the same unit if
			// the selected unit is still available in the unit subset;
			// otherwise, set it to be on the first unit in the list.
			if (reindex) {
				this.unitInputIndex1 = additionalToSubsetIndex(this.unitInputIndex1);
				this.unitInputIndex2 = additionalToSubsetIndex(this.unitInputIndex2);
			}
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
			additional_units_shown = false;
		} else {
			// Show the additional units.  Keep the spinners on the same units,
			// even though their indices may have changed.
			if (reindex) {
				this.unitInputIndex1 = subsetToAdditionalIndex(this.unitInputIndex1);
				this.unitInputIndex2 = subsetToAdditionalIndex(this.unitInputIndex2);
			}
			inputSpinner.setAdapter(unitAdapter);
			outputSpinner.setAdapter(unitAdapter);
			inputSpinner.setSelection(unitInputIndex1);
			outputSpinner.setSelection(unitInputIndex2);
			additional_units_shown = true;
		}
	}

	// Called when the user checks or unchecks the "Show additional units"
	// checkbox.
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.addUnitsCheckBox:
			if (((CheckBox)v).isChecked()) {
				if (!additional_units_shown) {
					changeUnitsShown(true);
				}
			} else {
				if (additional_units_shown) {
					changeUnitsShown(true);
				}
			}
		}
	}


	// Called when a different unit has been selected in the "from" or "to" unit
	// spinners.
	public void onItemSelected(AdapterView<?> parent, View view,
							   int position, long id)
	{
		switch (parent.getId()) {
		case R.id.unitInput1:
			// The "from" unit has been changed.
			unitInputIndex1 = position;
			inputRate1 = getUnit(position).getNormalizedValue();
			break;
		case R.id.unitInput2:
			// The "to" unit has been changed.
			unitInputIndex2 = position;
			inputRate2 = getUnit(unitInputIndex2).getNormalizedValue();
			break;
		}

		// If something is selected in both spinners, make a unit conversion.
		maybeDoConversion();
	}

	// Make a conversion if possible
	public void maybeDoConversion() {

		if (this.unitInputIndex1 == -1 || this.unitInputIndex2 == -1)
			return;

		// If the amount entered in the numeric input field is invalid, default
		// to 1.
		double amount;
		if (this.inputValid)
			amount = this.inputAmount;
		else
			amount = 1.0;

		String unit1 = getUnit(unitInputIndex1).getLocalizedName();
		String unit2 = getUnit(unitInputIndex2).getLocalizedName();

		Log.d(TAG, "Converting " + amount + " " + unit1 + " to " + unit2);

		double resultAmount = 0.0;

		if (this.category.equalsIgnoreCase("temperature")) {
			// Handle temperature as a special case, because the different
			// temperature scales are offset from each other and this is not yet
			// represented in the unit category configuration files.
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
			// Actually make a conversion!
			resultAmount = amount * (this.inputRate1 / this.inputRate2);
		}

		// Set the result of the conversion.
		String result = Double.toString(resultAmount);
		setConversionOutput(result);
	}

	// Called when nothing is currently selected in either the "from" or "to"
	// unit spinners.
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
