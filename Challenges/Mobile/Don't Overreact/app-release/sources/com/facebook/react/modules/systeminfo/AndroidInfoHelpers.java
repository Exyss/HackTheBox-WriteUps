package com.facebook.react.modules.systeminfo;

import android.content.Context;
import android.os.Build;
import com.facebook.react.R;
import java.util.Locale;

public class AndroidInfoHelpers {
    public static final String DEVICE_LOCALHOST = "localhost";
    public static final String EMULATOR_LOCALHOST = "10.0.2.2";
    public static final String GENYMOTION_LOCALHOST = "10.0.3.2";
    public static final String METRO_HOST_PROP_NAME = "metro.host";
    private static final String TAG = "AndroidInfoHelpers";
    private static String metroHostPropValue;

    private static boolean isRunningOnGenymotion() {
        return Build.FINGERPRINT.contains("vbox");
    }

    private static boolean isRunningOnStockEmulator() {
        return Build.FINGERPRINT.contains("generic");
    }

    public static String getServerHost(Integer num) {
        return getServerIpAddress(num.intValue());
    }

    public static String getServerHost(Context context) {
        return getServerIpAddress(getDevServerPort(context).intValue());
    }

    public static String getAdbReverseTcpCommand(Integer num) {
        return "adb reverse tcp:" + num + " tcp:" + num;
    }

    public static String getAdbReverseTcpCommand(Context context) {
        return getAdbReverseTcpCommand(getDevServerPort(context));
    }

    public static String getInspectorProxyHost(Context context) {
        return getServerIpAddress(getInspectorProxyPort(context).intValue());
    }

    public static String getFriendlyDeviceName() {
        if (isRunningOnGenymotion()) {
            return Build.MODEL;
        }
        return Build.MODEL + " - " + Build.VERSION.RELEASE + " - API " + Build.VERSION.SDK_INT;
    }

    private static Integer getDevServerPort(Context context) {
        return Integer.valueOf(context.getResources().getInteger(R.integer.react_native_dev_server_port));
    }

    private static Integer getInspectorProxyPort(Context context) {
        return Integer.valueOf(context.getResources().getInteger(R.integer.react_native_dev_server_port));
    }

    private static String getServerIpAddress(int i) {
        String metroHostPropValue2 = getMetroHostPropValue();
        if (metroHostPropValue2.equals("")) {
            if (isRunningOnGenymotion()) {
                metroHostPropValue2 = GENYMOTION_LOCALHOST;
            } else {
                metroHostPropValue2 = isRunningOnStockEmulator() ? EMULATOR_LOCALHOST : DEVICE_LOCALHOST;
            }
        }
        return String.format(Locale.US, "%s:%d", metroHostPropValue2, Integer.valueOf(i));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x003f, code lost:
        if (r2 == null) goto L_0x006f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0041, code lost:
        r2.destroy();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x006c, code lost:
        if (r2 == null) goto L_0x006f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x006f, code lost:
        r1 = com.facebook.react.modules.systeminfo.AndroidInfoHelpers.metroHostPropValue;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0072, code lost:
        return r1;
     */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0067 A[SYNTHETIC, Splitter:B:35:0x0067] */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0076 A[SYNTHETIC, Splitter:B:44:0x0076] */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x007d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static synchronized java.lang.String getMetroHostPropValue() {
        /*
        // Method dump skipped, instructions count: 132
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.react.modules.systeminfo.AndroidInfoHelpers.getMetroHostPropValue():java.lang.String");
    }
}
