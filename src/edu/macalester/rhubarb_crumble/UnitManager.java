package edu.macalester.rhubarb_crumble;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// The UnitManager class is responsible for parsing one of the unit
// configuration files (in the res/raw directory) that specifies the list of
// units (such as "meters", "centimeters", etc.) in a unit category (such as
// "length").
public class UnitManager {

	private static String TAG = "UnitManager";

	// A regular expression to match the identifier name for a unit.  This is
	// separate from the localized name that's actually displayed to the user.
	private static String IDENTIFIER_NAME_REGEX = "[a-z0-9_]+";

	// A regular expression to match a floating point number.
	private static String FLOATING_POINT_REGEX =
				"[-+]?(?:(?:[0-9]+(?:\\.[0-9]*)?)|(?:\\.[0-9]+))(?:[eE][-+]?[0-9]+)?";

	// A regular expression to match an integer.
	private static String INTEGER_REGEX = "[0-9]+";

	// Load a category of units from the corresponding configuration file in the
	// 'res/raw' directory.
	//
	// @category:
	// 		The name of the unit category (such as "length")
	//
	// @context:
	// 		Context of the Android application.
	//
	// @maxVisibilityLevel:
	// 		Only return units having a visibility level of this or less.
	//
	// Returns:
	// 		An ArrayList of Units in the category.  If the configuration file
	// 		cannot even be opened, an empty list of units is returned.  If any
	// 		lines in the configuration file are invalid, an error message is
	// 		printed, but the rest of the units that were read are still
	// 		returned.
	public static ArrayList<Unit> getUnits(String category, Context context,
										   int maxVisibilityLevel)
	{
		Resources resources = context.getResources();
		String filename = category + ".csv";
		ArrayList<Unit> units = new ArrayList<Unit>();
		String pkgname = context.getPackageName();

		Log.d(TAG, "Looking for resource " + filename + " in package " + pkgname);

		// Open the configuration file.
		int id = resources.getIdentifier(category, "raw", pkgname);
		InputStream is;
		try {
			is = resources.openRawResource(id);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Can't open resource file " + filename, e);
			return units;
		}
		BufferedReader rdr;
		try {
			rdr = new BufferedReader(new InputStreamReader(is, "US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Caught unexpected exception", e);
			try {
				is.close();
			} catch (IOException e2) { }
			return units;
		}

		// Regular expression to match:
		//	  <alphanumeric unit identifier name> , <floating point number> , <integer>
		Pattern pat = Pattern.compile("^\\s*(" + IDENTIFIER_NAME_REGEX + ")\\s*," +
									   "\\s*(" + FLOATING_POINT_REGEX  + ")\\s*," +
									   "\\s*(" + INTEGER_REGEX		   + ")\\s*$");
		try {
			String line;

			// For each line in the configurion file...
			while ((line = rdr.readLine()) != null) {

				// Skip empty lines and lines containing all whitespace.
				boolean all_whitespace = true;
				for (int i = 0; i < line.length(); i++) {
					if (!Character.isWhitespace(line.charAt(i))) {
						all_whitespace = false;
						break;
					}
				}
				if (all_whitespace)
					continue;

				// Skip lines beginning with the '#' character.
				if (line.charAt(0) == '#')
					continue;

				// Otherwise, this line is an actual unit entry, so match the
				// line against the regular expression from above, and extract
				// the fields as the groups from the matched text.
				Matcher m = pat.matcher(line);
				if (m.matches()) {
					String str_identifier_name  = m.group(1);
					String str_normalized_value = m.group(2);
					String str_visibility_level = m.group(3);

					int visibility_level = Integer.parseInt(str_visibility_level);

					if (visibility_level <= maxVisibilityLevel) {
						// Create a new Unit and add it to the units list.
						Unit unit = new Unit(context, str_identifier_name,
											 Double.parseDouble(str_normalized_value),
											 visibility_level);
						units.add(unit);
					}
				} else {
					Log.e(TAG, "Invalid line in resource file " + filename +
							   ": \"" + line + "\"");
				}
			}
		} catch (IOException e) {
			// @units is still returned if there is an IOException, but it may
			// only partially complete.
			Log.e(TAG, "Error reading resource file " + filename, e);
		}
		try {
			is.close();
		} catch (IOException e) { }
		return units;
	}
};
