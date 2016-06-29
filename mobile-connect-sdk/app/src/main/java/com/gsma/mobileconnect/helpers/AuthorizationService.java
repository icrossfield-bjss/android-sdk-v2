/*
 *                                   SOFTWARE USE PERMISSION
 *
 *  By downloading and accessing this software and associated documentation files ("Software") you are granted the
 *  unrestricted right to deal in the Software, including, without limitation the right to use, copy, modify, publish,
 *  sublicense and grant such rights to third parties, subject to the following conditions:
 *
 *  The following copyright notice and this permission notice shall be included in all copies, modifications or
 *  substantial portions of this Software: Copyright © 2016 GSM Association.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. YOU
 *  AGREE TO INDEMNIFY AND HOLD HARMLESS THE AUTHORS AND COPYRIGHT HOLDERS FROM AND AGAINST ANY SUCH LIABILITY.
 */
package com.gsma.mobileconnect.helpers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.fasterxml.jackson.databind.JsonNode;
import com.gsma.android.mobileconnect.R;
import com.gsma.mobileconnect.discovery.DiscoveryResponse;
import com.gsma.mobileconnect.impl.AndroidOIDCImpl;
import com.gsma.mobileconnect.model.DiscoveryModel;
import com.gsma.mobileconnect.oidc.DiscoveryResponseExpiredException;
import com.gsma.mobileconnect.oidc.IOIDC;
import com.gsma.mobileconnect.oidc.OIDCException;
import com.gsma.mobileconnect.oidc.ParsedAuthorizationResponse;
import com.gsma.mobileconnect.oidc.ParsedIdToken;
import com.gsma.mobileconnect.oidc.RequestTokenResponse;
import com.gsma.mobileconnect.oidc.RequestTokenResponseData;
import com.gsma.mobileconnect.oidc.TokenOptions;
import com.gsma.mobileconnect.utils.AndroidJsonUtils;
import com.gsma.mobileconnect.utils.AndroidRestClient;
import com.gsma.mobileconnect.utils.ErrorResponse;
import com.gsma.mobileconnect.utils.NoFieldException;
import com.gsma.mobileconnect.utils.StringUtils;
import com.gsma.mobileconnect.view.DiscoveryAuthenticationDialog;
import com.gsma.mobileconnect.view.InteractableWebView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Class to wrap the Authorization related calls to the Mobile Connect SDK.
 */
public class AuthorizationService extends BaseService
{
    private static final String TAG = AuthorizationService.class.getName();

    private IOIDC oidc;

    private static final String MOBILE_CONNECT_SESSION_LOCK = "gsma:mc:session_lock";

    private static final String MOBILE_CONNECT_SESSION_KEY = "gsma:mc:session_key";

    private static final Object LOCK_OBJECT = new Object();

    private static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

    private static final String SET_COOKIE_HEADER = "set-cookie";

    private static final int HTTP_OK = 200;

    private static final int HTTP_ACCEPTED = 202;

    private static final String INTERNAL_ERROR_CODE = "internal error";

    public AuthorizationService()
    {
        AndroidRestClient client = new AndroidRestClient();
        oidc = new AndroidOIDCImpl(client);
    }

    public AuthorizationService(IOIDC oidc)
    {
        this.oidc = oidc;
    }

