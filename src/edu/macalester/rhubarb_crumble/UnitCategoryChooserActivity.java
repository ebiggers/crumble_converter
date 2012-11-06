package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.view.animation.AnimationUtils;
import android.view.View.OnClickListener;

public class UnitCategoryChooserActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unit_category_chooser);
		View additionalUnitsLabel = findViewById(R.id.additional_units_label);
		//RotateAnimation animate = (RotateAnimation)AnimationUtils.loadAnimation(this, R.animator.rotate90);
		//animate.setFillAfter(true);
		//additionalUnitsLabel.setAnimation(animate);
		
	}
}
