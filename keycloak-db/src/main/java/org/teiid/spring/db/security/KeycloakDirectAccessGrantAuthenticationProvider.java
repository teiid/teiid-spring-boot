/*
 * Copyright 2012- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.db.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.adapters.jaas.DirectAccessGrantsLoginModule;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakAuthenticationException;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.util.JsonSerialization;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class KeycloakDirectAccessGrantAuthenticationProvider extends KeycloakAuthenticationProvider {

    private static final Logger log = Logger.getLogger(DirectAccessGrantsLoginModule.class);

    private KeycloakSpringBootConfigResolver resolver;
    private KeycloakDeployment deployment;
    private String scope;

    public KeycloakDirectAccessGrantAuthenticationProvider(
            KeycloakSpringBootConfigResolver keycloakSpringBootConfigResolver) {
        this.resolver = keycloakSpringBootConfigResolver;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (deployment == null) {
            deployment = resolver.resolve(null);
        }
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        if (token.getCredentials() == null) {
            throw new AuthenticationCredentialsNotFoundException("");
        }
        try {
            return directGrantAuth(token.getName(), token.getCredentials().toString());
        } catch (VerificationException|IOException e) {
            throw new KeycloakAuthenticationException(e.getMessage(), e);
        }
    }

    /**
     * @see DirectAccessGrantsLoginModule
     * @param username
     * @param password
     * @return
     * @throws IOException
     * @throws VerificationException
     */
    protected Authentication directGrantAuth(String username, String password) throws IOException, VerificationException {
        String authServerBaseUrl = deployment.getAuthServerBaseUrl();
        URI directGrantUri = KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.TOKEN_PATH).build(deployment.getRealm());
        HttpPost post = new HttpPost(directGrantUri);

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
        formparams.add(new BasicNameValuePair("username", username));
        formparams.add(new BasicNameValuePair("password", password));

        if (scope != null) {
            formparams.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
        }

        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);

        HttpClient client = deployment.getClient();
        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            StringBuilder errorBuilder = new StringBuilder("Login failed. Invalid status: " + status);
            if (entity != null) {
                InputStream is = entity.getContent();
                OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(is, OAuth2ErrorRepresentation.class);
                errorBuilder.append(", OAuth2 error. Error: " + errorRep.getError())
                        .append(", Error description: " + errorRep.getErrorDescription());
            }
            String error = errorBuilder.toString();
            log.warn(error);
            throw new IOException(error);
        }

        if (entity == null) {
            throw new IOException("No Entity");
        }

        InputStream is = entity.getContent();
        AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);

        AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenResponse.getToken(), tokenResponse.getIdToken(), deployment);

        return postTokenVerification(tokenResponse.getToken(), tokens.getAccessToken());
    }

    protected Authentication postTokenVerification(String tokenString, AccessToken token) {
        RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(deployment, null, tokenString, token, null, null, null);
        String principalName = AdapterUtils.getPrincipalName(deployment, token);
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(principalName, skSession);
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
        final KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, skSession);
        KeycloakAuthenticationToken newAuth = new KeycloakAuthenticationToken(account, false);
        //call to the super logic to map authorities
        return super.authenticate(newAuth);
    }

}
