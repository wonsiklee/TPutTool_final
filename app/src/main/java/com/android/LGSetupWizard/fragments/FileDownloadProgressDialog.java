package com.android.LGSetupWizard.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.android.LGSetupWizard.R;

public class FileDownloadProgressDialog extends Dialog implements View.OnClickListener, Dialog.OnClickListener{

    private static final String TAG = FileDownloadProgressDialog.class.getSimpleName();

    public FileDownloadProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.download_progress_popup);
        this.setCanceledOnTouchOutside(false);
        Button btn = this.findViewById(R.id.btn_progress_popup_stop);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "dialog on click");
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "asdf");
    }
}
