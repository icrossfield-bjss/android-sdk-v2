package com.gsma.android.xoperatorapidemo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.telephony.TelephonyManager;

import com.gsma.android.R;

/**
 * Created by Michael.Holmes on 22/02/2016.
 */
public class DeviceManager {
    private static DeviceManager ourInstance = new DeviceManager();

    public static DeviceManager getInstance() {
        return ourInstance;
    }

    private DeviceManager() {
    }

    public void updatePhoneState(Context context, Handler phoneStatusHandler){
        TelephonyManager telephonyManager=(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        PhoneState state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);

        boolean connected = state.isConnected(); // Is the device connected to
        // the Internet
        boolean usingMobileData = state.isUsingMobileData(); // Is the device
        // connected using cellular/mobile data
        boolean roaming = state.isRoaming(); // Is the device roaming

        int status = R.string.statusDisconnected;
        if (roaming) {
            status = R.string.statusRoaming;
        } else if (usingMobileData) {
            status = R.string.statusOnNet;
        } else if (connected) {
            status = R.string.statusOffNet;
        }
        phoneStatusHandler.sendEmptyMessage(status);
    }
}
