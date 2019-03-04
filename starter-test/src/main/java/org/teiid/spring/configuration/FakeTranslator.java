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
package org.teiid.spring.configuration;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.teiid.language.QueryExpression;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.resource.api.Connection;
import org.teiid.resource.api.ConnectionFactory;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TranslatorProperty;
import org.teiid.translator.TypeFacility;

@Translator(name = "fake")
public class FakeTranslator extends ExecutionFactory<Object, Object> {

    public Connection getConnection(ConnectionFactory<Connection> factory) throws TranslatorException {
        try {
            return factory.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void getMetadata(MetadataFactory metadataFactory, Object conn) throws TranslatorException {
        Table t = metadataFactory.addTable("mytable");
        t.setSupportsUpdate(true);
        Column c = metadataFactory.addColumn("mycolumn", TypeFacility.RUNTIME_NAMES.STRING, t);
        c.setUpdatable(true);
    }

    @Override
    public ResultSetExecution createResultSetExecution(QueryExpression command, ExecutionContext executionContext,
            RuntimeMetadata metadata, Object connection) throws TranslatorException {
        ResultSetExecution rse = new ResultSetExecution() {
            boolean first = true;

            @Override
            public void execute() throws TranslatorException {
            }

            @Override
            public void close() {
            }

            @Override
            public void cancel() throws TranslatorException {
            }

            @Override
            public List<?> next() throws TranslatorException, DataNotAvailableException {
                if (!first) {
                    return null;
                }
                first = false;
                String[] results = { myProperty == null?"one":myProperty };
                return Arrays.asList(results);
            }
        };
        return rse;
    }

    @Override
    public boolean isSourceRequiredForMetadata() {
        return false;
    }

    private String myProperty;

    @TranslatorProperty
    public String getMyProperty() {
        return myProperty;
    }

    public void setMyProperty(String prop) {
        myProperty = prop;
    }
}
