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

import java.util.List;

import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

public class DelegatingPlatformTransactionManager implements PlatformTransactionManager {
    private List<PlatformTransactionManager> tms;
    private ChainedTransactionManager delegate;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        check();
        return this.delegate.getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        check();
        this.delegate.commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        check();
        this.delegate.rollback(status);
    }

    // do not make copy, use same reference as the PlatformTransactionManagerAdapter
    // can keep adding the data sources.
    void setTransactionManagers(List<PlatformTransactionManager> tms) {
        this.tms = tms;
    }

    void check() throws TransactionException {
        if (this.delegate == null) {
            Assert.notNull(tms, "No registered data sources found");
            Assert.isTrue(!tms.isEmpty(), "No registered data sources found");
            this.delegate = new ChainedTransactionManager(tms.toArray(new PlatformTransactionManager[tms.size()]));
        }
    }
}

