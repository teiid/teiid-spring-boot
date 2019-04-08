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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.util.LinkedMultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class TeiidPartnerConnection extends PartnerConnection {
    private static final String AUTHORIZATION = "Authorization"; //$NON-NLS-1$
    private AccessGrant accessGrant;

    public TeiidPartnerConnection(ConnectorConfig config) throws ConnectionException {
        super(config);
    }

    @Override
    public com.sforce.soap.partner.LoginResult login(String username, String password)
            throws com.sforce.ws.ConnectionException {

        SalesforceConnectorConfig config = (SalesforceConnectorConfig)getConfig();

        if (config.getRefreshToken() == null) {
            if (username != null && password != null) {
                this.accessGrant = config.getOAuth2Template().exchangeCredentialsForAccess(username, password,
                        new LinkedMultiValueMap<String, String>());
            }
        } else {
            this.accessGrant = config.getOAuth2Template().refreshAccess(config.getRefreshToken(),
                    new LinkedMultiValueMap<String, String>());
        }

        if (accessGrant == null) {
            throw new com.sforce.ws.ConnectionException("Failed to get OAuth based connection; "
                    + "Failed to get user information");
        }

        String accessToken = this.accessGrant.getAccessToken();
        com.sforce.soap.partner.LoginResult loginResult = null;

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, "Bearer "+accessToken);
        headers.set("Accept", "application/xml");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = config.getRestTemplate().exchange(URI.create(config.getOAuth2Template().getId()),
                HttpMethod.GET, entity, String.class);
        String result = response.getBody();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(result.getBytes()));
            doc.getDocumentElement().normalize();

            Element urls = (Element)doc.getDocumentElement().getElementsByTagName("urls").item(0);
            loginResult = new com.sforce.soap.partner.LoginResult();
            loginResult.setSessionId(accessToken); // remove "Bearer " prefix.

            String endpoint = getConfig().getAuthEndpoint();
            int index = endpoint.indexOf("Soap/u/"); //$NON-NLS-1$
            String apiVersion = endpoint.substring(index+7);

            String partnerURL = urls.getElementsByTagName("partner").item(0).getTextContent();
            partnerURL = partnerURL.replace("{version}", apiVersion);

            loginResult.setServerUrl(partnerURL);

        } catch (IOException e) {
            throw new com.sforce.ws.ConnectionException("Failed to get OAuth based connection; "
                    + "Failed to get user information", e);
        } catch (ParserConfigurationException e) {
            throw new com.sforce.ws.ConnectionException("Failed to get OAuth based connection; "
                    + "Failed to get user information", e);
        } catch (IllegalStateException e) {
            throw new com.sforce.ws.ConnectionException("Failed to get OAuth based connection; "
                    + "Failed to get user information", e);
        } catch (SAXException e) {
            throw new com.sforce.ws.ConnectionException("Failed to get OAuth based connection; "
                    + "Failed to get user information", e);
        }
        return loginResult;
    }

    public boolean isAccessTokenValid() {
        if (this.accessGrant == null) {
            return false;
        }

        if (this.accessGrant.getExpireTime() != null) {
            return this.accessGrant.getExpireTime() < System.currentTimeMillis();
        }
        return this.accessGrant.getAccessToken() != null;
    }
}
