package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class UnitCategoryChooserActivity extends Activity
                                         implements OnClickListener
{

	private static final String TAG = "UnitCategoryChooserActivity";

	private void listenForClickOn(int resource_id) {
		View view = findViewById(resource_id);
		view.setOnClickListener(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unit_category_chooser);
		Log.d(TAG, "Starting UnitCategoryChooserActivity");

		listenForClickOn(R.id.additional_units_chekbox);

		listenForClickOn(R.id.distance_button);
		listenForClickOn(R.id.temperature_button);
		listenForClickOn(R.id.time_button);
		listenForClickOn(R.id.volume_button);
		listenForClickOn(R.id.weight_button);
		listenForClickOn(R.id.prefixes_button);
		listenForClickOn(R.id.angle_button);
		listenForClickOn(R.id.area_button);
		listenForClickOn(R.id.energy_button);
		listenForClickOn(R.id.force_button);
		listenForClickOn(R.id.pressure_button);
		listenForClickOn(R.id.speed_button);
	}

	public void onClick(View v)
	{
		String category;
		switch(v.getId()) {
		case R.id.additional_units_chekbox:
			View additionalUnitsField = findViewById(R.id.additional_units_layout);
			View additionalUnitsLabel = findViewById(R.id.additional_units_label);
			if(((CheckBox) v).isChecked()) {
				additionalUnitsField.setVisibility(View.VISIBLE);
				additionalUnitsLabel.setVisibility(View.VISIBLE);
				Log.d("Category", "Checkbox Checked.");
			} else {
				additionalUnitsField.setVisibility(View.GONE);
				additionalUnitsLabel.setVisibility(View.GONE);
				Log.d("Category", "Checkbox not Checked");
			}
			additionalUnitsField.invalidate();
			additionalUnitsLabel.invalidate();
			return;
		case R.id.angle_button:
			category = "angle";
			break;
		case R.id.area_button:
			category = "area";
			break;
		case R.id.energy_button:
			category = "energy";
			break;
		case R.id.force_button:
			category = "force";
			break;
		case R.id.distance_button:
			category = "length";
			break;
		case R.id.weight_button:
			category = "mass";
			break;
		case R.id.pressure_button:
			category = "pressure";
			break;
		case R.id.speed_button:
			category = "speed";
			break;
		case R.id.temperature_button:
			category = "temperature";
			break;
		case R.id.time_button:
			category = "time";
			break;
		case R.id.volume_button:
			category = "volume";
			break;
		default:
			Log.e(TAG, "Unimplemented button!");
			category = "";
			break;
		}
		Intent i = new Intent(this, UnitConverterActivity.class);
		i.putExtra("category", category);
		Log.d(TAG, "Starting UnitConverterActivity with category=\"" + category + "\"");
		startActivity(i);
	}
}
