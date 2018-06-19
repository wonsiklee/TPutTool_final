package com.lge.kobinfactory.lgiperf.iperf;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by hyukbin.ko on 2018-05-17.
 */

public class LGIperfUtils {
    private static final String TAG = LGIperfUtils.class.getSimpleName();
    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    private static Pattern VALID_DNS_PATTERN = null;
    private static Pattern VALID_RATE_PATTERN = null;
    private static final String IPV4PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final String IPV6PATTERN = "^([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}$";
    private static final String DNS_PATTERN = "^([a-zA-Z]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";
    //= "^([a-zA-Z](a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";
    //= "^((https?):\\/\\/)?([^:\\/\\s]+)(:([^\\/]*))?((\\/[^\\s/\\/]+)*)?\\/([^#\\s\\?]*)(\\?([^#\\s]*))?(#(\\w*))?$";
    private static final String RATE_PATTERN = "^[0-9a-f]+[KM]?$";
    static {
        try {
            VALID_IPV4_PATTERN = Pattern.compile(IPV4PATTERN, Pattern.CASE_INSENSITIVE);
            VALID_IPV6_PATTERN = Pattern.compile(IPV6PATTERN, Pattern.CASE_INSENSITIVE);
            VALID_DNS_PATTERN  = Pattern.compile(DNS_PATTERN, Pattern.CASE_INSENSITIVE);
            VALID_RATE_PATTERN  = Pattern.compile(RATE_PATTERN, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            Log.d(TAG,"not initialize pattern!");
        }
    }


    public static boolean isValidIpAddress(String ipAddress) {
        return isValidIp4Address(ipAddress) || isValidIp6Address(ipAddress);
    }

    public static boolean isValidIp4Address(String ipAddress) {
        return VALID_IPV4_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isValidIp6Address(String ipAddress) {
        return VALID_IPV6_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isValidDNS(String dnsName) {
        return VALID_DNS_PATTERN.matcher(dnsName).matches();
    }

    public static boolean isValidRate(String dnsName) {
        return VALID_RATE_PATTERN.matcher(dnsName).matches();
    }

    public static void setSharedPreferencesArrayList(final Context context, final String FILE_NAME, final String PREFERENCE_NAME, List<String> setPrefer) {
        SharedPreferences prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> set = new HashSet<String>();
        set.addAll(setPrefer);
        editor.putStringSet(PREFERENCE_NAME, set);
        editor.commit();
    }

    public static List<String> getSharedPreferencesArrayList(final Context context, final String FILE_NAME, final String PREFERENCE_NAME){
        SharedPreferences prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Set<String> set =  prefs.getStringSet(PREFERENCE_NAME,null);
        if(set==null) return null;
        List<String> list = new ArrayList<String>();
        list.addAll(set);
        return list;
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();

                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (isIPv4) {
                            return sAddr;
                        } else {
                            int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                            return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return null;
    }
}
