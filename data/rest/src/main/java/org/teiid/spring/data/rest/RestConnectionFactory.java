/*
 * Copyright 2012-2017 the original author or authors.
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

package org.teiid.spring.data.rest;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.social.support.LoggingErrorHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.teiid.spring.data.BaseConnectionFactory;

@ConfigurationProperties(prefix="spring.teiid.rest")
public class RestConnectionFactory extends BaseConnectionFactory<RestConnection> {
    private static final String AUTHORIZATION = "Authorization"; //$NON-NLS-1$

    private String securityType;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
    private String refreshToken;
    private String authorizeUrl;
    private String accessTokenUrl;
    private String scope;
    private String endpoint;

    private RestTemplate template;

    @Autowired
    private BeanFactory beanFactory;

    private AccessGrant accessGrant;

    public RestConnectionFactory() {
        super("rest");
        this.template = createRestTemplate();
    }

    @Override
    public RestConnection getConnection() throws Exception {
        if (this.securityType == null) {
            Map<String, List<String>> headers = new HashMap<>();
            return new RestConnection(template, beanFactory, this.endpoint, headers);
        }
        else if (this.securityType.contentEquals("http-basic")) {
            if (this.userName == null || this.password == null) {
                throw new IllegalStateException("http-basic authentication configured, "
                        + "however userid/password information not provided");
            }
            Map<String, List<String>> headers = new HashMap<>();
            String str = this.userName+":"+this.password;
            headers.put(AUTHORIZATION,Arrays.asList("Basic "+Base64.getEncoder().encodeToString(str.getBytes())));
            return new RestConnection(this.template, this.beanFactory, this.endpoint, headers);
        } else if (securityType.contentEquals("openid-connect")) {
            if (!isAccessTokenValid()) {
                refreshAccessToken();
            }
            Map<String, List<String>> headers = new HashMap<>();
            headers.put(AUTHORIZATION, Arrays.asList("Bearer "+this.accessGrant.getAccessToken()));
            return new RestConnection(this.template, this.beanFactory, this.endpoint, headers);
        } else {
            throw new IllegalStateException("Unsupported authentication for Rest layer " + this.securityType);
        }
    }

    private void refreshAccessToken() {
        RestOAuth2Template t = new RestOAuth2Template(this.clientId, this.clientSecret, this.authorizeUrl,
                this.accessTokenUrl, this.template);
        if (this.refreshToken == null) {
            if (this.userName != null && this.password != null) {
                this.accessGrant = t.exchangeCredentialsForAccess(this.userName, this.password,
                        new LinkedMultiValueMap<String, String>());
            } else {
                throw new IllegalStateException("openid-connect authentication configured, "
                        + "however userid/password information not provided nor refreshToken");
            }
        } else {
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add("scope", this.scope);
            this.accessGrant = t.refreshAccess(this.refreshToken, params);
        }
    }

    private boolean isAccessTokenValid() {
        if (this.accessGrant == null) {
            return false;
        }
        if (this.accessGrant.getExpireTime() != null) {
            return this.accessGrant.getExpireTime() < System.currentTimeMillis();
        }
        return this.accessGrant.getAccessToken() != null;
    }

    protected RestTemplate createRestTemplate() {
        try {
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
            restTemplate.getMessageConverters().add(new FormMapHttpMessageConverter());
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            restTemplate.setErrorHandler(new LoggingErrorHandler());
            return restTemplate;
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            return null;
        }
    }

    public String getSecurityType() {
        return securityType;
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }

    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
