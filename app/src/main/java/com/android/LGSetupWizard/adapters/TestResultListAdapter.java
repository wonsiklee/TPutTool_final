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

public class TestResultListAdapter extends BaseAdapter {

    private static final String TAG = TestResultListAdapter.class.getSimpleName();

    private Context mContext;
    private ArrayList<TestResultDTO> mTestResultLogList;

    public TestResultListAdapter(Context context) {
        this.mContext = context;
        this.mTestResultLogList = new ArrayList<>();

        this.mTestResultLogList = TestResultDBManager.getInstance(this.mContext).fetch(TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO);
    }

    @Override
    public int getCount() {
        return this.mTestResultLogList.size() + 1;
    }

    @Override
    public TestResultDTO getItem(int position) {
        return this.mTestResultLogList.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position - 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder sHolder = null;

        if (convertView == null) {
            sHolder = new ViewHolder();
            LayoutInflater li = LayoutInflater.from(this.mContext);
            convertView = li.inflate(R.layout.test_result_list_view_item, null);
            /*convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d(TAG, "clicked()");
                }
            });*/
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
            sHolder.mTxtViewIdx.setText(String.valueOf(this.mTestResultLogList.get(position - 1).mIndex));
            sHolder.mTxtViewDate.setText(this.mTestResultLogList.get(position - 1).mTestedTime.toString());
            sHolder.mTxtViewCategory.setText(this.mTestResultLogList.get(position - 1).mTestedCategory.toString());
            sHolder.mTxtViewValue.setText(String.valueOf(this.mTestResultLogList.get(position - 1).mTestResult));
        }

        return convertView;
    }

    private class ViewHolder {
        public LinearLayout mLinearLayoutItemWrapper;
        public TextView mTxtViewIdx;
        public TextView mTxtViewDate;
        public TextView mTxtViewCategory;
        public TextView mTxtViewValue;
    }
}
