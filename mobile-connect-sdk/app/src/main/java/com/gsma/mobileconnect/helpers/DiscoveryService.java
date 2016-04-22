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
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gsma.android.mobileconnect.R;
import com.gsma.mobileconnect.discovery.CompleteSelectedOperatorDiscoveryOptions;
import com.gsma.mobileconnect.discovery.DiscoveryException;
import com.gsma.mobileconnect.discovery.DiscoveryOptions;
import com.gsma.mobileconnect.discovery.DiscoveryResponse;
import com.gsma.mobileconnect.discovery.IDiscovery;
import com.gsma.mobileconnect.discovery.ParsedDiscoveryRedirect;
import com.gsma.mobileconnect.impl.AndroidDiscoveryImpl;
import com.gsma.mobileconnect.model.DiscoveryModel;
import com.gsma.mobileconnect.utils.AndroidRestClient;
import com.gsma.mobileconnect.utils.ErrorResponse;
import com.gsma.mobileconnect.utils.RestClient;
import com.gsma.mobileconnect.utils.StringUtils;

import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Class to wrap the Discovery related calls to the Mobile Connect SDK.
 */
public class DiscoveryService extends BaseService
{
    private static final String TAG = DiscoveryService.class.getName();

    private IDiscovery discovery;

    public DiscoveryService() {
        RestClient client = new AndroidRestClient();
        discovery = new AndroidDiscoveryImpl(null, client);
    }

    private static final String MOBILE_CONNECT_SESSION_LOCK = "gsma:mc:session_lock";
    private static final String MOBILE_CONNECT_SESSION_KEY = "gsma:mc:session_key";
    private static final Object LOCK_OBJECT = new Object();

    private static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";
    private static final String SET_COOKIE_HEADER = "set-cookie";


    private static final String INTERNAL_ERROR_CODE = "internal error";


    /**
     * This method is called to initiate the Mobile Connect process.
     *
     * The return is either an 'error', 'operator selection is required' or 'authorization can start' (the operator has been identified).
     *
     * @param config Mobile Connect Configuration instance
     * @return A status object
     */
    public MobileConnectStatus callMobileConnectForStartDiscovery(MobileConnectConfig config)
    {

        DiscoveryResponse discoveryResponse;
        try
        {
            DiscoveryOptions options = config.getDiscoveryOptions("Mobile");

            CaptureDiscoveryResponse captureDiscoveryResponse = new CaptureDiscoveryResponse();
            discovery.startAutomatedOperatorDiscovery(config, config.getDiscoveryRedirectURL(),
                    options, null, captureDiscoveryResponse);
            discoveryResponse = captureDiscoveryResponse.getDiscoveryResponse();
            CompleteSelectedOperatorDiscoveryOptions optionsSelected = config.getCompleteSelectedOperatorDiscoveryOptions();
     }
        catch(DiscoveryException ex)
        {
            return MobileConnectStatus.error(INTERNAL_ERROR_CODE, "Failed to obtain operator details.", ex);
        }

        if(!discoveryResponse.isCached())
        {
            if(!isSuccessResponseCode(discoveryResponse.getResponseCode()))
            {
                ErrorResponse errorResponse = getErrorResponse(discoveryResponse);
                return MobileConnectStatus.error(errorResponse.get_error(), errorResponse.get_error_description(), discoveryResponse);
            }
        }

        // The DiscoveryResponse may contain the operator endpoints in which case we can proceed to authorization with an operator.
        String operatorSelectionURL = discovery.extractOperatorSelectionURL(discoveryResponse);
        if(!StringUtils.isNullOrEmpty(operatorSelectionURL))
        {
            return MobileConnectStatus.operatorSelection(operatorSelectionURL);
        }
        else
        {
             return MobileConnectStatus.startAuthorization(discoveryResponse);
        }
    }

