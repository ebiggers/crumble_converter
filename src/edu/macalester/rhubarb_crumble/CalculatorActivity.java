package edu.macalester.rhubarb_crumble;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;

// The Activity that runs while the user is using the calculator.
public class CalculatorActivity extends Activity
								implements OnClickListener
{
	// No operation currently selected
	private static final int OP_NONE = 0;

	// Addition operation
	private static final int OP_ADD = 1;

	// Subtraction operation
	private static final int OP_SUB = 2;

	// Multiplication operation
	private static final int OP_MUL = 3;

	// Division operation
	private static final int OP_DIV = 4;

	private static final String TAG = "CalculatorActivity";

	// View for the output of the calculator
	private TextView displayView;

	// The number that's currently being entered into the calculator
	private String num1;

	// Any previous number that has been entered into the calculator and has
	// been saved (for example, a number that was entered before a binary
	// operation has been pressed; or a number that was obtained as a result of
	// an operation.).
	private String num2;

	// The current operation that has been entered.
	private int op;

	// true iff @num1 currently indicates a valid number.
	private boolean num1_valid;

	// Called when the CalculatorActivity is created.
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calculator);

		// Retrieve a reference to the TextView field for the calculator display.
		displayView = (TextView) findViewById(R.id.txtResultId);

		clear_calculator();
	}

	// Save the calculator state
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putString("display", displayView.getText().toString());
		savedInstanceState.putInt("op", op);
		savedInstanceState.putString("num1", num1);
		savedInstanceState.putString("num2", num2);
		savedInstanceState.putBoolean("num1_valid", num1_valid);
	}

	// Restore the calculator state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		op = savedInstanceState.getInt("op", OP_NONE);
		num1 = savedInstanceState.getString("num1");
		if (num1 == null)
			num1 = "";
		num2 = savedInstanceState.getString("num2");
		if (num2 == null)
			num2 = "";
		num1_valid = savedInstanceState.getBoolean("num1_valid", false);
		String display = savedInstanceState.getString("display");
		if (display != null)
			displayView.setText(display);
	}

	// Reset the calculator to its initial state.
	private void clear_calculator() {
		num1 = "";
		num2 = "";
		displayView.setText("");
		op = OP_NONE;
		num1_valid = false;
	}

	// Given the ID of a button that was pressed on the calculator, return the
	// ID of the operator that the button is labeled with, or OP_NONE if the
	// button is not labeled with an operator.
	private int buttonIdToOperator(int id) {
		switch (id) {
		case R.id.btnAddId:
			return OP_ADD;
		case R.id.btnSubId:
			return OP_SUB;
		case R.id.btnMulId:
			return OP_MUL;
		case R.id.btnDivId:
			return OP_DIV;
		default:
			return OP_NONE;
		}
	}

	// On-click event handler for all the calculator buttons
	@Override
	public void onClick(View view) {
		String display = null;
		int viewId = view.getId();

		switch (viewId) {
			// Digit buttons
			case R.id.btnNum0Id:
			case R.id.btnNum1Id:
			case R.id.btnNum2Id:
			case R.id.btnNum3Id:
			case R.id.btnNum4Id:
			case R.id.btnNum5Id:
			case R.id.btnNum6Id:
			case R.id.btnNum7Id:
			case R.id.btnNum8Id:
			case R.id.btnNum9Id:
				String digit = ((Button) view).getText().toString();
				if (num1.equals("0")) {
					// If "0" is currently entered, replace it with the entered
					// number.
					num1 = digit;
				} else {
					// Otherwise, append the digit to the already-entered
					// number.
					num1 += digit;
				}
				// @num1 will always be valid after a digit has been entered.
				num1_valid = true;
				display = num1;
				break;

			// Decimal point button
			case R.id.btnDecimalPointId:
				// Only allow entering a decimal point if a decimal point has
				// not already been entered.
				if (num1.indexOf('.') == -1) {
					num1 += ".";
					display = num1;
				}
				// @num1_valid is not set because a single decimal point without
				// any trailing digits is not a valid number.
				break;

			// Binary operators: + - x /
			case R.id.btnAddId:
			case R.id.btnSubId:
			case R.id.btnMulId:
			case R.id.btnDivId:
				if (num1_valid) {
					if (num2.length() != 0 && op != OP_NONE) {
						// If @num1 is valid and there is also a previous
						// operator entered and something saved in @num2, the
						// previous operation needs to be made before saving the
						// new one.
						makeBinaryOperation();
					} else {
						// If @num1 is valid but there is no previous operation
						// to make at this point, push @num1 back to @num2.
						// Note that the calculator display is *not* modified,
						// even though it will be showing the number that has
						// been pushed back to @num2.
						num2 = num1;
						num1 = "";
						num1_valid = false;
					}
					op = buttonIdToOperator(viewId);
				} else if (num1.length() == 0) {
					// If an operator is entered when @num1 is empty, save the
					// operator, unless @num2 is also empty, in which case there
					// is no point to save the operator because there are no
					// numbers for it to work on.
					if (num2.length() != 0) {
						op = buttonIdToOperator(viewId);
					}
				}
				break;

			// Equals button
			case R.id.btnEqualId:
				if (num2.length() != 0 && num1_valid && op != OP_NONE) {
					// If the user has entered a number, an operator, and a
					// number, make the operation when = is pressed, as
					// expected.
					makeBinaryOperation();
				} else if (num1_valid) {
					// Pressing equals when an operation cannot be made has the
					// effect of pushing @num1 back to @num2.
					num2 = num1;
					num1 = "";
					num1_valid = false;
					op = OP_NONE;
				}
				break;

			// Clear button
			case R.id.btnClearId:
				clear_calculator();
				break;

			// Del button
			case R.id.btnDelId:
				if (num1.length() != 0) {
					num1 = num1.substring(0, num1.length() - 1);
					if (num1.length() < 2) {
						if (num1.length() == 0 || num1.charAt(0) == '.') {
							num1_valid = false;
						}
					}
					display = num1;
				}
				break;
		}
		if (display != null)
			displayView.setText(display);
	}

	//
	// Make a binary operation (addition, subtraction, multiplication, division,
	// etc.).
	//
	// Preconditions:
	//		* @num1 and @num2 are both strings for valid double-precision
	//		  floating point numbers.
	//
	//		* @op is a valid operator other than OP_NONE.
	//
	// The calculator display is updated with the result of the operation, @num2
	// is set to text of the result, @num1 is set to the empty string, and
	// @num1_valid is set to false.
	private void makeBinaryOperation() {
		double n1, n2;
		try {
			n1 = Double.parseDouble(num1);
			n2 = Double.parseDouble(num2);
		} catch (NumberFormatException e) {
			displayView.setText("ERROR");
			Log.e(TAG, "Error parsing double", e);
			return;
		}
		double result;
		switch (op) {
		case OP_ADD:
			result = n2 + n1;
			break;
		case OP_SUB:
			result = n2 - n1;
			break;
		case OP_DIV:
			result = n2 / n1;
			break;
		case OP_MUL:
			result = n2 * n1;
			break;
		default:
			assert(false);
			result = 0;
			break;
		}
		num2 = Double.toString(result);
		num1 = "";
		op = OP_NONE;
		num1_valid = false;
		displayView.setText(num2);
	}
}
