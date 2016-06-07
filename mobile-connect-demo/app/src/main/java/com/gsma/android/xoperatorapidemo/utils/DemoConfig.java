package com.gsma.android.xoperatorapidemo.utils;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

import com.gsma.mobileconnect.helpers.DiscoveryService;
import com.gsma.mobileconnect.helpers.MobileConnectConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * simple utilities to help with retrieving application preferences
 */
public class DemoConfig {
	private static final String TAG = "DemoConfig";

	/*
	 * the loaded properties file
	 */


	/**
	 * load the preferences for the application - needs to have a reference to
	 * the activity initiating the load
	 * 
	 * @param activity
	 */
	public static MobileConnectConfig getMobileConfig(Activity activity) {

		/*
		 * use the AssetManager to load in the properties
		 */
		AssetManager assetManager = activity.getResources().getAssets();
		MobileConnectConfig config = null;

		try(InputStream inputStream = assetManager
                .open("demo.properties");) {
    		Properties properties = new Properties();
            properties.load(inputStream);
			config = getMobileConnectConfig(properties);

			Log.d(TAG, "The properties are now loaded");
			Log.d(TAG, "properties: " + properties);

		} catch (IOException e) {
			Log.e(TAG, "Failed to open mobileconnecttest property file");
			e.printStackTrace();
		}
		return config;

	}



	private static MobileConnectConfig getMobileConnectConfig(Properties properties) {

		MobileConnectConfig mobileConnectConfig = new MobileConnectConfig();

//		// Registered application client id
//		mobileConnectConfig.setClientId(properties.getProperty("config.clientId"));
//
//		// Registered application client secret
//		mobileConnectConfig.setClientSecret(properties.getProperty("config.clientSecret"));
//
		// Registered application url
		mobileConnectConfig.setApplicationURL(properties.getProperty("config.applicationURL"));

		// URL of the Mobile Connect Discovery End Point
		mobileConnectConfig.setDiscoveryURL(properties.getProperty("config.discoveryURL"));

		// URL to inform the Discovery End Point to redirect to, this should route to the "/discovery_redirect" handler below
		mobileConnectConfig.setDiscoveryRedirectURL(properties.getProperty("config.discoveryRedirectURL"));

		DiscoveryService service = new DiscoveryService();

		// Authorization State would typically set to a unique value
		mobileConnectConfig.setAuthorizationState(service.generateUniqueString("state_"));

		// Authorization Nonce would typically set to a unique value
		mobileConnectConfig.setAuthorizationNonce(service.generateUniqueString("nonce_"));


		return mobileConnectConfig;
	}
}
