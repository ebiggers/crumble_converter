package edu.macalester.rhubarb_crumble;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

// Represents a unit that's available in the unit converter.
public class Unit {

	// The name of the unit that will be displayed to the user.
	private String localizedName;

	// The abbreviation of the unit that will be displayed to the user.
	private String abbreviatedName;

	// The value of the unit, relative to some standard unit that measures the
	// same thing (e.g. lengths are all relative to meters)
	private double normalizedValue;

	// The higher this number, the more uncommon the unit is.
	private int visibilityLevel;

	private static String TAG = "CrumbleConverterUnit";

	public Unit(Context context, String identifier_name, double normalizedValue,
				int visibilityLevel)
	{
		Resources resources = context.getResources();
		String pkgname = context.getPackageName();
		try {
			int nameId = resources.getIdentifier(identifier_name, "string", pkgname);
			this.localizedName = resources.getString(nameId);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Can't find unit: " + identifier_name, e);
			this.localizedName = identifier_name;
		}

		try {
			int abbrevId = resources.getIdentifier(identifier_name + ".abbrev",
												   "string", pkgname);
			this.abbreviatedName = resources.getString(abbrevId);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Can't find unit abbreviation: " + identifier_name, e);
			this.abbreviatedName = identifier_name;
		}

		this.normalizedValue = normalizedValue;
		this.visibilityLevel = visibilityLevel;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Unit))
			return false;
		return getLocalizedName().equals(((Unit)other).getLocalizedName());
	}

	public String getLocalizedName() {
		return localizedName;
	}

	public String getLocalizedAbbreviation() {
		return abbreviatedName;
	}

	public double getNormalizedValue() {
		return normalizedValue;
	}

	public int getVisibilityLevel() {
		return visibilityLevel;
	}
}
