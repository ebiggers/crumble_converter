package edu.macalester.rhubarb_crumble;
 
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;
 
public class CalculatorActivity extends Activity
								implements OnClickListener
{

	private static final int OP_NONE = 0;
	private static final int OP_ADD = 1;
	private static final int OP_SUB = 2;
	private static final int OP_MUL = 3;
	private static final int OP_DIV = 4;

	private static final String TAG = "CalculatorActivity";

	private TextView displayView;
	private String num1;
	private String num2;
	private int op;
	private boolean num1_valid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calculator);

		// Retrieve a reference to the TextView field for the calucator display.
		displayView = (TextView) findViewById(R.id.txtResultId);

		clear_calculator();
	}

	private void clear_calculator() {
		num1 = "";
		num2 = "";
		displayView.setText("");
		op = OP_NONE;
		num1_valid = false;
	}

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

	// On-click event handler for all the buttons
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
				if (!num1.equals("0")) {
					String digit = ((Button) view).getText().toString();
					num1 += digit;
					num1_valid = true;
					display = num1;
				}
				break;


            //Decimal point button
            case R.id.btnDecimalPointId:
                if (num1.indexOf('.') == -1) {
                    num1 += ".";
                    display = num1;
                }
                break;

			// Binary operators: + - x /
			case R.id.btnAddId:
			case R.id.btnSubId:
			case R.id.btnMulId:
			case R.id.btnDivId:
				if (num1_valid) {
					if (num2.length() != 0 && op != OP_NONE) {
						makeBinaryOperation();
					} else {
						num2 = num1;
						num1 = "";
						num1_valid = false;
					}
					op = buttonIdToOperator(viewId);
				} else {
					if (num2.length() != 0) {
						op = buttonIdToOperator(viewId);
					}
				}
				break;

			// Equals button
			case R.id.btnEqualId:
				if (num2.length() != 0 && num1_valid && op != OP_NONE) {
					makeBinaryOperation();
				} else if (num1_valid) {
					num2 = num1;
					num1 = "";
					num1_valid = false;
					op = OP_NONE;
					display = num2;
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

	private void makeBinaryOperation() {
		double n1, n2;
		try {
			n1 = Double.parseDouble(num1);
			n2 = Double.parseDouble(num2);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Error parsing double", e);
			n1 = 0.0;
			n2 = 0.0;
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
