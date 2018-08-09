package com.android.LGSetupWizard.ui.dialog;

import android.app.Dialog;

/**
 * Created by hyukbin.ko on 2018-05-21.
 */

public interface OnSetDialogListener {
    void onSelect(Dialog dialog, Object selectedValue);
    void onSelect(Dialog dialog, Object selectedValue, int selectedPosition);
    void onCancel(Dialog dialog);
    void onDelete(Dialog dialog);
}
