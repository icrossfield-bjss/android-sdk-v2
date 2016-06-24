package com.gsma.android.xoperatorapidemo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.gsma.android.R;
import com.gsma.android.xoperatorapidemo.utils.AppSettings;
import com.gsma.android.xoperatorapidemo.utils.PhoneState;
import com.gsma.android.xoperatorapidemo.utils.PhoneUtils;
import com.gsma.mobileconnect.discovery.DiscoveryResponse;
import com.gsma.mobileconnect.helpers.AuthorizationListener;
import com.gsma.mobileconnect.helpers.AuthorizationService;
import com.gsma.mobileconnect.helpers.DiscoveryService;
import com.gsma.mobileconnect.helpers.MobileConnectConfig;
import com.gsma.mobileconnect.helpers.MobileConnectStatus;
import com.gsma.mobileconnect.model.DiscoveryModel;
import com.gsma.mobileconnect.oidc.ParsedIdToken;
import com.gsma.mobileconnect.oidc.RequestTokenResponse;
import com.gsma.mobileconnect.utils.AndroidJsonUtils;
import com.gsma.mobileconnect.utils.JsonUtils;
import com.gsma.mobileconnect.utils.NoFieldException;
import com.gsma.mobileconnect.utils.ParsedOperatorIdentifiedDiscoveryResult;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;


public class MainActivity extends Activity implements AuthorizationListener, View.OnClickListener {

    private static final String TAG = "MainActivity";

    public static MainActivity mainActivityInstance = null;
    static Handler discoveryHandler = null;
    static Handler phoneStatusHandler = null;
    private static boolean discoveryComplete = false;
    private static boolean connectionExists = true;
    private static MobileConnectStatus status;
    TextView vMCC = null;
    TextView vMNC = null;
    TextView vStatus = null;
    TextView vDiscoveryStatus = null;
    Button startOperatorId = null;
    RelativeLayout rlayout;
    DiscoveryService discoveryService = null;
    AuthorizationService authorizationService = null;
    MobileConnectConfig config;


