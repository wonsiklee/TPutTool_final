package com.android.LGSetupWizard.fragments;

import com.android.LGSetupWizard.IDeviceMonitoringService;
import com.android.LGSetupWizard.IDeviceMonitoringServiceCallback;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.statsreader.CPUStatsReader;
import com.android.LGSetupWizard.adapters.PackageNameSpinnerAdapter;
import com.android.LGSetupWizard.service.DeviceMonitoringService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConfigurationFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, View.OnFocusChangeListener, AdapterView.OnItemSelectedListener {

    static final String TAG = ConfigurationFragment.class.getSimpleName();

    private View mView;
    private ImageButton mImgBtnRecordingController;
    private Button mBtnLoggingController;

    private EditText mEditTxtPackageName;
    private EditText mEditTxtInterval;

    private RadioButton mRdoBtnChipsetVendorDefault;
    private RadioButton mRdoBtnChipsetVendorManual;
    private EditText mEditTxtCPUClockPath;

    private RadioButton mRdoBtnThermalVts;
    private RadioButton mRdoBtnThermalManual; // TODO : manual input should be implemented
    private EditText mEditTxtThermalPath;

    private RadioButton mRdoBtnDL;
    private RadioButton mRdoBtnUL;
    private int mDirection;
    private int mThermalType;

    private EditText mEditTxtInstantaneousTputCalculationDurationTime;
    private EditText mEditTxtTputThresholdValue;

    private TextView mTxtViewResult;

    private ArrayList<View> mUIControlList;

    private IDeviceMonitoringService mDeviceLoggingService;

    private Spinner mSpinnerPackageNames = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ConfigurationFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_configuration, container, false);
            this.initUIControls();

            Intent startIntent = new Intent(this.getContext(), DeviceMonitoringService.class);
            startIntent.setAction("com.lge.data.START_LOGGING");
            this.getContext().bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
        return mView;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick()");
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        Log.d(TAG, "onFocusChange()");
        switch (view.getId()) {

            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.radioButton_chipset_default:
                if (isChecked) {
                    this.mEditTxtCPUClockPath.setText("");
                    this.mEditTxtCPUClockPath.setVisibility(View.GONE);
                }
                break;
            case R.id.radioButton_chipset_manual:
                if (isChecked) {
                    this.mEditTxtCPUClockPath.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.radioButton_thermal_vts:
                if (isChecked) {
                    this.mEditTxtThermalPath.setVisibility(View.GONE);
                    this.mEditTxtThermalPath.setText(CPUStatsReader.getVtsFilePath());
                }
                break;
            case R.id.radioButton_thermal_manual_input:
                if (isChecked) {
                    this.mEditTxtThermalPath.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.radioButton_dl_direction:
                mDirection = DeviceMonitoringService.SHARED_PREFERENCES_DL_DIRECTION;
                break;
            case R.id.radioButton_ul_direction:
                mDirection = DeviceMonitoringService.SHARED_PREFERENCES_UL_DIRECTION;
                break;

            default:
                break;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttache(Context)");
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (this.mConnection != null) {
            this.getContext().unbindService(this.mConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected() : " + componentName);
            try {
                if (service != null) {
                    mDeviceLoggingService = IDeviceMonitoringService.Stub.asInterface(service);
                    mDeviceLoggingService.registerCallback(mCallback);
                    mBtnLoggingController.setEnabled(true);
                    refreshMonitoringButtons();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected()");
        }
    };

    private View.OnClickListener mStopMonitoringOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "mStopMonitoringOnClickListener onClick()");
            if (mDeviceLoggingService != null) {
                try {
                    if (mEditTxtPackageName.getText().toString().equals(getString(R.string.str_package_name_all_traffic))) {
                        mDeviceLoggingService.finishByTotalTrafficMonitoringLoop();
                    } else {
                        mDeviceLoggingService.finishByUidMonitoringLoop();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            refreshMonitoringButtons();
        }
    };

    private View.OnClickListener mStartMonitoringOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mStartMonitoringOnClickListener onClick()");
            UIValidationResult e = getUIValidationResult();

            boolean sSum = false;
            StringBuilder sb = new StringBuilder("아래의 입력 값을 확인하세요\n");
            if (e.isExceptionIncluded(UIValidationResult.UIException.PackageNameInvalid)) {
                sb.append("Package 명이 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.IntervalValueInvalid)) {
                sb.append("Interval 값이 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.InstantaneousTputCalculationTimeLengthInvalid)) {
                sb.append("기록 시작/종료 판단을 위한 시간 값이 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.TputThresholdValueInvalid)) {
                sb.append("기록 시작/종료 판단을 위한 Tput 값이 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.CPUFreqPathInvalid)) {
                sb.append("CPU Frequency path 가 올바르지 않습니다.\n");
                sSum = true;
            }

            Log.d(TAG, "vts zone number : " );
            /*if (e.isExceptionIncluded(UIValidationResult.UIException.CPUThermalPathInvalid)) {
                sb.append("CPU Thermal 경로가 올바르지 않습니다.");
                sSum = true;
            }*/

            if (sSum) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConfigurationFragment.this.getContext());
                alertDialogBuilder.setTitle("Invalid UI settings!!").setMessage(sb.toString()).setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return;
            }

            String sTemp = "";
            String sPackageName = mEditTxtPackageName.getText().toString();

            sTemp = mEditTxtInterval.getText().toString();
            int sInterval = (TextUtils.isEmpty(sTemp)) ? Integer.valueOf(mEditTxtInterval.getHint().toString()) : Integer.valueOf(sTemp);

            sTemp = mEditTxtCPUClockPath.getText().toString();
            String sCpuClockFilePath = (TextUtils.isEmpty(sTemp)) ? mEditTxtCPUClockPath.getHint().toString() : sTemp;

            sTemp = mEditTxtThermalPath.getText().toString();
            Log.d(TAG, "sTemp : " + sTemp);
            String sCpuThermalFilePath = (TextUtils.isEmpty(sTemp)) ? mEditTxtThermalPath.getHint().toString() : sTemp;

            sTemp = mEditTxtInstantaneousTputCalculationDurationTime.getText().toString();
            int sInstantaneousTputCalculationDurationTime = (TextUtils.isEmpty(sTemp)) ? Integer.valueOf(mEditTxtInstantaneousTputCalculationDurationTime.getHint().toString()) : Integer.valueOf(sTemp);

            sTemp = mEditTxtTputThresholdValue.getText().toString();
            float sTputThreshold = (TextUtils.isEmpty(sTemp)) ? Float.valueOf(mEditTxtTputThresholdValue.getHint().toString()) : Float.valueOf(sTemp);

            mDirection = mRdoBtnDL.isChecked() ? DeviceMonitoringService.SHARED_PREFERENCES_DL_DIRECTION : DeviceMonitoringService.SHARED_PREFERENCES_UL_DIRECTION;
            mThermalType = DeviceMonitoringService.THERMAL_VTS;
            try {
                if (sPackageName.equals(getString(R.string.str_package_name_all_traffic))) {
                    //int interval, String cpuFreqPath, String cpuThermalPath, int instantaneousTputCalculationTime, float tputThresholdValue, int direction, int thermalType
                    ConfigurationFragment.this.mDeviceLoggingService.fireUpByTotalTrafficMonitoringLoop(sInterval, sCpuClockFilePath, sCpuThermalFilePath, sInstantaneousTputCalculationDurationTime, sTputThreshold, mDirection, mThermalType, 0.1f);
                } else {
                    ConfigurationFragment.this.mDeviceLoggingService.fireUpByUidMonitoringLoop(sPackageName, sInterval, sCpuClockFilePath, sCpuThermalFilePath, sInstantaneousTputCalculationDurationTime, sTputThreshold, mDirection, mThermalType, 0.1f);
                }
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }

            refreshMonitoringButtons();
        }
    };

    View.OnClickListener mStartForcedRecordingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "mStartMonitoringOnClickListener onClick()");
            UIValidationResult e = getUIValidationResult();

            boolean sSum = false;
            StringBuilder sb = new StringBuilder("아래의 입력 값을 확인하세요\n");
            if (e.isExceptionIncluded(UIValidationResult.UIException.PackageNameInvalid)) {
                sb.append("Package 명이 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.IntervalValueInvalid)) {
                sb.append("Interval 값이 올바르지 않습니다.\n");
                sSum = true;
            }

            if (e.isExceptionIncluded(UIValidationResult.UIException.CPUFreqPathInvalid)) {
                sb.append("CPU Frequency path 가 올바르지 않습니다.\n");
                sSum = true;
            }
            if (e.isExceptionIncluded(UIValidationResult.UIException.CPUThermalPathInvalid)) {
                sb.append("CPU Thermal path가 올바르지 않습니다.");
                sSum = true;
            }

            if (sSum) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConfigurationFragment.this.getContext());
                alertDialogBuilder.setTitle("Invalid UI settings!!").setMessage(sb.toString()).setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return;
            }

            String sTemp = "";
            String sPackageName = mEditTxtPackageName.getText().toString();

            sTemp = mEditTxtInterval.getText().toString();
            int sInterval = (TextUtils.isEmpty(sTemp)) ? Integer.valueOf(mEditTxtInterval.getHint().toString()) : Integer.valueOf(sTemp);

            sTemp = mEditTxtCPUClockPath.getText().toString();
            String sCpuClockFilePath = (TextUtils.isEmpty(sTemp)) ? mEditTxtCPUClockPath.getHint().toString() : sTemp;

            sTemp = mEditTxtThermalPath.getText().toString();
            String sCpuThermalFilePath = (TextUtils.isEmpty(sTemp)) ? mEditTxtThermalPath.getHint().toString() : sTemp;

            mDirection = mRdoBtnDL.isChecked() ? DeviceMonitoringService.SHARED_PREFERENCES_DL_DIRECTION : DeviceMonitoringService.SHARED_PREFERENCES_UL_DIRECTION;
            mThermalType = DeviceMonitoringService.THERMAL_VTS;
            try {
                ConfigurationFragment.this.mDeviceLoggingService.fireUpForcedRecordingLoop(sPackageName, sInterval, sCpuClockFilePath, sCpuThermalFilePath, mDirection, mThermalType, 0.1f );
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }

            refreshMonitoringButtons();
        }
    };

    View.OnClickListener mStopForceRecordingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                ConfigurationFragment.this.mDeviceLoggingService.finishForcedRecordingLoop();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    IDeviceMonitoringServiceCallback.Stub mCallback = new IDeviceMonitoringServiceCallback.Stub() {

        @Override
        public void onMonitoringStarted() throws RemoteException {
            Log.d(TAG, "onMonitoringStarted()");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConfigurationFragment.this.mTxtViewResult.setText(ConfigurationFragment.this.mTxtViewResult.getText().toString() + "\nMonitoring Started...\n");
                    refreshMonitoringButtons();
                }
            });
        }

        @Override
        public void onMonitoringStopped() throws RemoteException {
            Log.d(TAG, "onMonitoringStopped()");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConfigurationFragment.this.mTxtViewResult.setText(ConfigurationFragment.this.mTxtViewResult.getText().toString() + "\nMonitoring Stopped...\n");
                    refreshMonitoringButtons();
                }
            });
        }

        @Override
        public void onRecordingStarted() throws RemoteException {
            Log.d(TAG, "onRecordingStarted()");
            //ConfigurationActivity.this.mTxtViewResult.setText(ConfigurationActivity.this.mTxtViewResult.getText().toString() + "\nRecording Started...");
        }

        @Override
        public void onRecordingStopped(final float overallTput, long duration, long totalTxBytes, long totalRxBytes, final int callCount) throws RemoteException {
            Log.d(TAG, "onRecordingStopped()");
            Log.d(TAG, "calling refreshMonitoringButtons()");
            Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshMonitoringButtons();
                        ConfigurationFragment.this.mTxtViewResult.setText(ConfigurationFragment.this.mTxtViewResult.getText().toString() + "CallCount : " + callCount + "     TPut : " + overallTput + " Mbps\n\n");
                    }
                });
            }
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        PackageInfo sItem = (PackageInfo) adapterView.getItemAtPosition(i);
        Log.d(TAG, "Selected LGFTPFileItem: " + sItem.packageName);

        if (getString(R.string.str_package_name_manual_input).equals(sItem.packageName)) {
            mEditTxtPackageName.setText("");
            mEditTxtPackageName.setVisibility(View.VISIBLE);
            return;
        }

        mEditTxtPackageName.setText(sItem.packageName);
        mEditTxtPackageName.setVisibility(View.GONE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        for (int i = 0; i < 1000; i++) {
            Toast.makeText(adapterView.getContext(), "Monitoring할 Package를 골라주세요.", Toast.LENGTH_LONG).show();
        }
    }

    static private class UIValidationResult {
        enum UIException {
            NoError(0x00000000), PackageNameInvalid(0x00000001), IntervalValueInvalid(0x00000002), InstantaneousTputCalculationTimeLengthInvalid(0x00000004), CPUFreqPathInvalid(0x00000008), CPUThermalPathInvalid(0x00000010), TputThresholdValueInvalid(0x00000011);

            private int value;

            UIException(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        public int mExceptinoCode;

        public UIValidationResult() {
            this.mExceptinoCode = UIException.NoError.getValue();
        }

        public void addException(UIException exception) {
            this.mExceptinoCode = this.mExceptinoCode | exception.getValue();
        }

        public void removeException(UIException exception) {
            this.mExceptinoCode = this.mExceptinoCode & ~(exception.getValue());
        }

        public boolean isExceptionIncluded(UIException e) {
            return (this.mExceptinoCode & e.getValue()) != 0;
        }

        @Override
        public String toString() {
            return Integer.toBinaryString(this.mExceptinoCode);
        }
    }

    private UIValidationResult getUIValidationResult() {

        UIValidationResult e = new UIValidationResult();
        // 1. packageName check
        Log.d(TAG, getString(R.string.str_package_name_all_traffic));
        Log.d(TAG, this.mEditTxtPackageName.getText().toString());
        if (!getString(R.string.str_package_name_all_traffic).equals(this.mEditTxtPackageName.getText().toString())) {
            PackageManager sPm = this.getActivity().getPackageManager();
            try {
                Log.d(TAG, "Selected PackageName : " + this.mEditTxtPackageName.getText().toString());
                sPm.getPackageInfo(this.mEditTxtPackageName.getText().toString(), 0);
            } catch (PackageManager.NameNotFoundException e1) {
                Log.d(TAG, "adding exception invalid package name ");
                e.addException(UIValidationResult.UIException.PackageNameInvalid);
            }
        }

        // 2. interval time check
        int sSamplingInterval = 0;
        try {
            sSamplingInterval = ("".equals(this.mEditTxtInterval.getText().toString())) ? Integer.valueOf(this.mEditTxtInterval.getHint().toString()) : Integer.valueOf(this.mEditTxtInterval.getText().toString());
            Log.d(TAG, "interval : " + sSamplingInterval);
            if (sSamplingInterval < 100 || sSamplingInterval > 5000) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            Log.d(TAG, "adding exception invalid interval time ");
            e.addException(UIValidationResult.UIException.IntervalValueInvalid);
        }

        // 3. InstantaneousTput calculation time length check
        try {
            int sInstantaneousTputCalculationTimeLength = ("".equals(this.mEditTxtInstantaneousTputCalculationDurationTime.getText().toString())) ? Integer.valueOf(this.mEditTxtInstantaneousTputCalculationDurationTime.getHint().toString()) : Integer.valueOf(this.mEditTxtInstantaneousTputCalculationDurationTime.getText().toString());
            if (sInstantaneousTputCalculationTimeLength < 0 || sInstantaneousTputCalculationTimeLength < sSamplingInterval) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            Log.d(TAG, "adding exception invalid threshold time ");
            e.addException(UIValidationResult.UIException.InstantaneousTputCalculationTimeLengthInvalid);
        }

        try {
            float tputThreshlod = (TextUtils.isEmpty(this.mEditTxtTputThresholdValue.getText())) ? Float.valueOf(this.mEditTxtTputThresholdValue.getHint().toString()) : Float.valueOf(this.mEditTxtTputThresholdValue.getText().toString());
            if (tputThreshlod < 0.1f) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            e.addException(UIValidationResult.UIException.TputThresholdValueInvalid);
        }

        // 4. cpu freq path check
//        if (!isFreqPathVaild((!"".equals(this.mEditTxtCPUClockPath.getText().toString())) ? this.mEditTxtCPUClockPath.getText().toString(): (this.mEditTxtCPUClockPath.getHint().toString()))) {
//            Log.d(TAG, "adding exception CPU freq path ");
//            e.addException(UIValidationResult.UIException.CPUFreqPathInvalid);
//        }

        // 5. cpu thermal path check

        return e;
    }

    private void initUIControls() {
        this.mUIControlList = new ArrayList<>();

        this.mImgBtnRecordingController = (ImageButton) this.mView.findViewById(R.id.imgBtn_recording);
        this.mImgBtnRecordingController.setOnClickListener(this.mStartForcedRecordingClickListener);

        this.mBtnLoggingController = (Button) this.mView.findViewById(R.id.btn_start_service);
        this.mBtnLoggingController.requestFocus();

        this.mEditTxtPackageName = (EditText) this.mView.findViewById(R.id.editTxt_package_name);
        this.mUIControlList.add(this.mEditTxtPackageName);

        this.mEditTxtInterval = (EditText) this.mView.findViewById(R.id.editTxt_interval);
        this.mUIControlList.add(this.mEditTxtInterval);

        this.mRdoBtnChipsetVendorDefault = (RadioButton) this.mView.findViewById(R.id.radioButton_chipset_default);
        this.mRdoBtnChipsetVendorManual = (RadioButton) this.mView.findViewById(R.id.radioButton_chipset_manual);
        this.mUIControlList.add(this.mRdoBtnChipsetVendorDefault);

        this.mEditTxtCPUClockPath = (EditText) this.mView.findViewById(R.id.editText_cpu_path);
        this.mEditTxtCPUClockPath.setOnFocusChangeListener(this);
        this.mUIControlList.add(this.mEditTxtCPUClockPath);

        this.mRdoBtnThermalVts = (RadioButton) this.mView.findViewById(R.id.radioButton_thermal_vts);
        this.mRdoBtnThermalVts.setOnCheckedChangeListener(this);
        this.mUIControlList.add(this.mRdoBtnThermalVts);

        this.mRdoBtnThermalManual = (RadioButton) this.mView.findViewById(R.id.radioButton_thermal_manual_input);
        this.mRdoBtnThermalManual.setOnCheckedChangeListener(this);
        this.mUIControlList.add(this.mRdoBtnThermalManual);

        this.mEditTxtThermalPath = (EditText) this.mView.findViewById(R.id.editText_thermal_path);
        Log.d(TAG, "get text : " + this.mEditTxtThermalPath.getText());
        //this.mEditTxtThermalPath.setText(CPUStatsReader.getVtsFilePath());
        this.mUIControlList.add(mEditTxtThermalPath);

        this.mEditTxtInstantaneousTputCalculationDurationTime = (EditText) this.mView.findViewById(R.id.editText_time_threshold_value);
        this.mUIControlList.add(mEditTxtInstantaneousTputCalculationDurationTime);

        this.mEditTxtTputThresholdValue = (EditText) this.mView.findViewById(R.id.editText_tput_threshold_value);
        this.mUIControlList.add(mEditTxtTputThresholdValue);

        this.mSpinnerPackageNames = (Spinner) this.mView.findViewById(R.id.spinner_package_name);
        this.mSpinnerPackageNames.setAdapter(new PackageNameSpinnerAdapter(this.getContext()));
        this.mSpinnerPackageNames.setOnItemSelectedListener(this);
        this.mSpinnerPackageNames.setEnabled(false);
        this.mUIControlList.add(mSpinnerPackageNames);

        this.mEditTxtPackageName.setVisibility(View.GONE);
        this.mEditTxtPackageName.setOnFocusChangeListener(this);
        this.mUIControlList.add(mEditTxtPackageName);

        this.mRdoBtnDL = (RadioButton) this.mView.findViewById(R.id.radioButton_dl_direction);
        this.mRdoBtnUL = (RadioButton) this.mView.findViewById(R.id.radioButton_ul_direction);
        this.mUIControlList.add(mRdoBtnDL);

        // listener setup
        this.mRdoBtnChipsetVendorDefault.setOnCheckedChangeListener(this);
        this.mUIControlList.add(mRdoBtnChipsetVendorDefault);

        this.mRdoBtnChipsetVendorManual.setOnCheckedChangeListener(this);
        this.mUIControlList.add(mRdoBtnChipsetVendorManual);

        this.mRdoBtnChipsetVendorDefault.setChecked(true);

        this.mRdoBtnDL.setOnCheckedChangeListener(this);
        this.mRdoBtnUL.setOnCheckedChangeListener(this);
        this.mRdoBtnDL.setChecked(true);
        this.mUIControlList.add(mRdoBtnDL);
        this.mUIControlList.add(mRdoBtnUL);

        this.mRdoBtnThermalVts.setOnCheckedChangeListener(this);
        this.mUIControlList.add(mRdoBtnThermalVts);

        this.mRdoBtnThermalVts.setChecked(true);

        this.mTxtViewResult = (TextView) this.mView.findViewById(R.id.txtView_result_summary);
        this.mTxtViewResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        this.mTxtViewResult.setText("Result Summary\n");
        this.mUIControlList.add(mTxtViewResult);

        this.refreshMonitoringButtons();
    }

    private void refreshMonitoringButtons() {
        Log.d(TAG, "refreshMonitoringButtons()");
        if (this.mDeviceLoggingService == null) {
            Log.d(TAG, "DeviceLoggingService is null");
            this.mBtnLoggingController.setEnabled(false);
            this.mImgBtnRecordingController.setEnabled(false);
            return;
        }

        Log.d(TAG, "getting device state... ");
        try {
            if (this.mDeviceLoggingService.isInMonitoringState()) {
                Log.d(TAG, "DeviceMonitoringService is in device monitoring state");
                // need to set the btn property to stop monitoring set.
                this.mBtnLoggingController.setText("모니터링 종료"); //set the text
                this.mBtnLoggingController.setOnClickListener(this.mStopMonitoringOnClickListener);
                for (View view : mUIControlList) {
                    view.setEnabled(false);
                }
                this.mImgBtnRecordingController.setEnabled(false);

            } else if (this.mDeviceLoggingService.isInForcibleRecordingState()) {
                Log.d(TAG, "DeviceMonitoringService is in forcibleRecordingState");
                // if it's in forced recording state, disable all other ui controls and monitoring start stop button
                this.mImgBtnRecordingController.setImageDrawable(this.getResources().getDrawable(R.drawable.record_stop));
                this.mImgBtnRecordingController.setOnClickListener(this.mStopForceRecordingClickListener);
                for (View view : mUIControlList) {
                    view.setEnabled(false);
                }
                this.mBtnLoggingController.setEnabled(false);
            } else {
                // otherwise,
                Log.d(TAG, "neither monitoring nor forced recording");
                this.mBtnLoggingController.setText("모니터링 시작");
                this.mBtnLoggingController.setEnabled(true);
                this.mBtnLoggingController.setOnClickListener(this.mStartMonitoringOnClickListener);

                this.mImgBtnRecordingController.setEnabled(true);
                this.mImgBtnRecordingController.setImageDrawable(this.getResources().getDrawable(R.drawable.record_start));
                this.mImgBtnRecordingController.setOnClickListener(this.mStartForcedRecordingClickListener);

                for (View view : mUIControlList) {
                    view.setEnabled(true);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        this.mSpinnerPackageNames.setFocusable(false);
    }
}
