package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "MainActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Launching Crumble Converter");
		setContentView(R.layout.main);

		/** Set onClickListeners for the three buttons on the main screen. */
		View currencyButton = findViewById(R.id.currency_button);
		assert currencyButton != null;
		currencyButton.setOnClickListener(this);

		View unitsButton = findViewById(R.id.units_button);
		unitsButton.setOnClickListener(this);

		View calculatorButton = findViewById(R.id.calculator_button);
		calculatorButton.setOnClickListener(this);
	}

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.currency_button:
			Log.d(TAG, "Starting CurrencyConverterActivity");
			startActivity(new Intent(this, CurrencyConverterActivity.class));
			break;
		case R.id.units_button:
			Log.d(TAG, "Starting UnitCategoryChooserActivity");
			startActivity(new Intent(this, UnitCategoryChooserActivity.class));
			break;
		case R.id.calculator_button:
			Log.d(TAG, "Starting CalculatorActivity");
			startActivity(new Intent(this, CalculatorActivity.class));
		}
	}
}
