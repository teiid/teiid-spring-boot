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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.teiid.spring.data.BaseConnectionFactory;

/**
 * Provides a light JTA TransactionManager based upon the
 * {@link PlatformTransactionManager} This is the transaction manager that Teiid
 * code sees.
 *
 * If no third party transaction manager found, but there are multiple data
 * sources are defined, then this class will provide light weight JTA *like*
 * functionality. Here in case of failure it is totally on the user to manually
 * rollback any changes if any datasources failed to commit during the commit
 * run. This transaction manager is best used when we are dealing with only 1
 * transactional resource.
 */
public final class PlatformTransactionManagerAdapter implements TransactionManager {

    public static class TransactionHolder {
        public final TransactionStatus status;
        public final Transaction transaction;

        TransactionHolder(TransactionStatus status, PlatformTransactionAdapter adapter) {
            this.status = status;
            this.transaction = adapter;
        }
    }

    private List<PlatformTransactionManager> txnManagersForEachDataSource = new ArrayList<>();

    private static DefaultTransactionDefinition NEW_TRANSACTION_DEFINITION = new DefaultTransactionDefinition();
    static {
        NEW_TRANSACTION_DEFINITION.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static DefaultTransactionDefinition EXISTING_TRANSACTION_DEFINITION = new DefaultTransactionDefinition();
    static {
        EXISTING_TRANSACTION_DEFINITION.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }

    private static final ThreadLocal<TransactionHolder> TRANSACTION_HOLDERS = new ThreadLocal<>();

    private static final class PlatformTransactionAdapter implements Transaction {

        private WeakReference<TransactionStatus> transactionStatus;

        PlatformTransactionAdapter(TransactionStatus status) {
            this.transactionStatus = new WeakReference<TransactionStatus>(status);
        }

        @Override
        public void registerSynchronization(final Synchronization synch)
                throws IllegalStateException, RollbackException, SystemException {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void beforeCompletion() {
                    synch.beforeCompletion();
                }

                @Override
                public void afterCompletion(int status) {
                    switch (status) {
                    case TransactionSynchronization.STATUS_COMMITTED:
                        status = Status.STATUS_COMMITTED;
                        break;
                    case TransactionSynchronization.STATUS_ROLLED_BACK:
                        status = Status.STATUS_ROLLEDBACK;
                        break;
                    case TransactionSynchronization.STATUS_UNKNOWN:
                        status = Status.STATUS_UNKNOWN;
                        break;
                    }
                    synch.afterCompletion(status);
                }
            });
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            TransactionStatus status = transactionStatus.get();
            if (status == null) {
                throw new IllegalStateException();
            }
            status.setRollbackOnly();
        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {
            throw new SystemException();
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException,
        SecurityException, SystemException {
            throw new SystemException();
        }

        @Override
        public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
            throw new SystemException();
        }

        @Override
        public boolean enlistResource(XAResource xaRes)
                throws IllegalStateException, RollbackException, SystemException {
            throw new SystemException();
        }

        @Override
        public int getStatus() throws SystemException {
            throw new SystemException();
        }
    }

    private PlatformTransactionManager platformTransactionManager;
    private WeakHashMap<TransactionStatus, PlatformTransactionAdapter> transactions = new WeakHashMap<>();

    public PlatformTransactionManagerAdapter() {

    }

    public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
        if (this.platformTransactionManager instanceof DelegatingPlatformTransactionManager) {
            ((DelegatingPlatformTransactionManager) this.platformTransactionManager)
            .setTransactionManagers(this.txnManagersForEachDataSource);
        }
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        try {
            if (platformTransactionManager == null) {
                return null;
            }
            TransactionHolder holder = getOrCreateTransaction(false);
            if (holder == null) {
                return null;
            }
            return holder.transaction;
        } catch (IllegalTransactionStateException e) {
            return null;
        }
    }

    public TransactionHolder getOrCreateTransaction(boolean start) {
        TransactionStatus status = null;
        try {
            //Spring managed transaction
            status = TransactionAspectSupport.currentTransactionStatus();
        } catch (NoTransactionException e) {
            //Teiid programatically managed transaction
            if (start) {
                status = platformTransactionManager.getTransaction(NEW_TRANSACTION_DEFINITION);
                TransactionHolder holder = new TransactionHolder(status, new PlatformTransactionAdapter(status));
                TRANSACTION_HOLDERS.set(holder);
                return holder;
            } else {
                try {
                    status = platformTransactionManager.getTransaction(EXISTING_TRANSACTION_DEFINITION);
                    //success means that there is one defined/associated, so we are safe to use
                    //the thread local
                    return TRANSACTION_HOLDERS.get();
                } catch (TransactionException e1) {
                    TRANSACTION_HOLDERS.remove();
                }
            }
        }
        if (status == null) {
            return null;
        }
        synchronized (transactions) {
            PlatformTransactionAdapter adapter = transactions.get(status);
            if (adapter == null) {
                adapter = new PlatformTransactionAdapter(status);
                transactions.put(status, adapter);
            }
            return new TransactionHolder(status, adapter);
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        throw new SystemException();
    }

    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
    RollbackException, SecurityException, SystemException {
        throw new SystemException();
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        throw new SystemException();
    }

    @Override
    public Transaction suspend() throws SystemException {
        throw new SystemException();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        throw new SystemException();
    }

    @Override
    public void resume(Transaction tobj) throws IllegalStateException, InvalidTransactionException, SystemException {
        throw new SystemException();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        throw new SystemException();
    }

    @Override
    public int getStatus() throws SystemException {
        throw new SystemException();
    }

    public void addDataSource(DataSource ds) {
        this.txnManagersForEachDataSource.add(new DataSourceTransactionManager(ds));
    }

    @SuppressWarnings("rawtypes")
    public void addDataSource(BaseConnectionFactory bean) {
        // TODO: not used currently, need to come up some strategy here.
    }

    public void commit(TransactionStatus status) {
        this.platformTransactionManager.commit(status);
        TRANSACTION_HOLDERS.remove();
    }

    public void rollback(TransactionStatus status) {
        this.platformTransactionManager.rollback(status);
        TRANSACTION_HOLDERS.remove();
    }

}

