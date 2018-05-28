package com.android.LGSetupWizard.fragments.Dialog;

/**
 * Created by hyukbin.ko on 2018-05-21.
 */

public interface OnSetDialogListener {
    void onSelect(Object selectedValue);
    void onSelect(Object selectedValue, int selectedPosition);
    void onCancel();
    void onDelete();
}