    /**
     * This method is called via the redirect from the operator authorization page.
     * <p>
     * The values encoded in the URL are used to obtain an authorization token from the operator.
     *
     * @param config            The config to be used.
     * @param discoveryResponse The discoveryResponse Object
     * @return A status object that is either an error, start discovery or complete.
     */
    public MobileConnectStatus callMobileConnectOnAuthorizationRedirect(MobileConnectConfig config,
                                                                        DiscoveryResponse discoveryResponse)
    {
        if (discoveryResponse == null)
        {
            return MobileConnectStatus.startDiscovery();
        }

        try
        {
            String url = DiscoveryModel.getInstance().getDiscoveryServiceRedirectedURL();
            CaptureParsedAuthorizationResponse captureParsedAuthorizationResponse = new
                    CaptureParsedAuthorizationResponse();
            oidc.parseAuthenticationResponse(url, captureParsedAuthorizationResponse);
            ParsedAuthorizationResponse parsedAuthorizationResponse = captureParsedAuthorizationResponse
                    .getParsedAuthorizationResponse();

            if (!StringUtils.isNullOrEmpty(parsedAuthorizationResponse.get_error()))
            {
                return MobileConnectStatus.error(parsedAuthorizationResponse.get_error(),
                                                 parsedAuthorizationResponse.get_error_description(),
                                                 parsedAuthorizationResponse,
                                                 null);
            }

            if (!hasMatchingState(parsedAuthorizationResponse.get_state(), config.getAuthorizationState()))
            {
                return MobileConnectStatus.error("Invalid authentication response",
                                                 "State values do not match",
                                                 parsedAuthorizationResponse,
                                                 null);
            }

            TokenOptions tokenOptions = config.getTokenOptions();
            CaptureRequestTokenResponse captureRequestTokenResponse = new CaptureRequestTokenResponse();

            oidc.requestToken(discoveryResponse,
                              config.getApplicationURL(),
                              parsedAuthorizationResponse.get_code(),
                              tokenOptions,
                              captureRequestTokenResponse);

            RequestTokenResponse requestTokenResponse = captureRequestTokenResponse.getRequestTokenResponse();

            if (!isSuccessResponseCode(requestTokenResponse.getResponseCode()))
            {
                ErrorResponse errorResponse = getErrorResponse(requestTokenResponse);
                return MobileConnectStatus.error(errorResponse.get_error(),
                                                 errorResponse.get_error_description(),
                                                 parsedAuthorizationResponse,
                                                 requestTokenResponse);
            }

            ErrorResponse errorResponse = requestTokenResponse.getErrorResponse();
            if (null != errorResponse)
            {
                return MobileConnectStatus.error(errorResponse.get_error(),
                                                 errorResponse.get_error_description(),
                                                 parsedAuthorizationResponse,
                                                 requestTokenResponse);
            }

            return MobileConnectStatus.complete(parsedAuthorizationResponse, requestTokenResponse);
        }
        catch (OIDCException ex)
        {
            ex.printStackTrace();
            return MobileConnectStatus.error("Failed to obtain a token.",
                                             "Failed to obtain an authentication token from the operator.",
                                             ex);
        }
        catch (DiscoveryResponseExpiredException ex)
        {
            return MobileConnectStatus.startDiscovery();
        }
    }

    /**
     * Extract an error response from a request token response, create a generic error if the request token response
     * does not
     * contain an error response.
     *
     * @param requestTokenResponse The request token response to query
     * @return The extracted error or a generic error
     */
    ErrorResponse getErrorResponse(RequestTokenResponse requestTokenResponse)
    {
        ErrorResponse errorResponse = requestTokenResponse.getErrorResponse();
        if (null == errorResponse)
        {
            errorResponse = new ErrorResponse();
            errorResponse.set_error(INTERNAL_ERROR_CODE);
            errorResponse.set_error_description("End point failed.");
        }
        return errorResponse;
    }

    /**
     * Test whether the state values in the Authorization request and the Authorization response match.
     * <p>
     * States match if both are null or the values equal each other.
     *
     * @param responseState The state contained in the response.
     * @param requestState  The state contained in the request.
     * @return True if the states match, false otherwise.
     */
    boolean hasMatchingState(String responseState, String requestState)
    {
        if (StringUtils.isNullOrEmpty(requestState) && StringUtils.isNullOrEmpty(responseState))
        {
            return true;
        }
        else if (StringUtils.isNullOrEmpty(requestState) || StringUtils.isNullOrEmpty(responseState))
        {
            return false;
        }
        else
        {
            return requestState.equals(responseState);
        }
    }

    /**
     * Handles the process between the MNO and the end user for the end user to
     * sign in/ authorize the application. The application hands over to the
     * browser during the authorization step. On completion the MNO redirects to
     * the application sending the completion information as URL parameters.
     *
     * @param config      the mobile config
     * @param authUri     the URI to the MNO's authorization page
     * @param scopes      which is an application specified string value.
     * @param redirectUri which is the return point after the user has
     *                    authenticated/consented.
     * @param state       which is application specified.
     * @param nonce       which is application specified.
     * @param maxAge      which is an integer value.
     * @param acrValues   which is an application specified.
     * @param activity    The parent Activity
     * @param listener    The listener used to alert the activity to the change
     * @param response    The information captured in the discovery phase.
     * @throws UnsupportedEncodingException
     */
    public void authorize(final MobileConnectConfig config,
                          String authUri,
                          String scopes,
                          final String redirectUri,
                          String state,
                          String nonce,
                          int maxAge,
                          String acrValues,
                          Activity activity,
                          final AuthorizationListener listener,
                          final DiscoveryResponse response) throws UnsupportedEncodingException
    {

        authorize(config,
                  authUri,
                  scopes,
                  redirectUri,
                  state,
                  nonce,
                  maxAge,
                  acrValues,
                  activity,
                  listener,
                  response,
                  null);
    }

