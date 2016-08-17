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

package org.cloudfoundry.identity.uaa.mock.token;

import org.cloudfoundry.identity.uaa.constants.OriginKeys;
import org.cloudfoundry.identity.uaa.mock.InjectedMockContextTest;
import org.cloudfoundry.identity.uaa.oauth.UaaTokenServices;
import org.cloudfoundry.identity.uaa.oauth.client.ClientConstants;
import org.cloudfoundry.identity.uaa.oauth.token.RevocableTokenProvisioning;
import org.cloudfoundry.identity.uaa.provider.IdentityProvider;
import org.cloudfoundry.identity.uaa.provider.IdentityProviderProvisioning;
import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimGroupMember;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.cloudfoundry.identity.uaa.scim.exception.MemberAlreadyExistsException;
import org.cloudfoundry.identity.uaa.scim.jdbc.JdbcScimGroupMembershipManager;
import org.cloudfoundry.identity.uaa.scim.jdbc.JdbcScimGroupProvisioning;
import org.cloudfoundry.identity.uaa.scim.jdbc.JdbcScimUserProvisioning;
import org.cloudfoundry.identity.uaa.test.TestClient;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneProvisioning;
import org.junit.Before;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.cloudfoundry.identity.uaa.mock.util.MockMvcUtils.getClientCredentialsOAuthAccessToken;

public class AbstractTokenMockMvcTests extends InjectedMockContextTest {

    public static final String SECRET = "secret";
    public static final String GRANT_TYPES = "password,implicit,client_credentials,authorization_code";
    public static final String TEST_REDIRECT_URI = "http://test.example.org/redirect";

    protected TestClient testClient;
    protected JdbcClientDetailsService clientDetailsService;
    protected JdbcScimUserProvisioning userProvisioning;
    protected JdbcScimGroupProvisioning groupProvisioning;
    protected JdbcScimGroupMembershipManager groupMembershipManager;
    protected UaaTokenServices tokenServices;
    protected Set<String> defaultAuthorities;

    protected IdentityZoneProvisioning identityZoneProvisioning;
    protected JdbcScimUserProvisioning jdbcScimUserProvisioning;
    protected IdentityProviderProvisioning identityProviderProvisioning;
    protected String adminToken;
    protected RevocableTokenProvisioning tokenProvisioning;

    @Before
    public void setUpContext() throws Exception {
        testClient = new TestClient(getMockMvc());
        clientDetailsService = (JdbcClientDetailsService) getWebApplicationContext().getBean("jdbcClientDetailsService");
        userProvisioning = (JdbcScimUserProvisioning) getWebApplicationContext().getBean("scimUserProvisioning");
        groupProvisioning = (JdbcScimGroupProvisioning) getWebApplicationContext().getBean("scimGroupProvisioning");
        groupMembershipManager = (JdbcScimGroupMembershipManager) getWebApplicationContext().getBean("groupMembershipManager");
        tokenServices = (UaaTokenServices) getWebApplicationContext().getBean("tokenServices");
        defaultAuthorities = (Set<String>) getWebApplicationContext().getBean("defaultUserAuthorities");
        identityZoneProvisioning = getWebApplicationContext().getBean(IdentityZoneProvisioning.class);
        jdbcScimUserProvisioning = getWebApplicationContext().getBean(JdbcScimUserProvisioning.class);
        identityProviderProvisioning = getWebApplicationContext().getBean(IdentityProviderProvisioning.class);
        IdentityZoneHolder.clear();

        adminToken =
            getClientCredentialsOAuthAccessToken(
                getMockMvc(),
                "admin",
                "adminsecret",
                "uaa.admin",
                null
            );
        tokenProvisioning = (RevocableTokenProvisioning) getWebApplicationContext().getBean("revocableTokenProvisioning");
    }

    protected IdentityZone setupIdentityZone(String subdomain) {
        IdentityZone zone = new IdentityZone();
        zone.getConfig().getTokenPolicy().setKeys(Collections.singletonMap(subdomain+"_key", "key_for_"+subdomain));
        zone.setId(UUID.randomUUID().toString());
        zone.setName(subdomain);
        zone.setSubdomain(subdomain);
        zone.setDescription(subdomain);
        identityZoneProvisioning.create(zone);
        return zone;
    }

