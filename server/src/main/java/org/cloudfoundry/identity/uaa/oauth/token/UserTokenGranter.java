/*
 * *****************************************************************************
 *      Cloud Foundry
 *      Copyright (c) [2009-2016] Pivotal Software, Inc. All Rights Reserved.
 *      This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *      You may not use this product except in compliance with the License.
 *
 *      This product includes a number of subcomponents with
 *      separate copyright notices and license terms. Your use of these
 *      subcomponents is subject to the terms and conditions of the
 *      subcomponent's license, as noted in the LICENSE file.
 * *****************************************************************************
 */

package org.cloudfoundry.identity.uaa.oauth.token;

import org.cloudfoundry.identity.uaa.oauth.UaaOauth2Authentication;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import static org.cloudfoundry.identity.uaa.oauth.token.TokenConstants.USER_TOKEN_REQUESTING_CLIENT_ID;

public class UserTokenGranter  extends AbstractTokenGranter {

    public UserTokenGranter(AuthorizationServerTokenServices tokenServices,
                            ClientDetailsService clientDetailsService,
                            OAuth2RequestFactory requestFactory) {
        super(tokenServices, clientDetailsService, requestFactory, TokenConstants.GRANT_TYPE_USER_TOKEN);
    }

    protected void validateRequest(TokenRequest request) {
        //things to validate
        //1. Authentication must exist and be authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication instanceof UaaOauth2Authentication)) {
            throw new InsufficientAuthenticationException("Invalid authentication object:"+authentication);
        }
        UaaOauth2Authentication oauth2Authentication = (UaaOauth2Authentication)authentication;
        //2. authentication must be a user, and authenticated
        if (oauth2Authentication.getUserAuthentication() == null || !oauth2Authentication.getUserAuthentication().isAuthenticated()) {
            throw new InsufficientAuthenticationException("Authentication containing a user is required:"+authentication);
        }
        //3. parameter requesting_client_id must be present
        if (request.getRequestParameters()==null || request.getRequestParameters().get(USER_TOKEN_REQUESTING_CLIENT_ID)==null) {
            throw new InvalidGrantException("Parameter "+USER_TOKEN_REQUESTING_CLIENT_ID+" is required.");
        }
        //4. grant_type must be user_token
        if (!TokenConstants.GRANT_TYPE_USER_TOKEN.equals(request.getGrantType())) {
            throw new InvalidGrantException("Invalid grant type");
        }

    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        validateRequest(tokenRequest);
        OAuth2Request storedOAuth2Request = getRequestFactory().createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, null);
    }
}