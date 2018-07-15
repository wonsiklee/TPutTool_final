package com.android.LGSetupWizard.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

public class NumberFormatFilter implements InputFilter {

    private final String TAG = NumberFormatFilter.class.getSimpleName();
    private EditText mTargetView;

    public NumberFormatFilter(EditText targetView) {
        this.mTargetView = targetView;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start == 0 && end == 1 && dstart == 0 && dend == 1 && TextUtils.equals(source, "0")) {
                return "1";
            }

            int input = Integer.parseInt(dest.toString() + source.toString());
            if (source.length() == 0) { // in case deletion
                if ((dend - dstart) == dest.length()) {
                    this.mTargetView.setSelection(0, this.mTargetView.getText().length());
                    return "1";
                } else {
                    return null;
                }
            }


            if (isInRange(1, 100, input)) {
                Log.d(TAG, "still in range");
                return source;
            } else {
                Log.d(TAG, "not in range");
                return "";
            }
        } catch (NumberFormatException nfe) {

        }
        return "";
    }

    private boolean isInRange(int min, int max, int value) {
        Log.d(TAG, "min : " + min + ", max : " + max + ", value : " + value);
        return max > min ? (value >= min && value <= max) : (value >= max && value <= min);
    }
}