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

package org.teiid.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.teiid.transport.SSLConfiguration;

@ConfigurationProperties(prefix = "teiid")
public class TeiidProperties {

    private String vdbFile;
    private int metadataLoadWaitTimeInMillis = 30000;
    private boolean jdbcEnable = false;
    private boolean jdbcSecureEnable = false;
    private int jdbcPort = 31000;
    private int jdbcSecurePort = 31443;
    private boolean pgEnable = false;
    private boolean pgSecureEnable = false;
    private int pgPort = 35432;
    private int pgSecurePort = 35443;
    private boolean allowAlter = false;
    private SSLConfiguration ssl = new SSLConfiguration();

    public TeiidProperties() {
        this.ssl.setMode(SSLConfiguration.ENABLED);
    }

    public boolean isAllowAlter() {
        return allowAlter;
    }

    public void setAllowAlter(boolean allowAlter) {
        this.allowAlter = allowAlter;
    }

    public String getVdbFile() {
        return vdbFile;
    }

    public void setVdbFile(String vdb) {
        this.vdbFile = vdb;
    }

    public int getMetadataLoadWaitTimeInMillis() {
        return metadataLoadWaitTimeInMillis;
    }

    public void setMetadataLoadWaitTimeInMillis(int metadataLoadWaitTimeInMillis) {
        this.metadataLoadWaitTimeInMillis = metadataLoadWaitTimeInMillis;
    }

    public boolean isJdbcEnable() {
        return jdbcEnable;
    }

    public void setJdbcEnable(boolean jdbcEnable) {
        this.jdbcEnable = jdbcEnable;
    }

    public int getJdbcPort() {
        return jdbcPort;
    }

    public void setJdbcPort(int jdbcPort) {
        this.jdbcPort = jdbcPort;
    }

    public boolean isPgEnable() {
        return pgEnable;
    }

    public void setPgEnable(boolean pgEnable) {
        this.pgEnable = pgEnable;
    }

    public int getPgPort() {
        return pgPort;
    }

    public void setPgPort(int pgPort) {
        this.pgPort = pgPort;
    }

    public boolean isJdbcSecureEnable() {
        return jdbcSecureEnable;
    }

    public void setJdbcSecureEnable(boolean jdbcSecureEnable) {
        this.jdbcSecureEnable = jdbcSecureEnable;
    }

    public boolean isPgSecureEnable() {
        return pgSecureEnable;
    }

    public void setPgSecureEnable(boolean pgSecureEnable) {
        this.pgSecureEnable = pgSecureEnable;
    }

    public int getJdbcSecurePort() {
        return jdbcSecurePort;
    }

    public void setJdbcSecurePort(int jdbcSecurePort) {
        this.jdbcSecurePort = jdbcSecurePort;
    }

    public int getPgSecurePort() {
        return pgSecurePort;
    }

    public void setPgSecurePort(int pgSecurePort) {
        this.pgSecurePort = pgSecurePort;
    }

    public SSLConfiguration getSsl() {
        return ssl;
    }

    public void setSsl(SSLConfiguration ssl) {
        this.ssl = ssl;
    }

}
