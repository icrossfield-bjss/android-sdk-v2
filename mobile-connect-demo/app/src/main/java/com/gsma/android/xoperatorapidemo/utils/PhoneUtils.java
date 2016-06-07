package com.gsma.android.xoperatorapidemo.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

/**
 * extract useful phone state information and return in the form of a PhoneState
 * object
 */
public class PhoneUtils {
	private static final String TAG = "PhoneUtils";

	/**
	 * convert information which can be obtained from the Android OS into
	 * PhoneState information necessary for discovery
	 * 
	 * @param telephonyMgr
	 * @param connectivityMgr
	 * @return
	 */
	public static PhoneState getPhoneState(TelephonyManager telephonyMgr,
			ConnectivityManager connectivityMgr) {

		/*
		 * the users' phone number is obtained - this is not always available/
		 * valid
		 */
		String msisdn = telephonyMgr.getLine1Number();

		/*
		 * get the active network
		 */
		NetworkInfo activeNetwork = connectivityMgr.getActiveNetworkInfo();

		/*
		 * check if the device is currently connected
		 */
        boolean connected = activeNetwork != null
                ? activeNetwork.isConnected()
                : false;

		/*
		 * check if the device is currently roaming
		 */
        boolean roaming = activeNetwork != null
                ? activeNetwork.isRoaming()
                : false;

		/*
		 * check if the device is using mobile/cellular data
		 */
        boolean usingMobileData = activeNetwork != null ? activeNetwork
                .getType() == ConnectivityManager.TYPE_MOBILE : false;



		
		/*
		 * get the SIM serial number
		 */
		String simSerialNumber = telephonyMgr.getSimSerialNumber();

		/*
		 * the simOperator indicates the registered network MCC/MNC the
		 * networkOperator indicates the current network MCC/MNC
		 */
		String simOperator = telephonyMgr.getSimOperator();
		/*
		 * Mobile Country Code is obtained from the first three digits of
		 * simOperator, Mobile Network Code is any remaining digits
		 */
		String mcc = null;
		String mnc = null;
		if (simOperator != null && simOperator.length() > 3) {
            if(Integer.parseInt(simOperator) > 0){
                mcc = simOperator.substring(0, 3);
                mnc = simOperator.substring(3);
            }

		}

		/*
		 * return a new PhoneState object from the parameters used in discovery
		 */
		return new PhoneState(msisdn, simOperator, mcc, mnc, connected,
				usingMobileData, roaming, simSerialNumber);

	}

	public static boolean hasPermission(Activity activity, String permission) {
		return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean requestPermission(Activity activity, String permission, int requestPermissionId) {
		boolean hasPermission = hasPermission(activity, permission);
		if(!hasPermission) {
			ActivityCompat.requestPermissions(activity, new String[]{ permission }, requestPermissionId);
		}

		return hasPermission;
	}
}
