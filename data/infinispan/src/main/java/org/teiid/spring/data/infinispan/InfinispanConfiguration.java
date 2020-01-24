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

public class InfinispanConfiguration {
    private String url;
    private String cacheName;

    // security
    private static final String[] saslAllowed = {"CRAM-MD5", "DIGEST-MD5", "PLAIN"};
    private String saslMechanism;
    private String userName;
    private String password;
    private String authenticationRealm;
    private String authenticationServerName;
    private String cacheTemplate;

    private String trustStoreFileName = System.getProperty("javax.net.ssl.trustStore");
    private String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
    private String keyStoreFileName = System.getProperty("javax.net.ssl.keyStore");
    private String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }
    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
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

    public String getAuthenticationRealm() {
        return authenticationRealm;
    }

    public void setAuthenticationRealm(String authenticationRealm) {
        this.authenticationRealm = authenticationRealm;
    }

    public String getAuthenticationServerName() {
        return authenticationServerName;
    }

    public void setAuthenticationServerName(String authenticationServerName) {
        this.authenticationServerName = authenticationServerName;
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

    public String getKeyStoreFileName() {
        return keyStoreFileName;
    }

    public void setKeyStoreFileName(String keyStoreFileName) {
        this.keyStoreFileName = keyStoreFileName;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getCacheTemplate() {
        return cacheTemplate;
    }

    public void setCacheTemplate(String cacheTemplate) {
        this.cacheTemplate = cacheTemplate;
    }

    public static String[] getSaslallowed() {
        return saslAllowed;
    }
}
