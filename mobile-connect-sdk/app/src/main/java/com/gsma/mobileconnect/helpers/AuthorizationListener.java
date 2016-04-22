package com.gsma.mobileconnect.helpers;

import com.gsma.mobileconnect.oidc.RequestTokenResponse;

/**
 * This Listener handles the tokens being returned from the Authorization service.
 * Created by nick.copley on 23/02/2016.
 */
public interface AuthorizationListener {

    /**
     * A RequestTokenResponse has been received. Please note that this doesn't mean the token is valid
     * for example it could have expired.
     * @param response a token response
     */
    public void tokenReceived(RequestTokenResponse response);
}
