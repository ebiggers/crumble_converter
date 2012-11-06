package edu.macalester.rhubarb_crumble;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class Unit {
	
	String localizedName;
	String abbreviatedName;
	double normValue;
	int visibility;
	
	private static String TAG = "CrumbleConverterUnit";
	
	public Unit(Context context, String name, double normalizedValue, int visibilityLevel) {
		try {
			int nameId = context.getResources().getIdentifier(name, "string", "edu.macalester.rhubarb_crumble");
			localizedName = context.getResources().getString(nameId);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Can't find unit: " + name, e);
			localizedName = name;
		}
		
		try {
			int abbrevId = context.getResources().getIdentifier(name + ".abbrev", "string", "edu.macalester.rhubarb_crumble");
			abbreviatedName = context.getResources().getString(abbrevId);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Can't find unit abbreviation: " + name, e);
			abbreviatedName = name;
		}
		
		normValue = normalizedValue;
		visibility = visibilityLevel;
	}
	
	public String getLocalizedName() {
		return localizedName;
	}
	
	public String getLocalizedAbbreviation() {
		return abbreviatedName;
	}
	
	public double getNormalizedValue() {
		return normValue;
	}
	
	public int visibility() {
		return visibility;
	}

}
