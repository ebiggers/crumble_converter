package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.os.Bundle;

public class MetricPrefixesListActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the content view to metric_prefixes.xml
		setContentView(R.layout.metric_prefixes_list);
	}

}
