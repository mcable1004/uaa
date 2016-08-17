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

package org.cloudfoundry.identity.uaa.oauth;

import org.cloudfoundry.identity.uaa.oauth.token.TokenConstants;
import org.cloudfoundry.identity.uaa.web.AddParametersRequestWrapper;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cloudfoundry.identity.uaa.oauth.token.TokenConstants.USER_TOKEN_REQUESTING_CLIENT_ID;

public class UserTokenAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (TokenConstants.GRANT_TYPE_USER_TOKEN.equals(request.getParameter(OAuth2Utils.GRANT_TYPE)) &&
            request.getParameter(OAuth2Utils.CLIENT_ID)!=null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth!=null && auth.isAuthenticated() && auth instanceof UaaOauth2Authentication) {
                UaaOauth2Authentication authentication = (UaaOauth2Authentication)auth;
                String recipientClientId = request.getParameter(OAuth2Utils.CLIENT_ID);
                OAuth2Request newRequest = new OAuth2Request(
                    authentication.getOAuth2Request().getRequestParameters(),
                    recipientClientId,
                    authentication.getOAuth2Request().getAuthorities(),
                    authentication.getOAuth2Request().isApproved(),
                    getScopes(request),
                    authentication.getOAuth2Request().getResourceIds(),
                    authentication.getOAuth2Request().getRedirectUri(),
                    authentication.getOAuth2Request().getResponseTypes(),
                    authentication.getOAuth2Request().getExtensions()
                );
                UaaOauth2Authentication newAuthentication = new UaaOauth2Authentication(
                    authentication.getTokenValue(),
                    authentication.getZoneId(),
                    newRequest,
                    authentication.getUserAuthentication()
                );
                Map<String,String[]> addedParameters = new HashMap<>();
                addedParameters.put(USER_TOKEN_REQUESTING_CLIENT_ID, new String[] {authentication.getOAuth2Request().getClientId()});
                request = new AddParametersRequestWrapper(addedParameters, request);
                SecurityContextHolder.getContext().setAuthentication(newAuthentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    public Set<String> getScopes(HttpServletRequest request) {
        String scope = request.getParameter(OAuth2Utils.SCOPE);
        if (scope!=null) {
            String[] scopes = StringUtils.tokenizeToStringArray(scope, " ");
            return new HashSet<>(Arrays.asList(scopes));
        } else {
            return null;
        }
    }
}
