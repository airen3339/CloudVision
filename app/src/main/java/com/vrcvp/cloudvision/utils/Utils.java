package com.vrcvp.cloudvision.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;

import com.vrcvp.cloudvision.Config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * 工具类
 * Created by yinglovezhuzhu@gmail.com on 2016/9/16.
 */
public class Utils {
    private Utils() {}

    /**
     * Measure a view.
     * @param child
     */
    public static void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * 获取全部meta-data数据
     * @param context Context对象
     * @return 存储所有meta-data数据的Bundle对象
     */
    public static Bundle getMetaDatas(Context context) {
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            if(null == applicationInfo || null == applicationInfo.metaData) {
                return null;
            }
            return applicationInfo.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取meta-data数据
     * @param context Context对象
     * @param key meta-data key
     * @return String类型的meta-data数据
     */
    public static String getStringMetaData(Context context, String key) {
        final Bundle bundle = getMetaDatas(context);
        if(null == bundle || !bundle.containsKey(key)) {
            return "";
        }
        return bundle.getString(key, "");
    }

    /**
     * 获取字符串的MD5编码
     * @param inputString
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String getMD5Hex(final String inputString) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(inputString.getBytes());

        byte[] digest = md.digest();

        return convertByteToHex(digest);
    }

    /**
     * 获取手机特征号(设备号)
     * @return 设备特征号，平板等没有的可能返回空
     */
    public static String getDeviceId(Context context) {
        String deviceId = "";
        try {
            TelephonyManager telephonyManager= (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                deviceId = telephonyManager.getDeviceId();
            } else {
                try {
                    deviceId = telephonyManager.getDeviceId(telephonyManager.getPhoneType());
                } catch (NoSuchMethodError e) {
                    deviceId = telephonyManager.getDeviceId();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null == deviceId ? "" : deviceId;
    }

    /**
     * 生成设备id
     * @param context Context对象
     * @return 设备唯一标识
     */
    public static String getClientId(Context context) {
        SharedPrefHelper sharedPrefHelper = SharedPrefHelper.newInstance(context, Config.SP_FILE_CONFIG);
        String clientId = sharedPrefHelper.getString(Config.SP_KEY_CLIENT_ID, "");
        if ("".equals(clientId)) {
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            try {
                String deviceId = getDeviceId(context);
                if (null != androidId && !"9774d56d682e549c".equals(androidId)
                        && null != deviceId && !"".equals(deviceId)
                        && !"000000000000000".equals(deviceId)) {
                    clientId = getMD5Hex(androidId + deviceId);
                } else if (null != androidId && !"9774d56d682e549c".equals(androidId)) {
                    clientId = getMD5Hex(androidId);
                } else if (null != deviceId && !"".equals(deviceId)
                        && !"000000000000000".equals(deviceId)) {
                    clientId = getMD5Hex(deviceId);
                } else {
                    clientId = getMD5Hex(context.getPackageName()
                            + new Random().nextLong() + System.currentTimeMillis());
                }
            } catch (NoSuchAlgorithmException e) {
                // 做最后的保障处理,基本不会到这里
                clientId = String.valueOf(System.currentTimeMillis());
            }
            sharedPrefHelper.saveString(Config.SP_KEY_CLIENT_ID, clientId);
        }
        return clientId;
    }

    /**
     * 获取MAC地址
     * @param context Context对象
     * @return MAC网卡地址，可能为空
     */
    public static String getMac(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(null == wifiManager) {
            return "";
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(null == wifiInfo) {
            return "";
        }
        return wifiInfo.getMacAddress();
    }

//    public static String getMac(Context context) {
//        return "AA:BB:CC:DD:EE";
//    }

    private static String convertByteToHex(byte[] byteData) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
}