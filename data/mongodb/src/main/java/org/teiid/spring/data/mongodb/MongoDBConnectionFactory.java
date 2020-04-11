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

import org.springframework.data.mongodb.core.MongoTemplate;
import org.teiid.spring.common.SourceType;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@ConnectionFactoryConfiguration(
        alias = "mongodb",
        translatorName = "mongodb",
        dependencies = {"org.teiid:spring-data-mongodb"},
        propertyPrefix= "spring.teiid.data.mongodb",
        sourceType=SourceType.MongoDB
        )
public class MongoDBConnectionFactory extends BaseConnectionFactory<MongoDBConnection> {

    private MongoTemplate mongoTemplate;
    private MongoClient mongoClient;

    public MongoDBConnectionFactory(MongoTemplate template) {
        this.mongoTemplate = template;
    }

    public MongoDBConnectionFactory(MongoDBConfiguration mongoDBConfiguration) {
        String database = mongoDBConfiguration.getDatabase();
        if (mongoDBConfiguration.getUri() != null) {
            MongoClientURI uri = new MongoClientURI(mongoDBConfiguration.getUri());
            mongoClient = new MongoClient(uri);
            database = uri.getDatabase();
        }
        if (mongoDBConfiguration.getCredential() == null) {
            mongoClient = new MongoClient(mongoDBConfiguration.getServers(), mongoDBConfiguration.getOptions());
        } else {
            mongoClient = new MongoClient(mongoDBConfiguration.getServers(), mongoDBConfiguration.getCredential(), mongoDBConfiguration.getOptions());
        }
        this.mongoTemplate = new MongoTemplate(mongoClient, database);
    }

    @Override
    public MongoDBConnection getConnection() throws Exception {
        return new MongoDBConnection(mongoTemplate);
    }

    @Override
    public void close() throws IOException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
