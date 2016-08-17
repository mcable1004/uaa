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

import org.cloudfoundry.identity.uaa.authentication.UaaAuthentication;
import org.cloudfoundry.identity.uaa.oauth.UaaOauth2Authentication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.HashMap;
import java.util.Map;

import static org.cloudfoundry.identity.uaa.oauth.token.TokenConstants.USER_TOKEN_REQUESTING_CLIENT_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.common.util.OAuth2Utils.GRANT_TYPE;


public class UserTokenGranterTest {

    private UserTokenGranter granter;
    private AuthorizationServerTokenServices tokenServices;
    private ClientDetailsService clientDetailsService;
    private OAuth2RequestFactory requestFactory;
    private UaaOauth2Authentication authentication;
    private TokenRequest tokenRequest;
    private UaaAuthentication userAuthentication;
    private Map<String,String> requestParameters;

    @Before
    public void setup() {
        tokenServices = mock(AuthorizationServerTokenServices.class);
        clientDetailsService = mock(ClientDetailsService.class);
        requestFactory = mock(OAuth2RequestFactory.class);
        authentication = mock(UaaOauth2Authentication.class);

        userAuthentication = mock(UaaAuthentication.class);
        granter = new UserTokenGranter(
            tokenServices,
            clientDetailsService,
            requestFactory
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        requestParameters = new HashMap<>();
        requestParameters.put(USER_TOKEN_REQUESTING_CLIENT_ID, "clientId");
        requestParameters.put(GRANT_TYPE, TokenConstants.GRANT_TYPE_USER_TOKEN);
        tokenRequest = new PublicTokenRequest();
        tokenRequest.setRequestParameters(requestParameters);
    }

    @After
    public void teardown() {
        SecurityContextHolder.clearContext();
    }

    @Test(expected = InsufficientAuthenticationException.class)
    public void test_no_authentication() throws Exception {
        SecurityContextHolder.clearContext();
        granter.validateRequest(tokenRequest);
    }

    @Test(expected = InsufficientAuthenticationException.class)
    public void test_not_authenticated() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(false);
        granter.validateRequest(tokenRequest);
    }

    @Test(expected = InsufficientAuthenticationException.class)
    public void test_not_a_user_authentication() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getUserAuthentication()).thenReturn(null);
        granter.validateRequest(tokenRequest);
    }

    @Test(expected = InvalidGrantException.class)
    public void test_invalid_grant_type() throws Exception {
        missing_parameter(GRANT_TYPE);
    }

    @Test(expected = InvalidGrantException.class)
    public void test_requesting_client_id_missing() throws Exception {
        missing_parameter(USER_TOKEN_REQUESTING_CLIENT_ID);
    }

    @Test
    public void happy_day() {
        missing_parameter("non existent");
    }

    protected void missing_parameter(String parameter) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getUserAuthentication()).thenReturn(null);
        when(authentication.getUserAuthentication()).thenReturn(userAuthentication);
        when(userAuthentication.isAuthenticated()).thenReturn(true);
        requestParameters.remove(parameter);
        tokenRequest.setGrantType(requestParameters.get(GRANT_TYPE));
        granter.validateRequest(tokenRequest);
    }

    public static class PublicTokenRequest extends TokenRequest {
        public PublicTokenRequest() {
        }
    }
}