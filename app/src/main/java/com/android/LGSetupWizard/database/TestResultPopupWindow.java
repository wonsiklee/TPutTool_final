package com.android.LGSetupWizard.database;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.TestResultListAdapter;

public class TestResultPopupWindow extends PopupWindow implements View.OnClickListener {

    private static final String TAG = TestResultPopupWindow.class.getSimpleName();
    private final TestResultListAdapter mTestResultListAdapter;

    private Context mContext;
    private View mPopupViewTestResultContentView;

    private ListView mListViewTestResults;

    private Button mBtnExportToFile;

    public TestResultPopupWindow(Context context) {
        super();
        this.mContext = context;

        this.mPopupViewTestResultContentView = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_view_test_result, null);

        this.mPopupViewTestResultContentView.findViewById(R.id.btn_dismiss_result).setOnClickListener(this);
        this.mBtnExportToFile = this.mPopupViewTestResultContentView.findViewById(R.id.btn_export_test_result_to_file);
        this.mBtnExportToFile.setOnClickListener(this);

        this.mTestResultListAdapter = new TestResultListAdapter(this.mContext);
        this.mListViewTestResults = this.mPopupViewTestResultContentView.findViewById(R.id.listView_test_result);
        this.mListViewTestResults.setAdapter(this.mTestResultListAdapter);
        this.mListViewTestResults.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    return true;
                } else {
                    new AlertDialog.Builder(mContext)
                            .setTitle("Description")
                            .setMessage(mTestResultListAdapter.getItem(position).mTestDescription)
                            .setNeutralButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).show();
                    return false;
                }
            }
        });

        this.setContentView(this.mPopupViewTestResultContentView);

        DisplayMetrics metrics = new DisplayMetrics();
        (((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()).getMetrics(metrics);

        this.setWidth((int)(metrics.widthPixels * 0.8f));
        this.setHeight((int)(metrics.heightPixels * 0.8f));

        this.setFocusable(true);
    }

    public void show(View parentView, TestResultDBManager.TestCategory category) {
        this.mTestResultListAdapter.updateDataSet(category);

        this.setAnimationStyle(R.style.IperfSwitchTextAppearance);
        this.showAtLocation(parentView, Gravity.CENTER, (int) parentView.getX(), (int) parentView.getY());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dismiss_result:
                TestResultPopupWindow.this.dismiss();
                break;
            case R.id.btn_export_test_result_to_file:
                break;
        }
    }
}
