package com.android.LGSetupWizard.database;


import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.util.Log;

import com.android.LGSetupWizard.R;

public class TestResultLogPopupWindow extends PopupWindow {

    private static final String TAG = TestResultLogPopupWindow.class.getSimpleName();

    private View mPopupViewTestResultContentView;
    private Context mContext;

    public TestResultLogPopupWindow(Context context) {
        super();
        this.mContext = context;
        this.mPopupViewTestResultContentView = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_view_test_result, null);
        this.mPopupViewTestResultContentView.findViewById(R.id.btn_dismiss_result).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        this.setContentView(this.mPopupViewTestResultContentView);

        DisplayMetrics metrics = new DisplayMetrics();
        (((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()).getMetrics(metrics);

        this.setWidth((int)(metrics.widthPixels * 0.8f));
        this.setHeight((int)(metrics.heightPixels * 0.8f));

        this.setFocusable(true);
    }

    public void show(View parentView, TestResultLogDBManager.TestCategory category) {
        Log.d(TAG, "show() " + category);
        super.showAsDropDown(parentView);
        // TODO : need to fetch category data from the DB.
    }
}
