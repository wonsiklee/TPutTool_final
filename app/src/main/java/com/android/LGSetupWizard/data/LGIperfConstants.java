package com.android.LGSetupWizard.data;

/**
 * Created by hyukbin.ko on 2018-05-18.
 */

public class LGIperfConstants {

    public static final String IPERF_NAME = "iperf";
    public static final String IPERF3_NAME = "iperf3";

    public static final String KEY_IPERF_VERSION = "iperf_version";
    public static final int IPERF_VERSION2 = 0;
    public static final int IPERF_VERSION3 = 1;

    public static final int IPERF_NOT_SET = -1;

    public static final int IPERF_MODE_SERVER = 0;
    public static final int IPERF_MODE_CLIENT = 1;

    public static final int IPERF_RATEUNIT_BPS = 0;
    public static final int IPERF_RATEUNIT_KBPS = 1;
    public static final int IPERF_RATEUNIT_MBPS = 2;

    public static final String STRING_IPERF_RATEUNIT_KBPS = "K";
    public static final String STRING_IPERF_RATEUNIT_MBPS = "M";

    public static final String KEY_IPERF_COMMAND = "iperf_command";

    public static final String ACTION_RESULT_INSTALL_IPERF = "com.lge.kobinfactory.lgiperf.action_result_install_iperf";
    public static final String EXTRA_RESULT_INSTALL_IPERF = "result";

    public static final String ACTION_SAVE_AVG = "com.lge.kobinfactory.lgiperf.action_save_avg";
    public static final String EXTRA_SAVE_AVG = "result";

    public static final String ACTION_RESULT_COMMAND = "com.lge.kobinfactory.lgiperf.action_result_command";
    public static final String EXTRA_RESULT_COMMAND = "result";

    public static final String ACTION_STOP_IPERF = "com.lge.kobinfactory.lgiperf.action_stop_iperf";


    public class Message{
        public static final int REQUEST_RUN_COMMAND = 0x00;
        public static final int REQUEST_STOP_COMMAND = 0x01;
        public static final int REQUEST_INSTALL_IPERF = 0x10;
        public static final int RESULT_INSTALL_IPERF = 0x11;

        public static final int RESULT_COMMAND = 0x20;
        public static final int SAVE_AVG = 0x30;
        public static final int STOP_IPERF = 0x40;
    }

    public static String toMessageString(int meesage_code){
        if(meesage_code == Message.REQUEST_RUN_COMMAND) return "REQUEST_RUN_COMMAND";
        if(meesage_code == Message.REQUEST_STOP_COMMAND) return "REQUEST_STOP_COMMAND";
        if(meesage_code == Message.REQUEST_INSTALL_IPERF) return "REQUEST_INSTALL_IPERF";
        if(meesage_code == Message.RESULT_INSTALL_IPERF) return "RESULT_INSTALL_IPERF";
        if(meesage_code == Message.RESULT_COMMAND) return "RESULT_COMMAND";
        if(meesage_code == Message.SAVE_AVG) return "SAVE_AVG";
        if(meesage_code == Message.STOP_IPERF) return "STOP_IPERF";

        return "not unexpected command";
    }


    public static String toIperfVersionString(int iperf_version_code){
        if(iperf_version_code == IPERF_VERSION2) return "IPERF_VERSION2";
        if(iperf_version_code == IPERF_VERSION3) return "IPERF_VERSION3";
        return null;
    }

    public static String toStringRateUnit(int rateUnit){
        if(rateUnit ==IPERF_RATEUNIT_MBPS) return STRING_IPERF_RATEUNIT_MBPS;
        if(rateUnit ==IPERF_RATEUNIT_KBPS) return STRING_IPERF_RATEUNIT_KBPS;
        return "";
    }

}
