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

package org.teiid.spring.data;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.teiid.core.util.EquivalenceUtil;

@SuppressWarnings("serial")
public abstract class BaseConnectionFactory implements javax.resource.cci.ConnectionFactory {

    private String translatorName;

    @Override
    public void setReference(Reference reference) {

    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return null;
    }

    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        return null;
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    protected static boolean checkEquals(Object left, Object right) {
        return EquivalenceUtil.areEqual(left, right);
    }

    public String getTranslatorName() {
        return translatorName;
    }

    public void setTranslatorName(String name) {
        this.translatorName = name;
    }
}
