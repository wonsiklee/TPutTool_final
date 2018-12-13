package com.android.LGSetupWizard.ui.popup;

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
import android.widget.Toast;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.TestResultListAdapter;
import com.android.LGSetupWizard.database.TestResultDBManager;

public class TestResultPopupWindow extends PopupWindow implements View.OnClickListener {

    private static final String TAG = TestResultPopupWindow.class.getSimpleName();
    private final TestResultListAdapter mTestResultListAdapter;

    private Context mContext;
    private View mPopupViewTestResultContentView;

    private ListView mListViewTestResults;

    private Button mBtnExportToFile;
    private Button mBtnDeleteResults;

    private TestResultDBManager.TestCategory mCategory;

    public TestResultPopupWindow(Context context) {
        super();
        this.mContext = context;

        this.mPopupViewTestResultContentView = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_view_test_result, null);

        this.mPopupViewTestResultContentView.findViewById(R.id.btn_dismiss_result).setOnClickListener(this);
        this.mBtnExportToFile = this.mPopupViewTestResultContentView.findViewById(R.id.btn_export_test_result_to_file);
        this.mBtnExportToFile.setOnClickListener(this);

        this.mBtnDeleteResults = this.mPopupViewTestResultContentView.findViewById(R.id.btn_delete_results);
        this.mBtnDeleteResults.setOnClickListener(this);

        this.mTestResultListAdapter = new TestResultListAdapter(this.mContext);
        this.mListViewTestResults = this.mPopupViewTestResultContentView.findViewById(R.id.listView_test_result);
        this.mListViewTestResults.setAdapter(this.mTestResultListAdapter);
        this.mListViewTestResults.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
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
                            }).setNegativeButton("삭제",

                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TestResultDBManager.getInstance(mContext).delete(mTestResultListAdapter.getItem(position).mIndex);
                                    TestResultPopupWindow.this.mTestResultListAdapter.updateDataSet(mCategory);
                                    return;
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
        this.mCategory = category;
        this.mTestResultListAdapter.updateDataSet(this.mCategory);

        this.setAnimationStyle(R.style.IperfSwitchTextAppearance);
        this.showAtLocation(parentView, Gravity.CENTER, 0, 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dismiss_result:
                this.dismiss();
                break;
            case R.id.btn_export_test_result_to_file:
                TestResultDBManager.getInstance(mContext).exportResults();
                break;
            case R.id.btn_delete_results:
                AlertDialog.Builder sAlertDeleteResults = new AlertDialog.Builder(mContext);
                sAlertDeleteResults.setMessage("결과를 모두 삭제하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TestResultDBManager.getInstance(mContext).delete(TestResultDBManager.ID_ALL);
                                TestResultPopupWindow.this.mTestResultListAdapter.updateDataSet(mCategory);
                                Toast.makeText(mContext, "결과가 모두 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog alert = sAlertDeleteResults.create();
                alert.show();
                break;
        }
    }
}