    protected IdentityProvider setupIdentityProvider() {
        return setupIdentityProvider(OriginKeys.UAA);
    }
    protected IdentityProvider setupIdentityProvider(String origin) {
        IdentityProvider defaultIdp = new IdentityProvider();
        defaultIdp.setName(origin);
        defaultIdp.setType(origin);
        defaultIdp.setOriginKey(origin);
        defaultIdp.setIdentityZoneId(IdentityZoneHolder.get().getId());
        return identityProviderProvisioning.create(defaultIdp);
    }

    protected BaseClientDetails setUpClients(String id, String authorities, String scopes, String grantTypes, Boolean autoapprove) {
        return setUpClients(id, authorities, scopes, grantTypes, autoapprove, null);
    }
    protected BaseClientDetails setUpClients(String id, String authorities, String scopes, String grantTypes, Boolean autoapprove, String redirectUri) {
        return setUpClients(id, authorities, scopes, grantTypes, autoapprove, redirectUri, null);
    }
    protected BaseClientDetails setUpClients(String id, String authorities, String scopes, String grantTypes, Boolean autoapprove, String redirectUri, List<String> allowedIdps) {
        return setUpClients(id, authorities, scopes, grantTypes, autoapprove, redirectUri, allowedIdps, -1);
    }
    protected BaseClientDetails setUpClients(String id, String authorities, String scopes, String grantTypes, Boolean autoapprove, String redirectUri, List<String> allowedIdps, int accessTokenValidity) {
        BaseClientDetails c = new BaseClientDetails(id, "", scopes, grantTypes, authorities);
        if (!"implicit".equals(grantTypes)) {
            c.setClientSecret(SECRET);
        }
        c.setRegisteredRedirectUri(new HashSet<>(Arrays.asList(TEST_REDIRECT_URI)));
        c.setAutoApproveScopes(Collections.singleton(autoapprove.toString()));
        Map<String, Object> additional = new HashMap<>();
        if (allowedIdps!=null && !allowedIdps.isEmpty()) {
            additional.put(ClientConstants.ALLOWED_PROVIDERS, allowedIdps);
        }
        c.setAdditionalInformation(additional);
        if (StringUtils.hasText(redirectUri)) {
            c.setRegisteredRedirectUri(new HashSet<>(Arrays.asList(redirectUri)));
        }
        if (accessTokenValidity>0) {
            c.setAccessTokenValiditySeconds(accessTokenValidity);
        }
        clientDetailsService.addClientDetails(c);
        return (BaseClientDetails) clientDetailsService.loadClientByClientId(c.getClientId());
    }

    protected ScimUser setUpUser(String username, String scopes, String origin, String zoneId) {
        ScimUser user = new ScimUser(null, username, "GivenName", "FamilyName");
        user.setPassword(SECRET);
        ScimUser.Email email = new ScimUser.Email();
        email.setValue("test@test.org");
        email.setPrimary(true);
        user.setEmails(Arrays.asList(email));
        user.setVerified(true);
        user.setOrigin(origin);

        user = userProvisioning.createUser(user, SECRET);

        Set<String> scopeSet = StringUtils.commaDelimitedListToSet(scopes);
        Set<ScimGroup> groups = new HashSet<>();
        for (String scope : scopeSet) {
            ScimGroup g = createIfNotExist(scope,zoneId);
            groups.add(g);
            addMember(user, g);
        }

        return userProvisioning.retrieve(user.getId());
    }

    protected ScimGroupMember addMember(ScimUser user, ScimGroup group) {
        ScimGroupMember gm = new ScimGroupMember(user.getId());
        try {
            return groupMembershipManager.addMember(group.getId(), gm);
        }catch (MemberAlreadyExistsException x) {
            return gm;
        }
    }

    protected ScimGroup createIfNotExist(String scope, String zoneId) {
        List<ScimGroup> exists = groupProvisioning.query("displayName eq \"" + scope + "\" and identity_zone_id eq \""+zoneId+"\"");
        if (exists.size() > 0) {
            return exists.get(0);
        } else {
            return groupProvisioning.create(new ScimGroup(null,scope,zoneId));
        }
    }
}
