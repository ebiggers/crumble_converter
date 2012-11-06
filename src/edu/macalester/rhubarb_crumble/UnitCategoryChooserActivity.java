package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;


public class UnitCategoryChooserActivity extends Activity implements OnClickListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unit_category_chooser);
		
		View additionalUnitsCheckBox = findViewById(R.id.additional_units_chekbox);
		additionalUnitsCheckBox.setOnClickListener(this);
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
		}
	}
}
