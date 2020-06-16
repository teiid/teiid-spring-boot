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


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.social.support.LoggingErrorHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

@ConnectionFactoryConfiguration(
        alias = "rest",
        translatorName = "rest"
        )
public class RestConnectionFactory implements BaseConnectionFactory<RestConnection> {
    private static final String AUTHORIZATION = "Authorization"; //$NON-NLS-1$

    private String securityType;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String refreshToken;
    private String authorizeUrl;
    private String accessTokenUrl;
    private String scope;
    private String endpoint;
    private boolean disableTrustManager = true;
    private boolean disableHostNameVerification;

    @Value("${teiid.ssl.trustStoreFileName:/etc/tls/private/truststore.pkcs12}")
    private String trustStoreFileName;
    @Value("${teiid.ssl.trustStorePassword:changeit}")
    private String trustStorePassword;

    private RestTemplate template;

    @Autowired
    private BeanFactory beanFactory;

    private AccessGrant accessGrant;

    public RestConnectionFactory() {
    }

    @Override
    public RestConnection getConnection() throws Exception {
        if (this.template == null) {
            this.template = createRestTemplate();
        }
        if (this.securityType == null) {
            Map<String, List<String>> headers = new HashMap<>();
            return new RestConnection(template, beanFactory, this.endpoint, headers);
        }
        else if (this.securityType.contentEquals("http-basic")) {
            if (this.username == null || this.password == null) {
                throw new IllegalStateException("http-basic authentication configured, "
                        + "however userid/password information not provided");
            }
            Map<String, List<String>> headers = new HashMap<>();
            String str = this.username+":"+this.password;
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
            if (this.username != null && this.password != null) {
                this.accessGrant = t.exchangeCredentialsForAccess(this.username, this.password,
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
            SSLConnectionSocketFactory csf = null;
            if (this.disableTrustManager) {
                csf = new SSLConnectionSocketFactory(
                        SSLContexts.custom().loadTrustMaterial(null, new TrustAllStrategy()).build(),
                        NoopHostnameVerifier.INSTANCE);
            } else {
                if (this.trustStoreFileName == null || !(new File(this.trustStoreFileName)).exists()) {
                    throw new IllegalStateException("Truststore name is not provided for Rest based datasource, "
                            + "if `https` verification is not required then turn on 'disableTrustManager` to "
                            + "skip the certifictate verification");
                }
                File trustStore = new File(this.trustStoreFileName);
                csf = new SSLConnectionSocketFactory(SSLContexts.custom()
                        .loadTrustMaterial(trustStore, this.trustStorePassword.toCharArray()).build(),
                        this.disableHostNameVerification ? NoopHostnameVerifier.INSTANCE : null);
            }

            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
            restTemplate.getMessageConverters().add(new FormMapHttpMessageConverter());
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            restTemplate.setErrorHandler(new LoggingErrorHandler());
            return restTemplate;
        } catch (Exception e) {
            throw new IllegalStateException("failed to create Rest cleint for making outgoing API calls", e);
        }
    }

    /**
     * The security type.  Can be http-basic, openid-connect, or null for none.
     * @return
     */
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
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

    public boolean isDisableTrustManager() {
        return disableTrustManager;
    }

    public void setDisableTrustManager(boolean disableTrustManager) {
        this.disableTrustManager = disableTrustManager;
    }

    public boolean isDisableHostNameVerification() {
        return disableHostNameVerification;
    }

    public void setDisableHostNameVerification(boolean disableHostNameVerification) {
        this.disableHostNameVerification = disableHostNameVerification;
    }

    public String getTrustStoreFileName() {
        return trustStoreFileName;
    }

    public void setTrustStoreFileName(String trustStoreFileName) {
        this.trustStoreFileName = trustStoreFileName;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @Override
    public void close() throws IOException {
    }
}
