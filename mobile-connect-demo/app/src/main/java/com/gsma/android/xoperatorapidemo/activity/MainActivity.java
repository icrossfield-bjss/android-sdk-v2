package com.gsma.android.xoperatorapidemo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.android.R;
import com.gsma.android.xoperatorapidemo.discovery.DiscoveryStartupSettings;
import com.gsma.android.xoperatorapidemo.utils.DemoConfig;
import com.gsma.android.xoperatorapidemo.utils.PhoneState;
import com.gsma.android.xoperatorapidemo.utils.PhoneUtils;
import com.gsma.mobileconnect.discovery.DiscoveryResponse;
import com.gsma.mobileconnect.helpers.AuthorizationListener;
import com.gsma.mobileconnect.helpers.AuthorizationService;
import com.gsma.mobileconnect.helpers.DiscoveryService;
import com.gsma.mobileconnect.helpers.MobileConnectConfig;
import com.gsma.mobileconnect.helpers.MobileConnectStatus;
import com.gsma.mobileconnect.model.DiscoveryModel;
import com.gsma.mobileconnect.oidc.RequestTokenResponse;
import com.gsma.mobileconnect.utils.AndroidJsonUtils;
import com.gsma.mobileconnect.utils.JsonUtils;
import com.gsma.mobileconnect.utils.ParsedOperatorIdentifiedDiscoveryResult;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class MainActivity extends Activity implements AuthorizationListener, View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_CODE_PHONE_STATE = 9875576;

    public static MainActivity mainActivityInstance = null;
    static Handler discoveryHandler = null;
    static Handler phoneStatusHandler = null;
    private static boolean discoveryComplete = false;
    private static boolean connectionExists = true;
    private static MobileConnectStatus status;
    Button discoveryButton = null;
    TextView vMCC = null;
    TextView vMNC = null;
    TextView vStatus = null;
    TextView vDiscoveryStatus = null;
    Button startOperatorId = null;
    Button settingButton = null;
    RelativeLayout rlayout;
    DiscoveryService discoveryService = new DiscoveryService();
    AuthorizationService authorizationService = new AuthorizationService();
    MobileConnectConfig config;


    private BroadcastReceiver ConnectivityChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updatePhoneState();
        }

    };

    /**
     * Clear the current state.
     */
    public static void clearDiscoveryData() {
        Log.d(TAG, "Clearing discovery data");
        status = null;
    }

    /*
     * method called when the application first starts.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting the app...");

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        config = DemoConfig.getMobileConfig(this);


        vMCC = (TextView) findViewById(R.id.valueMCC);
        vMNC = (TextView) findViewById(R.id.valueMNC);
        vMCC.setText(getText(R.string.valueUnknown));
        vMNC.setText(getText(R.string.valueUnknown));
        vStatus = (TextView) findViewById(R.id.valueStatus);
        vDiscoveryStatus = (TextView) findViewById(R.id.valueDiscoveryStatus);

        discoveryButton = (Button) findViewById(R.id.discoveryButton);
        startOperatorId = (Button) findViewById(R.id.startOperatorId);
        settingButton = (Button) findViewById(R.id.settingsButton);

        if(!PhoneUtils.requestPermission(this, Manifest.permission.READ_PHONE_STATE, PERMISSIONS_CODE_PHONE_STATE)){
            return;
        }

        createAfterPermissionsGranted();
    }

    private void createAfterPermissionsGranted() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        this.registerReceiver(this.ConnectivityChangedReceiver, intentFilter);

		/*
         * load settings from private local storage
		 */
        SettingsActivity.loadSettings(this);

        rlayout = (RelativeLayout) findViewById(R.id.mainActivity);
        rlayout.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Toast noInternetConnection = Toast.makeText(getApplicationContext(), "No internet Connection", Toast.LENGTH_SHORT);
                noInternetConnection.show();
            }
        });

        CookieSyncManager.createInstance(this.getApplicationContext());
        CookieManager.getInstance().setAcceptCookie(true);

        discoveryHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "Discovery result processing. " + msg.what);
                status = (MobileConnectStatus) msg.obj;
                if (status != null) {
                    boolean discoveryComplete = !status.isStartDiscovery();
                    if (discoveryComplete) {
                        vMCC.setText(DiscoveryModel.getInstance().getMcc());
                        vMNC.setText(DiscoveryModel.getInstance().getMnc());
                        vDiscoveryStatus.setText(getString(R.string.discoveryStatusCompleted));
                        Log.d(TAG, "Discovery Complete");
                    } else {
                        vDiscoveryStatus.setText(getString(R.string.discoveryStatusFailer));
                        Log.d(TAG, getString(R.string.discoveryStatusFailer));
                    }
                } else {
                    Log.d(TAG, "Discovery failed - Status is empty");
                }
                setButtonStates(status);
            }
        };

        phoneStatusHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                vStatus.setText(getString(msg.what));
            }
        };

        updatePhoneState();
        vMCC.setText(DiscoveryModel.getInstance().getMcc());
        vMNC.setText(DiscoveryModel.getInstance().getMnc());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case PERMISSIONS_CODE_PHONE_STATE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createAfterPermissionsGranted();
                    initialiseDiscovery();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        Toast noInternetConnection = Toast.makeText(getApplicationContext(), "No internet Connection", Toast.LENGTH_SHORT);
        noInternetConnection.show();

    }

    /**
     * Update the phone state based on information gathered from the Android SDK.
     */
    public void updatePhoneState() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        PhoneState state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);

        boolean connected = state.isConnected(); // Is the device connected to
        // the Internet
        boolean usingMobileData = state.isUsingMobileData(); // Is the device
        // connected using cellular/mobile data
        boolean roaming = state.isRoaming(); // Is the device roaming

        int connectivityStatus = R.string.statusDisconnected;
        if (roaming) {
            connectivityStatus = R.string.statusRoaming;
        } else if (usingMobileData) {
            connectivityStatus = R.string.statusOnNet;
        } else if (connected) {
            connectivityStatus = R.string.statusOffNet;
        }

        if (!roaming && !usingMobileData && (wifi.getConnectionInfo().getNetworkId() == -1)) {
            //no wifi or roaming or mobile data
            rlayout.setClickable(true);
            discoveryButton.setEnabled(false);
            startOperatorId.setEnabled(false);
            settingButton.setEnabled(false);
            connectionExists = false;
        } else {
            //assume an internet connection is avilable
            rlayout.setClickable(false);
            setButtonStates(status);
            settingButton.setEnabled(true);
            connectionExists = true;
        }

        if (SettingsActivity.getServingOperator().isAutomatic()) {
            DiscoveryModel.getInstance().setMcc(state.getMcc());
            DiscoveryModel.getInstance().setMnc(state.getMnc());
        }

        phoneStatusHandler.sendEmptyMessage(connectivityStatus);
    }

    /*
     * on start or return to the main screen reset the screen so that discovery
     * can be started
     */
    @Override
    public void onStart() {
        super.onStart();

        initialiseDiscovery();
    }

    private void initialiseDiscovery() {
        if(!PhoneUtils.hasPermission(this, Manifest.permission.READ_PHONE_STATE)){
            return;
        }

        Log.d(TAG, "Checking for cached discovery response");
        vMCC.setText(DiscoveryModel.getInstance().getMcc() != null ? DiscoveryModel.getInstance().getMcc() : getText(R.string.valueUnknown));
        vMNC.setText(DiscoveryModel.getInstance().getMnc() != null ? DiscoveryModel.getInstance().getMnc() : getText(R.string.valueUnknown));

        if (connectionExists) {
            if (status == null) {
                vDiscoveryStatus.setText(getString(R.string.discoveryStatusUnknown));
                updatePhoneState();
                DiscoveryStartupSettings startupOption = SettingsActivity.getDiscoveryStartupSettings();
                if (startupOption != DiscoveryStartupSettings.STARTUP_OPTION_PREEMPTIVE) {
                    String mcc = SettingsActivity.getMcc();
                    String mnc = SettingsActivity.getMnc();

                    Log.d(TAG, mcc + " " + mnc + " " + SettingsActivity.getServingOperator().getMcc());
                    vMCC.setText(DiscoveryModel.getInstance().getMcc() != null ? DiscoveryModel.getInstance().getMcc() : getText(R.string.valueUnknown));
                    vMNC.setText(DiscoveryModel.getInstance().getMnc() != null ? DiscoveryModel.getInstance().getMnc() : getText(R.string.valueUnknown));

                    Log.d(TAG, "StartUp options - " + startupOption.toString());
                    if (startupOption == DiscoveryStartupSettings.STARTUP_OPTION_PASSIVE) {
                        Log.d(TAG, "Initiating passive discovery");

                        if (SettingsActivity.getServingOperator().isAutomatic()) {
                            runDiscovery();
                        } else {
                            vDiscoveryStatus.setText(getString(R.string.discoveryStatusStarted));
                        }

                    } else {
                        //manual discovery
                        vDiscoveryStatus.setText(getString(R.string.discoveryStatusPending));
                        if (SettingsActivity.getServingOperator().isAutomatic()) {
                            DiscoveryModel.getInstance().setMcc(SettingsActivity.getServingOperator().getMcc());
                            DiscoveryModel.getInstance().setMnc(SettingsActivity.getServingOperator().getMnc());
                        } else {
                            DiscoveryModel.getInstance().setMcc(null);
                            DiscoveryModel.getInstance().setMnc(null);
                        }
                        //Log.d(TAG,DiscoveryModel.getInstance().getMcc());
                        vMCC.setText(DiscoveryModel.getInstance().getMcc() != null ? DiscoveryModel.getInstance().getMcc() : getText(R.string.valueUnknown));
                        vMNC.setText(DiscoveryModel.getInstance().getMnc() != null ? DiscoveryModel.getInstance().getMnc() : getText(R.string.valueUnknown));

                    }
                } else {
                    //Force discovery
                    if (SettingsActivity.getServingOperator().isAutomatic()) {
                        runDiscovery();
                    } else {

                        config.setIdentifiedMCC(null);
                        config.setIdentifiedMNC(null);
                        status = discoveryService.callMobileConnectForStartDiscovery(config);
                        if (status.isOperatorSelection()) {
                            Log.d(TAG, "Operator Selection required");
                            discoveryService.doDiscoveryWithWebView(config, this, discoveryHandler, status.getUrl());
                        }
                    }
                }
            } else {
                vMCC.setText(DiscoveryModel.getInstance().getMcc());
                vMNC.setText(DiscoveryModel.getInstance().getMnc());
            }
            setButtonStates(status);
        }
    }

    /**
     * Call the Discovery SDK.
     */
    private void runDiscovery() {
        Log.d(TAG, "Run Discovery");
        updatePhoneState();
        vDiscoveryStatus.setText(getString(R.string.discoveryStatusStarted));

        config.setIdentifiedMCC(DiscoveryModel.getInstance().getMcc());
        config.setIdentifiedMNC(DiscoveryModel.getInstance().getMnc());
        status = discoveryService.callMobileConnectForStartDiscovery(config);
        if (status.isOperatorSelection()) {
            Log.d(TAG, "Operator Selection required");
            discoveryService.doDiscoveryWithWebView(config, this, discoveryHandler, status.getUrl());
        } else {
            Message msg = new Message();
            msg.what = R.string.discoveryStatusCompleted;
            msg.obj = status;
            discoveryHandler.sendMessage(msg);
        }

    }

    /*
     * default method to add a menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    /**
     * Update the button states.
     *
     * @param status
     */
    private void setButtonStates(MobileConnectStatus status) {
        Log.d(TAG, "Setting button states");

        if (status == null) {
            startOperatorId.setVisibility(View.INVISIBLE);
            discoveryButton.setEnabled(true);
        } else {
            startOperatorId.setVisibility(status.isStartAuthorization() ? View.VISIBLE : View.INVISIBLE);
            discoveryButton.setEnabled(false);
        }
    }

    /**
     * The Settings Activity
     *
     * @param view
     */
    public void startSettings(View view) {
        Intent intent = new Intent(
                this,
                SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Attempt to get the Auth URI based on either automatic or user provided information. This will lauch
     * a WebView to capture the information if required.
     *
     * @param view
     */
    public void handleDiscovery(View view) {

        updatePhoneState();
        vDiscoveryStatus.setText(getString(R.string.discoveryStatusStarted));
        DiscoveryStartupSettings startupOption = SettingsActivity.getDiscoveryStartupSettings();
        if (startupOption == DiscoveryStartupSettings.STARTUP_OPTION_MANUAL) {
            if (SettingsActivity.getServingOperator().isAutomatic()) {
                runDiscovery();
            } else if (!SettingsActivity.getServingOperator().isAutomatic()) {

                config.setIdentifiedMCC(null);
                config.setIdentifiedMNC(null);
                status = discoveryService.callMobileConnectForStartDiscovery(config);
                if (status.isOperatorSelection()) {
                    Log.d(TAG, "Operator Selection required");
                    discoveryService.doDiscoveryWithWebView(config, this, discoveryHandler, status.getUrl());
                }
            }
        } else {
            DiscoveryModel.getInstance().setMcc(SettingsActivity.getServingOperator().getMcc());
            DiscoveryModel.getInstance().setMnc(SettingsActivity.getServingOperator().getMnc());
            runDiscovery();
        }
        setButtonStates(status);
    }

    /**
     * Called from the Layout XML. This is the button click response that initiates Authorisation.
     *
     * @param view
     * @throws UnsupportedEncodingException
     */
    public void startOperatorId(View view) throws UnsupportedEncodingException {
        if (status != null) {

            DiscoveryResponse resp = status.getDiscoveryResponse();

            String openIDConnectScopes = "openid";

            //  String returnUri=getMobileConnectConfig().getAuthorizationRedirectURL();
            String returnUri = "https://localhost:8080";
            String clientId = config.getClientId();
            String clientSecret = config.getClientSecret();
            String state = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();
            int maxAge = 3600;
            String acrValues = "2";

            config.setDiscoveryRedirectURL(returnUri);
            config.setAuthorizationState(state);


            maxAge = 0;

            ParsedOperatorIdentifiedDiscoveryResult parsedOperatorIdentifiedDiscoveryResult = JsonUtils.parseOperatorIdentifiedDiscoveryResult(resp.getResponseData());
            if (parsedOperatorIdentifiedDiscoveryResult == null || parsedOperatorIdentifiedDiscoveryResult.getAuthorizationHref() == null) {
                String error;
                if (config.getIdentifiedMCC() != null) {
                    error = "Authorisation URI for MMC/MNC not known";
                } else {
                    error = "Authorisation failed because MMC/MNC not found";
                }
                Toast toast = Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT);
                toast.show();
            } else {
                String authorizationHref = parsedOperatorIdentifiedDiscoveryResult.getAuthorizationHref();

                Log.d(TAG, "Starting OpenIDConnect authorization");
                authorizationService.authorize(config, authorizationHref, clientId, clientSecret, openIDConnectScopes, returnUri, state, nonce,
                        maxAge, acrValues, this, this, resp);
            }

        }
    }

    /**
     * Process the Auth response. This will launch a new Activity to display the UserInfo
     *
     * @param state
     * @param authorizationCode
     * @param error
     * @param clientId
     * @param clientSecret
     * @param scopes
     * @param returnUri
     */
    public void authorizationCodeResponse(String state, String authorizationCode, String error, String clientId, String clientSecret, String scopes, String returnUri) {

        DiscoveryResponse resp = status.getDiscoveryResponse();

        ParsedOperatorIdentifiedDiscoveryResult parsedOperatorIdentifiedDiscoveryResult = AndroidJsonUtils.parseOperatorIdentifiedDiscoveryResult(resp.getResponseData());

        Intent intent = new Intent(
                this,
                AuthorizationCompleteActivity.class);
        intent.putExtra("state", state);
        intent.putExtra("code", authorizationCode);
        intent.putExtra("error", error);
        intent.putExtra("clientId", clientId);
        intent.putExtra("clientSecret", clientSecret);
        intent.putExtra("scopes", scopes);
        intent.putExtra("returnUri", returnUri);
        intent.putExtra("userinfoUri", parsedOperatorIdentifiedDiscoveryResult.getUserInfoHref());

        startActivity(intent);
    }

    @Override
    public void tokenReceived(RequestTokenResponse tokenResponse) {
        String state = config.getAuthorizationState();
        String clientId = config.getClientId();
        String clientSecret = config.getClientSecret();
        String openIDConnectScopes = config.getAuthorizationScope();
        String returnUri = config.getDiscoveryRedirectURL();

        String token;
        String error;
        if (tokenResponse.hasErrorResponse()) {
            token = null;
            error = tokenResponse.getErrorResponse().get_error();
        } else {
            token = tokenResponse.getResponseData().get_access_token();

            error = null;
        }

        authorizationCodeResponse(state, token, error, clientId, clientSecret, openIDConnectScopes, returnUri);
    }

    @Override
    public void authorizationFailed(MobileConnectStatus mobileConnectStatus) {
        Log.d(TAG, "AuthorizationFailed");
        Toast authorizationFailed = Toast.makeText(getApplicationContext(), "AuthorizationFailed", Toast.LENGTH_SHORT);
        authorizationFailed.show();
    }
}
