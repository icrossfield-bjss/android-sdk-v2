package com.gsma.mobileconnect.helpers;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.net.UrlQuerySanitizer;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by Usmaan.Dad on 6/22/2016.
 */
public abstract class MobileConnectWebViewClient extends WebViewClient
{
    protected ProgressBar progressBar;

    protected Dialog dialog;

    protected String redirectUrl;

    public MobileConnectWebViewClient(Dialog dialog, ProgressBar progressBar, String redirectUrl)
    {
        this.progressBar = progressBar;
        this.dialog = dialog;
        this.redirectUrl = redirectUrl;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
    {
        this.handleError(getErrorStatus(failingUrl));
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
    {
        dialog.cancel();
        this.handleError(getErrorStatus(request.getUrl().toString()));
    }

    private MobileConnectStatus getErrorStatus(String url)
    {
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(url);
        String error = sanitizer.getValue("error");
        String errorDescription = sanitizer.getValue("error_description");

        return MobileConnectStatus.error(error, errorDescription, new Exception(errorDescription));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url)
    {
        Log.d(MobileConnectWebViewClient.class.getSimpleName(), "onPageStarted disco url=" + url);
        progressBar.setVisibility(View.VISIBLE);

        if (!url.startsWith(redirectUrl))
        {
            return false;
        }

        // Check for response errors in the URL
        if (url.contains("error"))
        {
            handleError(getErrorStatus(url));
        }

        /*
        * Check to see if the url contains the discovery token
        * identifier - it could be a url parameter or a page
        * fragment. The following checks and string manipulations
        * retrieve the actual discovery token
        */
        if (qualifyUrl(url))
        {
            handleResult(url);
        }

        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url)
    {
        super.onPageFinished(view, url);
        progressBar.setVisibility(View.GONE);
    }

    protected abstract boolean qualifyUrl(String url);

    protected abstract void handleError(MobileConnectStatus status);

    protected abstract void handleResult(String url);
}
