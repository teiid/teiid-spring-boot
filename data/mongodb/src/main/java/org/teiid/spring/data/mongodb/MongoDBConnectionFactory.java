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
package org.teiid.spring.data.mongodb;


import java.io.IOException;

import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@ConnectionFactoryConfiguration(
        alias = "mongodb",
        translatorName = "mongodb",
        dependencies = {"org.teiid:spring-data-mongodb"},
        propertyPrefix= "spring.teiid.data.mongodb"
        )
public class MongoDBConnectionFactory implements BaseConnectionFactory<MongoDBConnection> {

    private MongoClient mongoClient;
    private MongoDBConfiguration config;

    public MongoDBConnectionFactory(MongoDBConfiguration mongoDBConfiguration) {
        this.config = mongoDBConfiguration;
    }

    @Override
    public MongoDBConnection getConnection() throws Exception {
        if (this.mongoClient == null) {
            if (this.config.getUri() != null) {
                MongoClientURI uri = new MongoClientURI(this.config.getUri());
                mongoClient = new MongoClient(uri);
            }
            if (this.config.getCredential() == null) {
                mongoClient = new MongoClient(this.config.getServers(), this.config.getOptions());
            } else {
                mongoClient = new MongoClient(this.config.getServers(), this.config.getCredential(),this.config.getOptions());
            }

        }
        return new MongoDBConnection(this.mongoClient, this.config.getDatabase());
    }

    @Override
    public void close() throws IOException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
