package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class UnitCategoryChooserActivity extends Activity implements OnClickListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unit_category_chooser);
		
		View additionalUnitsCheckBox = findViewById(R.id.additional_units_chekbox);
		additionalUnitsCheckBox.setOnClickListener(this);
		
		View distanceButton = findViewById(R.id.distance_button);
		distanceButton.setOnClickListener(this);
		
		View temperatureButton = findViewById(R.id.temperature_button);
		temperatureButton.setOnClickListener(this);
		
		View timeButton = findViewById(R.id.time_button);
		timeButton.setOnClickListener(this);
		
		View volumeButton = findViewById(R.id.volume_button);
		volumeButton.setOnClickListener(this);
		
		View weightButton = findViewById(R.id.weight_button);
		weightButton.setOnClickListener(this);
		
		View metricPrefixesButton = findViewById(R.id.prefixes_button);
		metricPrefixesButton.setOnClickListener(this);
		
		View angleButton = findViewById(R.id.angle_button);
		angleButton.setOnClickListener(this);
		
		View areaButton = findViewById(R.id.area_button);
		areaButton.setOnClickListener(this);
		
		View energyButton = findViewById(R.id.energy_button);
		energyButton.setOnClickListener(this);
		
		View forceButton = findViewById(R.id.force_button);
		forceButton.setOnClickListener(this);
		
		View pressureButton = findViewById(R.id.pressure_button);
		pressureButton.setOnClickListener(this);
		
		View speedButton = findViewById(R.id.speed_button);
		speedButton.setOnClickListener(this);
	}
	
	public void onClick(View v)
	{
		switch(v.getId()) {
		case R.id.additional_units_chekbox:
			View additionalUnitsField = findViewById(R.id.additional_units_layout);
			View additionalUnitsLabel = findViewById(R.id.additional_units_label);
			if(((CheckBox) v).isChecked())
			{
				additionalUnitsField.setVisibility(View.VISIBLE);
				additionalUnitsLabel.setVisibility(View.VISIBLE);
				Log.d("Category", "Checkbox Checked.");
			}
			else
			{
				additionalUnitsField.setVisibility(View.GONE);
				additionalUnitsLabel.setVisibility(View.GONE);
				Log.d("Category", "Checkbox not Checked");
			}
			additionalUnitsField.invalidate();
			additionalUnitsLabel.invalidate();
			break;
		case R.id.distance_button:
			Intent i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.distance);
			startActivity(i);
			break;
		case R.id.temperature_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.temperature);
			startActivity(i);
			break;
		case R.id.time_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.time);
			startActivity(i);
			break;
		case R.id.volume_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.volume);
			startActivity(i);
			break;
		case R.id.weight_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.weight);
			startActivity(i);
			break;
		case R.id.angle_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.angle);
			startActivity(i);
			break;
		case R.id.area_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.area);
			startActivity(i);
			break;
		case R.id.energy_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.energy);
			startActivity(i);
			break;
		case R.id.force_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.force);
			startActivity(i);
			break;
		case R.id.pressure_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.pressure);
			startActivity(i);
			break;
		case R.id.speed_button:
			i = new Intent(this, UnitConverterActivity.class);
			i.putExtra("category", R.string.speed);
			startActivity(i);
			break;
		}
	}
}
