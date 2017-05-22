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

package org.teiid.embedded.helper.ironJacamar;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

import javax.resource.ResourceException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.jca.adapters.jdbc.JDBCResourceAdapter;
import org.jboss.jca.adapters.jdbc.local.LocalManagedConnectionFactory;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.notx.NoTxConnectionManagerImpl;
import org.jboss.jca.core.connectionmanager.pool.mcp.LeakDumperManagedConnectionPool;
import org.jboss.jca.core.connectionmanager.pool.strategy.OnePool;
import org.jboss.jca.core.connectionmanager.tx.TxConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.usertx.UserTransactionRegistry;
import org.jboss.jca.core.spi.transaction.xa.XATerminator;
import org.jboss.jca.core.tx.noopts.TransactionIntegrationImpl;
import org.jboss.jca.core.tx.noopts.UserTransactionRegistryImpl;
import org.jboss.jca.core.tx.noopts.XATerminatorImpl;
import org.teiid.embedded.helper.IronJacamarHelper;
import org.teiid.embedded.helper.NarayanaHelper;

public class IronJacamarHelperImpl implements IronJacamarHelper {

    @Override
    public DataSource newNoTxDataSource(Consumer<Configuration> consumer) throws ResourceException {
        
        Objects.requireNonNull(consumer);
        Configuration config = new Configuration();
        consumer.accept(config);
        
        Objects.requireNonNull(config.localManagedConnectionFactory());
        LocalManagedConnectionFactory mcf = config.localManagedConnectionFactory();
        mcf.setResourceAdapter(new JDBCResourceAdapter());
        
        NoTxConnectionManagerImpl cm = new NoTxConnectionManagerImpl();
        String mcp = LeakDumperManagedConnectionPool.class.getName();
        PoolConfiguration poolConfig = config.poolConfiguration();
        if(poolConfig == null) {
            poolConfig = new PoolConfiguration();
        }
        OnePool pool = new OnePool(mcf, poolConfig, false, true, mcp);
        pool.setConnectionManager(cm);
        cm.setPool(pool);
        
        return (DataSource) mcf.createConnectionFactory(cm);
    }

    @Override
    public DataSource newDataSource(Consumer<Configuration> consumer) throws ResourceException {
        
        Objects.requireNonNull(consumer);
        Configuration config = new Configuration();
        consumer.accept(config);
        
        Objects.requireNonNull(config.localManagedConnectionFactory());
        LocalManagedConnectionFactory mcf = config.localManagedConnectionFactory();
        mcf.setResourceAdapter(new JDBCResourceAdapter());
        
        TransactionManager tm = NarayanaHelper.Factory.transactionManager(c -> c.coreEnvironmentBean(core -> {
            core.setSocketProcessIdPort(0);
            core.setSocketProcessIdMaxPorts(2);
        }).coordinatorEnvironmentBean(coordinator -> {
            coordinator.setEnableStatistics(false);
            coordinator.setDefaultTimeout(300);
            coordinator.setTransactionStatusManagerEnable(false);
            coordinator.setTxReaperCancelFailWaitPeriod(120000);
        }).objectStoreEnvironmentBean(objectStore -> {
            objectStore.setObjectStoreDir(System.getProperty("java.io.tmpdir")  + File.separator + "narayana");
        }));
        TransactionSynchronizationRegistry tsr = NarayanaHelper.Factory.transactionSynchronizationRegistry();
        UserTransactionRegistry utr = new UserTransactionRegistryImpl();
        XATerminator terminator = new XATerminatorImpl();
        TransactionIntegration txIntegration = new TransactionIntegrationImpl(tm, tsr, utr, terminator, null);
        
        TxConnectionManagerImpl cm = new TxConnectionManagerImpl(txIntegration, true);
        String mcp = LeakDumperManagedConnectionPool.class.getName();
        PoolConfiguration poolConfig = config.poolConfiguration();
        if(poolConfig == null) {
            poolConfig = new PoolConfiguration();
        }
        OnePool pool = new OnePool(mcf, poolConfig, false, true, mcp);
        pool.setConnectionManager(cm);
        cm.setPool(pool);
        
        return (DataSource) mcf.createConnectionFactory(cm);
    }

}
