package com.gsma.android.xoperatorapidemo.utils;

import com.gsma.mobileconnect.helpers.DiscoveryService;
import com.gsma.mobileconnect.helpers.MobileConnectConfig;

/*
 * Customise the settings in this file in order to use your own application details
 */
public class AppSettings {


    /*
     * The following must be configured correctly for your application.
     * Use the Mobile Connect developer portal - https://developer.mobileconnect.io
     * to register your application, this will provide you with the clientID and clientSecret
     * for calling the Mobile Connect discovery API.
     *
     * You will also need to provide the applicationRedirectURL - this is a URL which is opened
     * during the authorization process and is used to pass control (and information) back to your
     * application.
     */
//    private static String discoveryURL="https://discover.mobileconnect.io/gsma/v2/discovery";
//    private static String discoveryClientID="CnlwTezNhkeXxkAq77OXrmEZEUoSjG63";
//    private static String discoveryClientSecret="DbpCO2fIK8ux01Qp";

    private static String discoveryURL="http://discovery.sandbox2.mobileconnect.io/v2/discovery";
    private static String discoveryClientID="bc25c661-3c6e-45ee-9870-a5c84574f3d2";
    private static String discoveryClientSecret="8511782b-7c28-4c79-950b-64f3e8a1a97a";

    private static String applicationRedirectURL="http://mc.mobilesites.net/walkthrough.html";

    /*
     * This URL is opened at the end of the discovery phase - it doesn't specifically need to be
     * changed, and it doesn't need to exist for the SDK to recognise discovery is complete.
     */
    private static String discoveryRedirectURL="http://localhost/discoveryComplete.html";

    public static String getDiscoveryClientID() {
        return discoveryClientID;
    }

    public static String getDiscoveryClientSecret() {
        return discoveryClientSecret;
    }

    public static String getApplicationRedirectURL() {
        return applicationRedirectURL;
    }

    public static String getDiscoveryURL() {
        return discoveryURL;
    }

    public static String getDiscoveryRedirectURL() {
        return discoveryRedirectURL;
    }

    public static MobileConnectConfig getMobileConnectConfig() {

        MobileConnectConfig mobileConnectConfig = new MobileConnectConfig();

        // Registered application client id
        mobileConnectConfig.setClientId(discoveryClientID);

        // Registered application client secret
        mobileConnectConfig.setClientSecret(discoveryClientSecret);

        // Registered application url
        mobileConnectConfig.setApplicationURL(applicationRedirectURL);

        // URL of the Mobile Connect Discovery End Point
        mobileConnectConfig.setDiscoveryURL(discoveryURL);

        // URL to inform the Discovery End Point to redirect to, this should route to the "/discovery_redirect" handler below
        mobileConnectConfig.setDiscoveryRedirectURL(discoveryRedirectURL);

        DiscoveryService service = new DiscoveryService();

        // Authorization State would typically set to a unique value
        mobileConnectConfig.setAuthorizationState(service.generateUniqueString("S_"));

        // Authorization Nonce would typically set to a unique value
        mobileConnectConfig.setAuthorizationNonce(service.generateUniqueString("N_"));

        return mobileConnectConfig;
    }

}
