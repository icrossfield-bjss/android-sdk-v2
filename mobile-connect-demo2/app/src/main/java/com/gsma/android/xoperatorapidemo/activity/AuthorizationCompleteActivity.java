package com.gsma.android.xoperatorapidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gsma.android.R;
import com.gsma.mobileconnect.helpers.AuthorizationService;
import com.gsma.mobileconnect.helpers.RetrieveUserinfoTask;
import com.gsma.mobileconnect.helpers.UserInfo;
import com.gsma.mobileconnect.helpers.UserInfoListener;
import com.gsma.mobileconnect.oidc.RequestTokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

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

	TextView statusField = null;

	TextView authorizationCompleteEmailValue = null;
	TextView authorizationCompleteSubValue = null;

	boolean setEmail=false;

	private static final String NA ="not available";
	AuthorizationService service = new AuthorizationService();

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
	}

	/*
	 * when this activity starts
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

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



			//Log.d(TAG, "handling code="+code+" error="+error);
			
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

	public void tokenResponse(RequestTokenResponse response) {
		//Log.d(TAG, "received token response");
		String access_token=response.getResponseData().get_access_token();
		boolean haveAccessToken=false;
		if (access_token!=null && access_token.trim().length()>0) {
			statusField.setText("retrieved access token");
			haveAccessToken=true;
		} else {
			statusField.setText("access token not received");
		}
		String id_token = response.getResponseData().get_access_token();
		if (id_token!=null && id_token.trim().length()>0) {
			String[] id_token_parts=id_token.split("\\.");
			if (id_token_parts!=null && id_token_parts.length>=2) {
				String idValue=id_token_parts[1];
				byte[] decoded=Base64.decode(idValue, Base64.DEFAULT);
				String dec=new String(decoded);
				//Log.d(TAG, "decoded to "+dec);
				try {
					JSONObject json=new JSONObject(dec);
					String email=json.has("email")?json.getString("email"):null;
					if (email!=null && email.trim().length()>0) {
						authorizationCompleteEmailValue.setText(email);
						setEmail=true;
					}
					String sub=json.has("sub")?json.getString("sub"):null;
					if (sub!=null && sub.trim().length()>0) {
						authorizationCompleteSubValue.setText(sub);
					}
				} catch (JSONException e) {
				}
			}					
		}

	}


	public void errorToken(JSONObject error) {
		statusField.setText("error retrieving access token");
	}



	public void errorUserinfo(JSONObject error) {
		statusField.setText("error retrieving userinfo");
	}

	@Override
	public void userReceived(UserInfo userInfo) {
		Log.d(TAG, "userInfo" + userInfo);
	}
}
