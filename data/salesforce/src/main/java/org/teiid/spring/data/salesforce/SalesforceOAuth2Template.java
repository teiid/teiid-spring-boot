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

import java.util.Map;

import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SalesforceOAuth2Template extends OAuth2Template {
    private String instanceUrl = null;
    private String id = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public SalesforceOAuth2Template(String clientId, String clientSecret, String authorizeUrl,
            String accessTokenUrl) {
        super(clientId, clientSecret, authorizeUrl, accessTokenUrl);
        setUseParametersForClientAuthentication(true);
    }

    @Override
    protected AccessGrant postForAccessGrant(String accessTokenUrl, MultiValueMap<String, String> parameters) {
        JsonNode response = getRestTemplate().postForObject(accessTokenUrl, parameters, JsonNode.class);

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.convertValue(response, Map.class);
        this.instanceUrl = (String) result.get("instance_url");
        this.id = (String)result.get("id");
        return this.createAccessGrant((String) result.get("access_token"), (String) result.get("scope"),
                (String) result.get("refresh_token"), getIntegerValue(result, "expires_in"), result);
    }

    private Long getIntegerValue(Map<String, Object> map, String key) {
        try {
            return Long.valueOf(String.valueOf(map.get(key))); // normalize to String before creating integer value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
