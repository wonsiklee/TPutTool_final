package com.android.LGSetupWizard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.TestResultDTO;
import com.android.LGSetupWizard.database.TestResultDBManager;

import java.util.ArrayList;

import android.util.Log;

public class TestResultListAdapter extends BaseAdapter {

    private static final String TAG = TestResultListAdapter.class.getSimpleName();
    private static TestResultDTO DUMMY_TEST_RESULT = new TestResultDTO();

    private Context mContext;
    private ArrayList<TestResultDTO> mTestResultLogList;

    public TestResultListAdapter(Context context) {
        this.mContext = context;
        this.initTestResultLogList();
        /*this.mTestResultLogList = TestResultDBManager.getInstance(this.mContext).fetch(TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO);
        this.mTestResultLogList.add(0, DUMMY_TEST_RESULT);*/
    }

    @Override
    public int getCount() {
        Log.d(TAG, "getCount() called ");
        return this.mTestResultLogList.size();
    }

    @Override
    public TestResultDTO getItem(int position) {
        return this.mTestResultLogList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView() " + position);
        ViewHolder sHolder = null;

        if (convertView == null) {
            sHolder = new ViewHolder();
            LayoutInflater li = LayoutInflater.from(this.mContext);
            convertView = li.inflate(R.layout.test_result_list_view_item, null);
            sHolder.mTxtViewIdx = convertView.findViewById(R.id.txtView_test_result_idx);
            sHolder.mTxtViewDate = convertView.findViewById(R.id.txtView_test_result_date_time);
            sHolder.mTxtViewCategory = convertView.findViewById(R.id.txtView_test_category);
            sHolder.mTxtViewValue = convertView.findViewById(R.id.txtView_test_result_value);

            convertView.setTag(sHolder);
        } else {
            sHolder = (ViewHolder) convertView.getTag();
        }

        if (position == 0) {

        } else {
            sHolder.mTxtViewIdx.setText(String.valueOf(this.mTestResultLogList.get(position).mIndex));
            sHolder.mTxtViewDate.setText(this.mTestResultLogList.get(position).mTestedTime.toString());
            sHolder.mTxtViewCategory.setText(this.mTestResultLogList.get(position).mTestedCategory.toString());
            sHolder.mTxtViewValue.setText(String.valueOf(this.mTestResultLogList.get(position).mTestResult));
        }

        return convertView;
    }

    public void updateDataSet(TestResultDBManager.TestCategory category) {
        this.initTestResultLogList();
        this.mTestResultLogList = TestResultDBManager.getInstance(this.mContext).fetch(TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO);
        this.mTestResultLogList.add(0, DUMMY_TEST_RESULT);

        this.notifyDataSetChanged();
    }

    private void initTestResultLogList() {
        this.mTestResultLogList = null;
        this.mTestResultLogList = new ArrayList<>();
        this.mTestResultLogList.add(DUMMY_TEST_RESULT);
    }

    private class ViewHolder {
        public LinearLayout mLinearLayoutItemWrapper;
        public TextView mTxtViewIdx;
        public TextView mTxtViewDate;
        public TextView mTxtViewCategory;
        public TextView mTxtViewValue;
    }
}