    /**
     * This method is called to extract the response from the operator selection process and then determine what to do next.
     * <p>
     *
     * @param config Mobile Connect Configuration instance
     * @return A status object
     */
    public MobileConnectStatus callMobileConnectOnDiscoveryRedirect(MobileConnectConfig config)
    {
        CaptureParsedDiscoveryRedirect captureParsedDiscoveryRedirect = new CaptureParsedDiscoveryRedirect();
        try
        {
            String url = DiscoveryModel.getInstance().getDiscoveryServiceRedirectedURL();
            discovery.parseDiscoveryRedirect(url, captureParsedDiscoveryRedirect);
        }
        catch (URISyntaxException ex)
        {
            return MobileConnectStatus.error(INTERNAL_ERROR_CODE, "Cannot parse the redirect parameters.", ex);
        }

        ParsedDiscoveryRedirect parsedDiscoveryRedirect = captureParsedDiscoveryRedirect.getParsedDiscoveryRedirect();
        if(parsedDiscoveryRedirect == null || !parsedDiscoveryRedirect.hasMCCAndMNC())
        {
            // The operator has not been identified, need to start again.
            return MobileConnectStatus.startDiscovery();
        }

        DiscoveryResponse discoveryResponse;
        try
        {
            CompleteSelectedOperatorDiscoveryOptions options = config.getCompleteSelectedOperatorDiscoveryOptions();
            CaptureDiscoveryResponse captureDiscoveryResponse = new CaptureDiscoveryResponse();

            // Obtain the discovery information for the selected operator
            discovery.completeSelectedOperatorDiscovery(config, config.getDiscoveryRedirectURL(),
                    parsedDiscoveryRedirect.getSelectedMCC(),
                    parsedDiscoveryRedirect.getSelectedMNC(),
                    options, null, captureDiscoveryResponse);
            discoveryResponse = captureDiscoveryResponse.getDiscoveryResponse();

            //move to models
            DiscoveryModel.getInstance().setMcc(parsedDiscoveryRedirect.getSelectedMCC());
            DiscoveryModel.getInstance().setMnc(parsedDiscoveryRedirect.getSelectedMNC());
        }
        catch (DiscoveryException ex)
        {
            return MobileConnectStatus.error(INTERNAL_ERROR_CODE, "Failed to obtain operator details.", ex);
        }

        if(!discoveryResponse.isCached())
        {
            if(!isSuccessResponseCode(discoveryResponse.getResponseCode()))
            {
                ErrorResponse errorResponse = getErrorResponse(discoveryResponse);
                return MobileConnectStatus.error(errorResponse.get_error(), errorResponse.get_error_description(), discoveryResponse);
            }
        }

        if(discovery.isOperatorSelectionRequired(discoveryResponse))
        {
            return MobileConnectStatus.startDiscovery();
        }

        return MobileConnectStatus.startAuthorization(discoveryResponse);
    }

    /**
     * Create an Android WebView to display the MNO Discovery page. The Webview then captures the redirect on success containing
     * the MMC and MNC values. The handler is then called with these values.
     * @param config Mobile Connect Configuration instance
     * @param activity The parent Activity
     * @param discoveryHandler The Android Handler
     * @param operatorUrl The MNO discovery URI.
     */
    public void doDiscoveryWithWebView(final MobileConnectConfig config, Activity activity, final Handler discoveryHandler, final String operatorUrl) {

        ViewGroup.LayoutParams fillParent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        LinearLayoutCompat webViewLayout = (LinearLayoutCompat)activity.getLayoutInflater().inflate(R.layout.discovery_web_view, null);
        WebView view = (WebView)webViewLayout.findViewById(R.id.discoveryWebView);
        activity.addContentView(webViewLayout, fillParent);

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView w) {
                super.onCloseWindow(w);
                Log.d(TAG, "Window close");
                w.setVisibility(View.INVISIBLE);
                w.destroy();
            }

        });
        view.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Log.d(TAG, "onReceivedError errorCode=" + errorCode
                        + " description=" + description + " failingUrl="
                        + failingUrl);

                view.setVisibility(View.INVISIBLE);
                view.destroy();
          }

            /*
             * The onPageStarted method is called whenever the WebView
             * starts to load a new page - by examining the url for a
             * discovery token we can extract this and move to the next
             * stage of the process
             *
             * @see
             * android.webkit.WebViewClient#onPageStarted(android.webkit
             * .WebView, java.lang.String, android.graphics.Bitmap)
             */
            @Override
            public void onPageStarted(WebView view, String url,
                                      Bitmap favicon) {
                Log.d(TAG, "onPageStarted disco url=" + url);
							/*
							 * Check to see if the url contains the discovery token
							 * identifier - it could be a url parameter or a page
							 * fragment. The following checks and string manipulations
							 * retrieve the actual discovery token
							 */

                if (url != null && url.contains("mcc_mnc=")) {

                    DiscoveryModel.getInstance().setDiscoveryServiceRedirectedURL(url);
                    view.stopLoading();
                    view.setVisibility(View.INVISIBLE);
                    view.destroy();
                    if(DiscoveryModel.getInstance().getDiscoveryServiceRedirectedURL() != null) {
                        MobileConnectStatus status =  callMobileConnectOnDiscoveryRedirect(config);
                        Message message = new Message();
                        message.obj = status;
                        discoveryHandler.dispatchMessage(message);
                    }

                }
            }

        });

					/*
					 * enable JavaScript - the discovery web pages are enhanced with
					 * JavaScript
					 */
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        String databasePath = activity.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setDatabasePath(databasePath);

        HashMap<String, String> extraheaders = new HashMap<String, String>();
        view.loadUrl(operatorUrl, extraheaders);
        view.requestFocus(View.FOCUS_DOWN);
    }

    /**
     * Extract an error response from a discovery response, create a generic error if the discovery response does not
     * contain an error response.
     *
     * @param discoveryResponse The discovery response to check.
     * @return The extracted error response, or a generic error.
     */
    ErrorResponse getErrorResponse(DiscoveryResponse discoveryResponse)
    {
        ErrorResponse errorResponse = discovery.getErrorResponse(discoveryResponse);
        if(null == errorResponse)
        {
            errorResponse = new ErrorResponse();
            errorResponse.set_error(INTERNAL_ERROR_CODE);
            errorResponse.set_error_description("End point failed.");
        }
        return errorResponse;
    }

    public void setDiscovery(IDiscovery discovery) {
        this.discovery = discovery;
    }

}