    /**
     * Handles the process between the MNO and the end user for the end user to
     * sign in/ authorize the application. The application hands over to the
     * browser during the authorization step. On completion the MNO redirects to
     * the application sending the completion information as URL parameters.
     *
     * @param config           the mobile config
     * @param authUri          the URI to the MNO's authorization page
     * @param scopes           which is an application specified string value.
     * @param redirectUri      which is the return point after the user has
     *                         authenticated/consented.
     * @param state            which is application specified.
     * @param nonce            which is application specified.
     * @param maxAge           which is an integer value.
     * @param acrValues        which is an application specified.
     * @param context          The Android context
     * @param listener         The listener used to alert the activity to the change
     * @param response         The information captured in the discovery phase.
     * @param hmapExtraOptions A HashMap containing additional authorization options
     * @throws UnsupportedEncodingException
     */
    public void authorize(final MobileConnectConfig config,
                          String authUri,
                          String scopes,
                          final String redirectUri,
                          String state,
                          String nonce,
                          int maxAge,
                          String acrValues,
                          Context context,
                          final AuthorizationListener listener,
                          final DiscoveryResponse response,
                          HashMap<String, Object> hmapExtraOptions) throws UnsupportedEncodingException
    {
        JsonNode discoveryResponseWrapper = response.getResponseData();
        JsonNode discoveryResponseJsonNode = discoveryResponseWrapper.get("response");

        String clientId = null;
        String clientSecret = null;

        try
        {
            clientId = AndroidJsonUtils.getExpectedStringValue(discoveryResponseJsonNode, "client_id");
        }
        catch (NoFieldException e)
        {
            e.printStackTrace();
        }
        Log.d(TAG, "clientId = " + clientId);

        try
        {
            clientSecret = AndroidJsonUtils.getExpectedStringValue(discoveryResponseJsonNode, "client_secret");
        }
        catch (NoFieldException e)
        {
            e.printStackTrace();
        }
        Log.d(TAG, "clientSecret = " + clientSecret);

        try
        {
            Log.d(TAG, "clientSecret = " + clientId);
            Log.d(TAG, "authUri = " + authUri);
            Log.d(TAG, "responseType = code");
            Log.d(TAG, "clientId = " + clientSecret);
            Log.d(TAG, "scopes = " + scopes);
            Log.d(TAG, "returnUri = " + redirectUri);
            Log.d(TAG, "state = " + state);
            Log.d(TAG, "nonce = " + nonce);
            Log.d(TAG, "maxAge = " + maxAge);
            Log.d(TAG, "acrValues = " + acrValues);

            if (authUri == null)
            {
                authUri = "";
            }
            String requestUri = authUri;
            if (authUri.indexOf("?") == -1)
            {
                requestUri += "?";
            }
            else if (authUri.indexOf("&") == -1)
            {
                requestUri += "&";
            }
            String charSet = Charset.defaultCharset().name();
            requestUri += "response_type=" + URLEncoder.encode("code", charSet);
            requestUri += "&client_id=" + URLEncoder.encode(clientId, charSet);
            requestUri += "&scope=" + URLEncoder.encode(scopes, charSet);
            requestUri += "&redirect_uri=" + URLEncoder.encode(redirectUri, charSet);
            requestUri += "&state=" + URLEncoder.encode(state, charSet);
            requestUri += "&nonce=" + URLEncoder.encode(nonce, charSet);
            //  requestUri += "&prompt=" + URLEncoder.encode(prompt.value(), charSet);
            requestUri += "&max_age=" + URLEncoder.encode(Integer.toString(maxAge), charSet);
            requestUri += "&acr_values=" + URLEncoder.encode(acrValues);

            if (hmapExtraOptions != null && hmapExtraOptions.size() > 0)
            {
                for (String key : hmapExtraOptions.keySet())
                {
                    requestUri += "&" + key + "=" + URLEncoder.encode(hmapExtraOptions.get(key).toString(), charSet);
                }
            }

            RelativeLayout webViewLayout = (RelativeLayout) LayoutInflater.from(context)
                                                                          .inflate(R.layout.layout_web_view, null);

            final InteractableWebView webView = (InteractableWebView) webViewLayout.findViewById(R.id.web_view);
            final ProgressBar progressBar = (ProgressBar) webViewLayout.findViewById(R.id.progressBar);

            final DiscoveryAuthenticationDialog dialog = DiscoveryAuthenticationDialog.getInstance(context);

            if (webView.getParent() != null)
            {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }

            dialog.setContentView(webView);

            webView.setWebChromeClient(new WebChromeClient()
            {
                @Override
                public void onCloseWindow(WebView w)
                {
                    super.onCloseWindow(w);
                    Log.d(TAG, "Window close");
                    w.setVisibility(View.INVISIBLE);
                    w.destroy();
                }

            });

            webView.setWebViewClient(new AuthorizationWebViewClient(dialog,
                                                                    progressBar,
                                                                    listener,
                                                                    redirectUri,
                                                                    config,
                                                                    response));

            webView.loadUrl(requestUri);
            dialog.show();
        }
        catch (NullPointerException e)
        {
            Log.d(TAG, "NullPointerException=" + e.getMessage(), e);
        }
    }

