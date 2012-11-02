package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.os.Bundle;

public class CurrencyConverterActivity extends Activity {

	private CurrencyRatesManager rates_manager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currency_converter);
		rates_manager = new CurrencyRatesManager(this);
	}

}
