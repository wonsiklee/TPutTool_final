package com.android.LGSetupWizard.clients;

/**
 * Created by hyukbin.ko on 2018-05-18.
 */

public class LGIperfConstants {

    public static final String IPERF_NAME = "iperf";
    public static final String IPERF3_NAME = "iperf3";

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

    public static String toStringRateUnit(int rateUnit){
        if(rateUnit ==IPERF_RATEUNIT_MBPS) return STRING_IPERF_RATEUNIT_MBPS;
        if(rateUnit ==IPERF_RATEUNIT_KBPS) return STRING_IPERF_RATEUNIT_KBPS;
        return "";
    }

}
