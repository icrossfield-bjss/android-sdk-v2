package com.gsma.android.xoperatorapidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gsma.android.R;
import com.gsma.mobileconnect.helpers.RetrieveUserinfoTask;
import com.gsma.mobileconnect.helpers.UserInfo;
import com.gsma.mobileconnect.helpers.UserInfoListener;

/*
 * initiate the process of sign-in using the OperatorID API. 
 * the sign-in process is based on the user accessing the operator portal
 * through a browser. It is based on OpenID Connect
 * 
 * details on using an external browser are not finalised therefore at the moment
 * this uses a WebView
 */
public class AuthorizationCompleteActivity extends Activity implements UserInfoListener {
	private static final String TAG = "AuthCompleteActivity";

	AuthorizationCompleteActivity authorizationCompleteActivityInstance; // saved copy of this instance -
	// needed when sending an intent
	
	String authUri = null;
	String tokenUri = null;
	String userinfoUri = null;
	String clientId = null;
	String clientSecret = null;
	String scopes = null;
	String returnUri = null;
	String state = null;
	String code = null;
	String error = null;
    String accessToken = null;
    String PCR = null;

	TextView statusField = null;

	TextView authorizationCompleteTokenValue = null;
	TextView authorizationCompletePCRValue = null;

	boolean setEmail=false;

	private static final String NA ="not available";
//	AuthorizationService service = new AuthorizationService();

	/*
	 * method called when this activity is created - handles the receiving of
	 * endpoint parameters and setting up the WebView
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		authorizationCompleteActivityInstance = this;
		setContentView(R.layout.activity_identity_authorization_complete);
		
		statusField = (TextView) findViewById(R.id.authorizationCompleteStatus);

		authorizationCompleteTokenValue = (TextView) findViewById(R.id.authorizationCompleteTokenValue);
		authorizationCompletePCRValue = (TextView) findViewById(R.id.authorizationCompletePCRValue);
	}

	/*
	 * when this activity starts
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

        authorizationCompleteTokenValue.setText(getString(R.string.authorizationCompleteTokenValue));
        authorizationCompletePCRValue.setText(getString(R.string.authorizationCompletePCRValue));

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			/*
			 * Extract the parameters from the bundle provided
			 */

			userinfoUri = extras.getString("userinfoUri");
			scopes = extras.getString("scopes");
			returnUri = extras.getString("returnUri");
			state = extras.getString("state");
			code = extras.getString("code");
			error = extras.getString("error");
			clientId = extras.getString("clientId");
			clientSecret = extras.getString("clientSecret");

            accessToken = extras.getString("accessToken");
            PCR = extras.getString("PCR");

            if (accessToken!=null) {
                authorizationCompleteTokenValue.setText(accessToken);
            }
            if (PCR!=null) {
                authorizationCompletePCRValue.setText(PCR);
            }

			String statusDescription="Unknown";
			boolean authorized=false;
			if (code!=null && code.trim().length()>0) {
				statusDescription="Authorized";
				authorized=true;
			} else if (error!=null && error.trim().length()>0) {
				statusDescription="Not authorized";
			} 
				
			statusField.setText(statusDescription);
			
			if (authorized) {
				//MobileConnectConfig config,String userinfoUri, String accessToken, UserInfoListener listener
				RetrieveUserinfoTask task = new RetrieveUserinfoTask(userinfoUri,code, clientId,clientSecret,this);
				task.execute();
			}
			
 		}
	}

	/*
	 * go back to the main screen
	 */
	public void home(View view) {
		Intent intent = new Intent(authorizationCompleteActivityInstance, MainActivity.class);
		startActivity(intent);
	}

	@Override
	public void userReceived(UserInfo userInfo) {
		Log.d(TAG, "userInfo" + userInfo);
	}
}
