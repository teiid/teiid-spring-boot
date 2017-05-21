/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
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
