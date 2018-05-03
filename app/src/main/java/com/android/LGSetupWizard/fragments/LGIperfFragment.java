package com.android.LGSetupWizard.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGIperfClient;
import com.android.LGSetupWizard.clients.LGNIAClient;
import com.android.LGSetupWizard.clients.LGNIADownloadStateChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

public class LGIperfFragment extends Fragment {
    private static final String TAG = LGIperfFragment.class.getSimpleName();
    private View mView;
    private LGIperfClient mLGIperfClient;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_nia, container, false);
            //this.initUIControls();

            mLGIperfClient = new LGIperfClient();
        }
        return mView;
    }

}