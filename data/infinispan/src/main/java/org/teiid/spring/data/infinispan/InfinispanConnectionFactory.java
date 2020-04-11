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

import java.io.IOException;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.teiid.infinispan.api.BaseInfinispanConnection;
import org.teiid.spring.common.SourceType;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.translator.TranslatorException;

@ConnectionFactoryConfiguration(
        alias = "infinispan-hotrod",
        translatorName = "infinispan-hotrod",
        dependencies = {"org.teiid:spring-data-infinispan"},
        propertyPrefix= "spring.teiid.data.infinispan",
        sourceType=SourceType.Infinispan
        )
public class InfinispanConnectionFactory extends BaseConnectionFactory<BaseInfinispanConnection> {

    private org.teiid.infinispan.api.InfinispanConnectionFactory icf;
    private InfinispanConfiguration config;

    @Autowired(required = false)
    private TransactionManager transactionManager;

    public InfinispanConnectionFactory(InfinispanConfiguration config) {
        this.config = config;
        try {
            this.icf = new org.teiid.infinispan.api.InfinispanConnectionFactory(config, ()->{return transactionManager;});
        } catch (TranslatorException e) {
            throw new RuntimeException(e);
        }
    }

    public InfinispanConfiguration getConfig() {
        return config;
    }

    @Override
    public BaseInfinispanConnection getConnection() throws Exception {
        return icf.getConnection();
    }

    @Override
    public void close() throws IOException {
        this.icf.close();
    }
}
