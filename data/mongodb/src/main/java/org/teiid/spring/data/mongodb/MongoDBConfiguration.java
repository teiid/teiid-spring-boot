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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class MongoDBConfiguration {
    public enum SecurityType {None, SCRAM_SHA_1, MONGODB_CR, Kerberos, X509}
    private String remoteServerList=null;
    private String user;
    private String password;
    private String database;
    private String securityType = SecurityType.SCRAM_SHA_1.name();
    private String authDatabase;
    private Boolean ssl;

    MongoClient mongoClient() {
        if (getCredential() == null) {
            return new MongoClient(getServers());
        }
        else {
            return new MongoClient(getServers(), getCredential(), getOptions());
        }
    }

    private MongoClientOptions getOptions() {
        //if options needed then use URL format
        final MongoClientOptions.Builder builder = MongoClientOptions.builder();
        if (getSsl()) {
            builder.sslEnabled(true);
        }
        return builder.build();
    }

    public MongoCredential getCredential() {
        MongoCredential credential = null;
        if (this.securityType.equals(SecurityType.SCRAM_SHA_1.name())) {
            credential = MongoCredential.createScramSha1Credential(this.user,
                    (this.authDatabase == null) ? this.database: this.authDatabase,
                            this.password.toCharArray());
        }
        else if (this.securityType.equals(SecurityType.MONGODB_CR.name())) {
            credential = MongoCredential.createMongoCRCredential(this.user,
                    (this.authDatabase == null) ? this.database: this.authDatabase,
                            this.password.toCharArray());
        }
        else if (this.securityType.equals(SecurityType.Kerberos.name())) {
            credential = MongoCredential.createGSSAPICredential(this.user);
        }
        else if (this.securityType.equals(SecurityType.X509.name())) {
            credential = MongoCredential.createMongoX509Credential(this.user);
        } else if (this.securityType.equals(SecurityType.None.name())) {
            // skip
        }
        else if (this.user != null && this.password != null) {
            // to support legacy pre-3.0 authentication
            credential = MongoCredential.createMongoCRCredential(
                    MongoDBConfiguration.this.user,
                    (this.authDatabase == null) ? this.database: this.authDatabase,
                            this.password.toCharArray());
        }
        return credential;
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

    protected MongoClientURI getConnectionURI() {
        String serverlist = getRemoteServerList();
        if (serverlist.startsWith("mongodb://")) { //$NON-NLS-1$
            return new MongoClientURI(getRemoteServerList());
        }
        return null;
    }

    protected List<ServerAddress> getServers() {
        String serverlist = getRemoteServerList();
        if (!serverlist.startsWith("mongodb://")) { //$NON-NLS-1$
            List<ServerAddress> addresses = new ArrayList<ServerAddress>();
            StringTokenizer st = new StringTokenizer(serverlist, ","); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int idx = token.indexOf(':');
                int port = 27017;
                if (idx > 0) {
                    port = Integer.valueOf(token.substring(idx+1));
                }
                addresses.add(new ServerAddress(token.substring(0, idx), port));
            }
            return addresses;
        }
        return null;
    }
}