    public class AuthorizationWebViewClient extends MobileConnectWebViewClient
    {
        AuthorizationListener listener;

        MobileConnectConfig config;

        DiscoveryResponse response;

        public AuthorizationWebViewClient(Dialog dialog,
                                          ProgressBar progressBar,
                                          AuthorizationListener listener,
                                          String redirectUri,
                                          MobileConnectConfig config,
                                          DiscoveryResponse response)
        {
            super(dialog, progressBar, redirectUri);
            this.listener = listener;
            this.config = config;
            this.response = response;
        }

        @Override
        protected boolean qualifyUrl(String url)
        {
            return url.contains("code");
        }

        @Override
        protected void handleError(MobileConnectStatus status)
        {
            listener.authorizationFailed(status);
        }

        @Override
        protected void handleResult(String url)
        {
            dialog.cancel();

            ParameterList parameters = ParameterList.getKeyValuesFromUrl(url, 0);

            String state = parameters.getValue("state");
            String code = parameters.getValue("code");
            String error = parameters.getValue("error");

            Log.d(TAG, "state = " + state);
            Log.d(TAG, "code = " + code);
            Log.d(TAG, "error = " + error);
            Log.d(TAG, "Redirect URL " + url);
            DiscoveryModel.getInstance().setDiscoveryServiceRedirectedURL(url);
            config.setAuthorizationState(state);

            AuthorizationService connect = new AuthorizationService();
            MobileConnectStatus mobileConnectStatus = connect.callMobileConnectOnAuthorizationRedirect(config,
                                                                                                       response);
            notifyListener(mobileConnectStatus, listener);
        }
    }

    protected void notifyListener(MobileConnectStatus mobileConnectStatus, AuthorizationListener listener)
    {
        if (mobileConnectStatus == null)
        {
            listener.authorizationFailed(MobileConnectStatus.startDiscovery());
        }
        else if (mobileConnectStatus.isError())
        {
            // An error occurred, the error, description and (optionally) exception is available.
            Log.d(TAG, "Authorization has failed");
            Log.d(TAG, mobileConnectStatus.getError());
            Log.d(TAG, mobileConnectStatus.getDescription());

            listener.authorizationFailed(mobileConnectStatus);
        }
        else if (mobileConnectStatus.isStartDiscovery())
        {
            // The operator could not be identified, start the discovery process.
            Log.d(TAG, "The operator could not be identified, need to restart the discovery process.");
            listener.authorizationFailed(mobileConnectStatus);
        }
        else if (mobileConnectStatus.isComplete())
        {
            // Successfully authenticated, ParsedAuthenticationResponse and RequestTokenResponse are available
            try
            {
                RequestTokenResponse response = mobileConnectStatus.getRequestTokenResponse();
                RequestTokenResponseData responseData = response.getResponseData();
                ParsedIdToken parsedIdToken = responseData.getParsedIdToken();
                String token = parsedIdToken.get_pcr();
                Log.d(TAG, "Authorization has completed successfully");
                Log.d(TAG, "PCR is " + token);

                listener.tokenReceived(mobileConnectStatus.getRequestTokenResponse());
            }
            catch (Exception e)
            {
                //This shouldn't happen so we will catch as exception to ensure something would return
                Log.d(TAG, "Part of the Auth response was incorrect", e);
                listener.authorizationFailed(MobileConnectStatus.startDiscovery());
            }
        }
        else
        {
            Log.d(TAG,
                  "The status is in an unknown state (not Complete, Error or Start Discovery - please restart " +
                  "discovery");
            listener.authorizationFailed(MobileConnectStatus.startDiscovery());
        }
    }

    /**
     * Helper function developed to parse the redirect sent by the authorization
     * server to the application and extract code, state and error values.
     *
     * @param returnUri which is the return point after the user has
     *                  authenticated/consented.
     * @return ParameterList
     */
    public ParameterList extractRedirectParameter(String returnUri)
    {
        ParameterList parameters = null;

        if (returnUri != null && returnUri.trim().length() > 0)
        {
            parameters = ParameterList.getKeyValuesFromUrl(returnUri, 0);
        }

        return parameters;
    }
}
