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
package org.teiid.spring.data.ftp;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTP;
import org.springframework.beans.factory.annotation.Autowired;

public class FtpConfiguration implements org.teiid.file.ftp.FtpConfiguration {

    private String parentDirectory;
    private String fileMapping;
    protected String username;
    protected String host;
    protected String password;
    protected Integer port = FTP.DEFAULT_PORT;
    protected Integer bufferSize = 2048;
    protected Integer clientMode = org.apache.commons.net.ftp.FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;
    protected Integer fileType = org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;
    protected String controlEncoding = FTP.DEFAULT_CONTROL_ENCODING;

    private Integer connectTimeout;
    private Integer defaultTimeout;
    private Integer dataTimeout;
    private Boolean isFtps = false;
    private Boolean useClientMode;
    private Boolean sessionCreation;

    private String authValue;
    private String[] cipherSuites;
    private String[] protocols;
    private Boolean needClientAuth;
    private Boolean wantsClientAuth;
    private Boolean implicit = false;
    private String execProt = "P"; //$NON-NLS-1$
    private String protocol;

    @Autowired(required=false)
    private TrustManager trustManager;

    @Autowired(required=false)
    private KeyManager keyManager;

    @Override
    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    @Override
    public String getFileMapping() {
        return fileMapping;
    }

    public void setFileMapping(String fileMapping) {
        this.fileMapping = fileMapping;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public Integer getClientMode() {
        return clientMode;
    }

    public void setClientMode(Integer clientMode) {
        this.clientMode = clientMode;
    }

    @Override
    public Integer getFileType() {
        return fileType;
    }

    /**
     * File types defined by {@link org.apache.commons.net.ftp.FTP} constants:
     * <ul>
     * <li>{@link org.apache.commons.net.ftp.FTP#ASCII_FILE_TYPE}</li>
     * <li>{@link org.apache.commons.net.ftp.FTP#EBCDIC_FILE_TYPE}</li>
     * <li>{@link org.apache.commons.net.ftp.FTP#BINARY_FILE_TYPE}</li>
     * <li>{@link org.apache.commons.net.ftp.FTP#LOCAL_FILE_TYPE}</li>
     * </ul>
     * @param fileType The file type.
     */
    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    @Override
    public String getControlEncoding() {
        return controlEncoding;
    }

    public void setControlEncoding(String controlEncoding) {
        this.controlEncoding = controlEncoding;
    }

    @Override
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public Integer getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Integer defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    public Integer getDataTimeout() {
        return dataTimeout;
    }

    public void setDataTimeout(Integer dataTimeout) {
        this.dataTimeout = dataTimeout;
    }

    @Override
    public Boolean getIsFtps() {
        return isFtps;
    }

    public void setIsFtps(Boolean isFtps) {
        this.isFtps = isFtps;
    }

    @Override
    public Boolean getUseClientMode() {
        return useClientMode;
    }

    public void setUseClientMode(Boolean useClientMode) {
        this.useClientMode = useClientMode;
    }

    @Override
    public Boolean getSessionCreation() {
        return sessionCreation;
    }

    public void setSessionCreation(Boolean sessionCreation) {
        this.sessionCreation = sessionCreation;
    }

    @Override
    public String getAuthValue() {
        return authValue;
    }

    public void setAuthValue(String authValue) {
        this.authValue = authValue;
    }

    public String getCipherSuites() {
        return formStringFromArray(cipherSuites);
    }

    private String formStringFromArray(String[] cipherSuites) {
        String result = ""; //$NON-NLS-1$
        for(String str : cipherSuites) {
            result = result + str + ","; //$NON-NLS-1$
        }
        return result.substring(0, result.length() - 1);
    }

    public void setCipherSuites(String cipherSuites) {
        this.cipherSuites = cipherSuites.split(","); //$NON-NLS-1$
    }

    public String getProtocols() {
        return formStringFromArray(protocols);
    }

    public void setProtocols(String protocols) {
        this.protocols = protocols.split(","); //$NON-NLS-1$
    }

    @Override
    public Boolean getNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(Boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    @Override
    public Boolean getWantsClientAuth() {
        return wantsClientAuth;
    }

    public void setWantsClientAuth(Boolean wantsClientAuth) {
        this.wantsClientAuth = wantsClientAuth;
    }

    @Override
    public Boolean isImplicit() {
        return implicit;
    }

    public void setImplicit(Boolean implicit) {
        this.implicit = implicit;
    }

    @Override
    public String getExecProt() {
        return execProt;
    }

    public void setExecProt(String execProt) {
        this.execProt = execProt;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public TrustManager getTrustManager() {
        return this.trustManager;
    }

    @Override
    public KeyManager getKeyManager() {
        return this.keyManager;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.cipherSuites;
    }

    @Override
    public String[] getSupportedProtocols() {
        return this.protocols;
    }
}
