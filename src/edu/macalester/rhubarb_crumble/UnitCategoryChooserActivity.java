package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

// The Activity that runs when the user is choosing a category of units to
// convert.
public class UnitCategoryChooserActivity extends Activity
										 implements OnClickListener
{

	private static final String TAG = "UnitCategoryChooserActivity";
	private boolean additional_categories_shown;

	private void listenForClickOn(int resource_id) {
		View view = findViewById(resource_id);
		view.setOnClickListener(this);
	}

	// Called when the activity is created.  Sets the appropriate view and sets
	// up onClick handlers for the buttons and the checkbox.
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
		listenForClickOn(R.id.prefixes_button);
		additional_categories_shown = false;
	}

	public void updateCategoriesShown() {
		View additionalUnitsField = findViewById(R.id.additional_units_layout);
		View additionalUnitsLabel = findViewById(R.id.additional_units_label);
		if (additional_categories_shown) {
			additionalUnitsField.setVisibility(View.VISIBLE);
			additionalUnitsLabel.setVisibility(View.VISIBLE);
		} else {
			additionalUnitsField.setVisibility(View.GONE);
			additionalUnitsLabel.setVisibility(View.GONE);
		}
		additionalUnitsField.invalidate();
		additionalUnitsLabel.invalidate();
	}

	// Called when a button has been pressed or the checkbox has been checked.
	public void onClick(View v)
	{
		String category;
		switch(v.getId()) {
		case R.id.additional_units_chekbox:
			if(((CheckBox) v).isChecked()) {
				Log.d("Category", "Checkbox Checked.");
				if (!additional_categories_shown) {
					additional_categories_shown = true;
					updateCategoriesShown();
				}
			} else {
				Log.d("Category", "Checkbox not Checked");
				if (additional_categories_shown) {
					additional_categories_shown = false;
					updateCategoriesShown();
				}
			}
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
		case R.id.prefixes_button:
			Intent i = new Intent(this, MetricPrefixesListActivity.class);
			Log.d(TAG, "Starting MetricPrefixesListActivity");
			startActivity(i);
			return;
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

	// Restore the activity state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		View v = findViewById(R.id.additional_units_chekbox);
		if (((CheckBox)v).isChecked()) {
			additional_categories_shown = true;
			updateCategoriesShown();
		}
	}
}
