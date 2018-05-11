package com.android.LGSetupWizard.fragments;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.LGSetupWizard.R;

import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class LGFTPFileDownloadProgressDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = LGFTPFileDownloadProgressDialog.class.getSimpleName();

    private TextView mTxtViewPercentage;
    private Button mBtnCancelDownload;
    private ProgressBar mProgressBar;
    private TextView mTxtViewAvgTput;
    private TextView mTxtViewBytesTransferred;
    private TextView mTxtViewDownloadingFileName;
    private TextView mTxtViewDownloadingFileCount;

    /*@Setter @Getter    private int mTotalFileCount;
    @Setter @Getter    private int mCurrentFileCount;*/

    public LGFTPFileDownloadProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.download_progress_popup);
        this.setCanceledOnTouchOutside(false);

        this.mBtnCancelDownload = this.findViewById(R.id.btn_progress_popup_stop);
        this.mBtnCancelDownload.setOnClickListener(this);

        this.mTxtViewPercentage = this.findViewById(R.id.txtView_download_progress_percentage);

        this.mProgressBar = this.findViewById(R.id.progressBar_download_progress);

        this.mTxtViewAvgTput = this.findViewById(R.id.txtView_download_progress_avg_tput);
        this.mTxtViewBytesTransferred = this.findViewById(R.id.txtView_downloading_mbytes_transferred);

        this.mTxtViewDownloadingFileName = this.findViewById(R.id.txtView_downloading_file_name);
        this.mTxtViewDownloadingFileCount = this.findViewById(R.id.txtView_download_progress_file_count);
    }

    @Override
    public void onClick(View v) {
        this.dismissDialog();
    }

    @Override
    public void onBackPressed() {
        this.dismissDialog();
    }

    public void updateProgressValue(float percentage, long bytesTransferred, float tputValue) {
        this.mProgressBar.setProgress((int) percentage);
        this.mTxtViewPercentage.setText(String.format("%.2f", percentage) + " %");
        this.mTxtViewBytesTransferred.setText("Transferred : " + String.format("%.2f", bytesTransferred / 1024.0f / 1024) + " MBytes");
        this.mTxtViewAvgTput.setText("Avg TPut : " + String.format("%.2f", tputValue) + " Mbps");
    }

    public void updateFileCount(int totalCount, int currentFileCount) {
        this.mTxtViewDownloadingFileCount.setText(currentFileCount + " / " + totalCount);
    }

    public void setDownloadingFileName(String downloadingFileName) {
        this.mTxtViewDownloadingFileName.setText(downloadingFileName);
    }

    private void dismissDialog() {
        Log.d(TAG, "dismiss()");
        this.dismiss();
    }
}
