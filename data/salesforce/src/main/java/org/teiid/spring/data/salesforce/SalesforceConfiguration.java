/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.data.salesforce;

import org.springframework.web.client.RestTemplate;

public class SalesforceConfiguration implements org.teiid.salesforce.SalesforceConfiguration {
    private String url = "https://login.salesforce.com/services/Soap/u/45.0";
    private Long requestTimeout = Long.MAX_VALUE;
    private Long connectTimeout = Long.MAX_VALUE;
    private Integer pollingInterval = 500;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String refreshToken;
    private RestTemplate restTemplate = new RestTemplate();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SalesforceOAuth2Template getOAuth2Template() {
        SalesforceOAuth2Template template = new SalesforceOAuth2Template(clientId, clientSecret,
                "https://login.salesforce.com/services/oauth2/authorize",
                "https://login.salesforce.com/services/oauth2/token");
        template.setUseParametersForClientAuthentication(true);
        return template;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    @Override
    public Long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Integer pollingInterval) {
        this.pollingInterval = pollingInterval;
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

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
