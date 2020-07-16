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
package com.teiid.spring.data.amazon.simpledb;

import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.simpledb.api.BaseSimpleDBConfiguration;
import org.teiid.translator.simpledb.api.SimpleDBConnection;
import org.teiid.translator.simpledb.api.SimpleDBConnectionImpl;

@ConnectionFactoryConfiguration(
        alias = "simpledb",
        translatorName = "simpledb",
        configuration = SimpleDBConfiguration.class
)
public class SimpleDBConnectionFactory extends org.teiid.translator.simpledb.api.SimpleDBConnectionFactory implements BaseConnectionFactory<SimpleDBConnection> {
    public SimpleDBConnectionFactory(BaseSimpleDBConfiguration simpleDBConfig) throws TranslatorException {
        super(simpleDBConfig);
    }

    @Override
    public SimpleDBConnection getConnection() throws Exception {
        return new SimpleDBConnectionImpl(getSimpleDBClient());
    }

    @Override
    public void close() {

    }
}
