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

	private ArrayList<Unit> units;
	private static final String TAG = "UnitConverterActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the view to unit_converter.xml
		setContentView(R.layout.unit_converter);
		
		// Get the selected category from the UnitCategoryChooser and get the units associated with that category
		String category = getIntent().getStringExtra("category");
		
		this.units = UnitManager.getUnits(category, this, 1);
		String[] unitNames = this.units.toString().split(",");
		
		Log.d(TAG, "Unit names: ");
		for(String unitName : unitNames) {
			Log.d(TAG, unitName);
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
	}
	
	// Set the text in the converter output field.
	private void setConversionOutput(String s) {
		TextView v = (TextView)findViewById(R.id.unit_conversion_output);
		v.setText(s);
	}

	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
