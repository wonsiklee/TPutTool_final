package com.android.LGSetupWizard.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.TestResultDTO;
import com.android.LGSetupWizard.database.TestResultDBManager;

import java.util.ArrayList;

public class TestResultListAdapter extends BaseAdapter {

    private static final String TAG = TestResultListAdapter.class.getSimpleName();
    private static TestResultDTO DUMMY_TEST_RESULT = new TestResultDTO();

    private Context mContext;
    private ArrayList<TestResultDTO> mTestResultLogList;

    public TestResultListAdapter(Context context) {
        this.mContext = context;
        this.initTestResultLogList();
    }

    @Override
    public int getCount() {
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
        ViewHolder sHolder = null;

        if (convertView == null) {
            sHolder = new ViewHolder();
            LayoutInflater li = LayoutInflater.from(this.mContext);
            convertView = li.inflate(R.layout.test_result_list_view_item, null);
            sHolder.mTxtViewIdx = convertView.findViewById(R.id.txtView_test_result_idx);
            sHolder.mTxtViewDate = convertView.findViewById(R.id.txtView_test_result_date_time);
            sHolder.mTxtViewFileIO = convertView.findViewById(R.id.txtView_test_category);
            sHolder.mTxtViewValue = convertView.findViewById(R.id.txtView_test_result_value);

            convertView.setTag(sHolder);
        } else {
            sHolder = (ViewHolder) convertView.getTag();
        }

        if (position == 0) {
            sHolder.mTxtViewIdx.setBackgroundColor(Color.LTGRAY);
            sHolder.mTxtViewDate.setBackgroundColor(Color.LTGRAY);
            sHolder.mTxtViewFileIO.setBackgroundColor(Color.LTGRAY);
            sHolder.mTxtViewValue.setBackgroundColor(Color.LTGRAY);
        } else {
            sHolder.mTxtViewIdx.setText(String.valueOf(position));
            sHolder.mTxtViewDate.setText(this.mTestResultLogList.get(position).mTestedTime.toString());
            String fileIO;
            String testCategory = this.mTestResultLogList.get(position).mTestedCategory.toString();
            if (testCategory.contains("WITHOUT")) {
                fileIO = "X";
            } else if (testCategory.contains("WITH")) {
                fileIO = "O";
            } else {
                fileIO = "N/A";
            }
            sHolder.mTxtViewFileIO.setText(fileIO);
            sHolder.mTxtViewValue.setText(String.format("%.2f", this.mTestResultLogList.get(position).mTestResult));
        }

        return convertView;
    }

    public void updateDataSet(TestResultDBManager.TestCategory category) {
        this.initTestResultLogList();
        this.mTestResultLogList = TestResultDBManager.getInstance(this.mContext).fetch(category);
        this.mTestResultLogList.add(0, DUMMY_TEST_RESULT);

        this.notifyDataSetChanged();
    }

    private void initTestResultLogList() {
        this.mTestResultLogList = null;
        this.mTestResultLogList = new ArrayList<>();
        this.mTestResultLogList.add(DUMMY_TEST_RESULT);
    }

    private class ViewHolder {
        public TextView mTxtViewIdx;
        public TextView mTxtViewDate;
        public TextView mTxtViewFileIO;
        public TextView mTxtViewValue;
    }
}
