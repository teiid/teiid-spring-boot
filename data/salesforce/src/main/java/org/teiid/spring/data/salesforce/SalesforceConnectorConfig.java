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

import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.MessageHandler;

public class SalesforceConnectorConfig extends ConnectorConfig {
    private SalesforceOAuth2Template oauthTemplate;
    private RestTemplate restTemplate;
    private String refreshToken;

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SalesforceOAuth2Template getOAuth2Template() {
        return oauthTemplate;
    }

    public void setOAuth2Template(SalesforceOAuth2Template template) {
        this.oauthTemplate = template;
    }

    @Override
    public void addMessageHandler(MessageHandler handler) {
        throw new UnsupportedOperationException();
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
