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
package org.teiid.spring.data.infinispan;

import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.springframework.beans.factory.annotation.Value;

public class InfinispanConfiguration implements org.teiid.infinispan.api.InfinispanConfiguration {
    private String url;
    private String cacheName;

    // security
    private String saslMechanism;
    private String username;
    private String password;
    private String authenticationRealm = "default";
    private String authenticationServerName = "infinispan";
    private String cacheTemplate;

    @Value("${teiid.ssl.trustStoreFileName:/etc/tls/private/truststore.pkcs12}")
    private String trustStoreFileName;
    @Value("${teiid.ssl.trustStorePassword:changeit}")
    private String trustStorePassword;
    @Value("${teiid.ssl.keyStoreFileName:/etc/tls/private/keystore.pkcs12}")
    private String keyStoreFileName;
    @Value("${teiid.ssl.keyStorePassword:changeit}")
    private String keyStorePassword;

    private TransactionMode transactionMode;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }
    @Override
    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getAuthenticationRealm() {
        return authenticationRealm;
    }

    public void setAuthenticationRealm(String authenticationRealm) {
        this.authenticationRealm = authenticationRealm;
    }

    @Override
    public String getAuthenticationServerName() {
        return authenticationServerName;
    }

    public void setAuthenticationServerName(String authenticationServerName) {
        this.authenticationServerName = authenticationServerName;
    }

    @Override
    public String getTrustStoreFileName() {
        return trustStoreFileName;
    }

    public void setTrustStoreFileName(String trustStoreFileName) {
        this.trustStoreFileName = trustStoreFileName;
    }

    @Override
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @Override
    public String getKeyStoreFileName() {
        return keyStoreFileName;
    }

    public void setKeyStoreFileName(String keyStoreFileName) {
        this.keyStoreFileName = keyStoreFileName;
    }

    @Override
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public String getCacheTemplate() {
        return cacheTemplate;
    }

    public void setCacheTemplate(String cacheTemplate) {
        this.cacheTemplate = cacheTemplate;
    }

    public void setTransactionMode(TransactionMode transactionMode) {
        this.transactionMode = transactionMode;
    }

    @Override
    public String getTransactionMode() {
        if (transactionMode == null) {
            return null;
        }
        return transactionMode.name();
    }

    @Override
    public String getRemoteServerList() {
        return url;
    }

    public void setRemoteServerList(String remoteSeverList) {
        this.url = remoteSeverList;
    }
}
