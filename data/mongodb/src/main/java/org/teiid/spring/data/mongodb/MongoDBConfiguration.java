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
package org.teiid.spring.data.mongodb;

import com.mongodb.MongoClientOptions;

public class MongoDBConfiguration implements org.teiid.mongodb.MongoDBConfiguration {
    private String remoteServerList=null;
    private String user;
    private String password;
    private String database;
    private String securityType = SecurityType.SCRAM_SHA_1.name();
    private String authDatabase;
    private Boolean ssl;

    private String uri;

    public MongoClientOptions getOptions() {
        //if options needed then use URL format
        final MongoClientOptions.Builder builder = MongoClientOptions.builder();
        if (getSsl()) {
            builder.sslEnabled(true);
        }
        return builder.build();
    }

    /**
     * Returns the <code>host:port[;host:port...]</code> list that identifies the remote servers
     * to include in this cluster;
     * @return <code>host:port[;host:port...]</code> list
     */
    public String getRemoteServerList() {
        return this.remoteServerList;
    }

    /**
     * Set the list of remote servers that make up the MongoDB cluster.
     * @param remoteServerList the server list in appropriate <code>server:port;server2:port2</code> format.
     */
    public void setRemoteServerList( String remoteServerList ) {
        this.remoteServerList = remoteServerList;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String username) {
        this.user = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String googlePassword) {
        this.password = googlePassword;
    }

    public Boolean getSsl() {
        return this.ssl != null?this.ssl:false;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSecurityType() {
        return this.securityType;
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    public String getAuthDatabase() {
        return this.authDatabase;
    }

    public void setAuthDatabase(String database) {
        this.authDatabase = database;
    }

    /**
     * The full connection URI string to mongodb. If this is used, no other configuration properties will be looked at.
     * The database should also be set in the URI.
     * @return
     */
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getUsername() {
        return user;
    }
}