    private BroadcastReceiver ConnectivityChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updatePhoneState();
        }

    };

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

        config = AppSettings.getMobileConnectConfig();

        vMCC = (TextView) findViewById(R.id.valueMCC);
        vMNC = (TextView) findViewById(R.id.valueMNC);
        vMCC.setText(getText(R.string.valueUnknown));
        vMNC.setText(getText(R.string.valueUnknown));
        vStatus = (TextView) findViewById(R.id.valueStatus);
        vDiscoveryStatus = (TextView) findViewById(R.id.valueDiscoveryStatus);

        startOperatorId = (Button) findViewById(R.id.startOperatorId);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        this.registerReceiver(this.ConnectivityChangedReceiver, intentFilter);

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

                        runMobileConnectLogin();
                    } else {
                        vDiscoveryStatus.setText(getString(R.string.discoveryStatusFailer));
                        Log.d(TAG, getString(R.string.discoveryStatusFailer));
                    }
                } else {
                    Log.d(TAG, "Discovery failed - Status is empty");
                }
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
            startOperatorId.setEnabled(false);
            connectionExists = false;
        } else {
            //assume an internet connection is avilable
            rlayout.setClickable(false);
            startOperatorId.setEnabled(true);
            connectionExists = true;
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

        Log.d(TAG, "called onStart");

        vMCC.setText(getText(R.string.valueUnknown));
        vMNC.setText(getText(R.string.valueUnknown));
        vDiscoveryStatus.setText(getString(R.string.discoveryStatusPending));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "called onResume");
    }

    /**
     * Call the Discovery SDK.
     */
    private void runDiscovery() {
        Log.d(TAG, "Run Discovery");
        updatePhoneState();

        if (connectionExists) {
            vDiscoveryStatus.setText(getString(R.string.discoveryStatusStarted));

            discoveryService=new DiscoveryService();

            status = discoveryService.callMobileConnectForStartDiscovery(config);

            Log.d(TAG, "Making initial discovery request");
            Log.d(TAG, "Initial response="+status.toString());
            if (status.getDiscoveryResponse()!=null && status.getDiscoveryResponse().getResponseData()!=null) {
                Log.d(TAG, "Response = "+status.getDiscoveryResponse().getResponseData().toString());
            }
            if (status.isError()) {
                String error="Discovery error";
                Toast toast = Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT);
                toast.show();
            } else if (status.isOperatorSelection()) {
                Log.d(TAG, "Operator Selection required");
                discoveryService.doDiscoveryWithWebView(config, this, discoveryHandler, status.getUrl());
            } else {
                Message msg = new Message();
                msg.what = R.string.discoveryStatusCompleted;
                msg.obj = status;
                discoveryHandler.sendMessage(msg);
            }
        } else {
            String error="Device is not currently connected to the Internet";
            Toast toast = Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public void runMobileConnectLogin() {

        Log.d(TAG, "Run Mobile Connect Login. Status = "+status);
        if (status != null) {

            DiscoveryResponse resp = status.getDiscoveryResponse();
            JsonNode discoveryResponseWrapper = resp.getResponseData();
            JsonNode discoveryResponse = discoveryResponseWrapper.get("response");

            ParsedOperatorIdentifiedDiscoveryResult parsedOperatorIdentifiedDiscoveryResult = JsonUtils.parseOperatorIdentifiedDiscoveryResult(resp.getResponseData());

            String authorizationHref = parsedOperatorIdentifiedDiscoveryResult.getAuthorizationHref();
            String tokenHref = parsedOperatorIdentifiedDiscoveryResult.getTokenHref();

            Log.d(TAG, "authorizationHref="+authorizationHref);
            Log.d(TAG, "tokenHref="+tokenHref);

            String encryptedMSISDN = DiscoveryModel.getInstance().getEncryptedMSISDN();
            HashMap<String, Object> authOptions = new HashMap<String, Object>();
            if (encryptedMSISDN != null) {
                String hint = "ENCR_MSISDN:" + encryptedMSISDN;
                Log.d(TAG, "Setting login_hint to " + hint);
                authOptions.put("login_hint", hint);
            }

            try {
                Log.d(TAG, "getting client_id from discovery response " + discoveryResponse.toString());
                String clientId = AndroidJsonUtils.getExpectedStringValue(discoveryResponse, "client_id");
                Log.d(TAG, "clientId = " + clientId);

                String clientSecret = AndroidJsonUtils.getExpectedStringValue(discoveryResponse, "client_secret");

                String openIDConnectScopes = "openid";

                String returnUri = config.getApplicationURL();
                String state = UUID.randomUUID().toString();
                String nonce = UUID.randomUUID().toString();
                int maxAge = 3600;
                String acrValues = "2";

                config.setDiscoveryRedirectURL(returnUri);
                config.setAuthorizationState(state);

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

                    authorizationService = new AuthorizationService();

                    Log.d(TAG, "Starting OpenIDConnect authorization");
                    authorizationService.authorize(config, authorizationHref, clientId, clientSecret, openIDConnectScopes, returnUri, state, nonce,
                            maxAge, acrValues, this, this, resp, authOptions);
                }
            } catch (NoFieldException nfe) {
                Log.e(TAG, "NoFieldException handling");
            } catch (UnsupportedEncodingException ueo) {
                Log.e(TAG, "UnsupportedEncodingException handling");
            }
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
     * Called from the Layout XML. This is the button click response that initiates Authorisation.
     *
     * @param view
     * @throws UnsupportedEncodingException
     */
    public void startOperatorId(View view) throws UnsupportedEncodingException {
        runDiscovery();
    }

    public void displayAuthorizationResponse(String state, String authorizationCode, String error, String clientId, String clientSecret, String scopes, String returnUri,
                                             String accessToken, String PCR) {

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
        intent.putExtra("accessToken", accessToken);
        intent.putExtra("PCR", PCR);
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

        String accessToken;
        String error;
        String pcr=null;
        if (tokenResponse.hasErrorResponse()) {
            accessToken = null;
            error = tokenResponse.getErrorResponse().get_error();
        } else {
            accessToken = tokenResponse.getResponseData().get_access_token();
            ParsedIdToken idtoken = tokenResponse.getResponseData().getParsedIdToken();
            if (idtoken!=null) {
                pcr = idtoken.get_pcr();
            }

            Log.d(TAG, "access_token="+accessToken);
            Log.d(TAG, "pcr="+pcr);
            error = null;
        }

        displayAuthorizationResponse(state, accessToken, error, clientId, clientSecret, openIDConnectScopes, returnUri, accessToken, pcr);
    }

    @Override
    public void authorizationFailed(MobileConnectStatus mobileConnectStatus) {
        Log.d(TAG, "AuthorizationFailed");
        Toast authorizationFailed = Toast.makeText(getApplicationContext(), "Authorization Failed : "+mobileConnectStatus.getError(), Toast.LENGTH_SHORT);
        authorizationFailed.show();
    }
}
