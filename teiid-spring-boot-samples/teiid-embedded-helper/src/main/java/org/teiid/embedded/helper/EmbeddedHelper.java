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
package org.teiid.embedded.helper;

import java.util.function.Consumer;

import javax.resource.ResourceException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.security.AuthenticationManager;

public interface EmbeddedHelper extends IronJacamarHelper, NarayanaHelper, PicketboxHelper {
    
    EmbeddedHelper Factory = new EmbeddedHelperImpl();

    class EmbeddedHelperImpl implements EmbeddedHelper {

        @Override
        public DataSource newNoTxDataSource(Consumer<org.teiid.embedded.helper.ironJacamar.Configuration> consumer) throws ResourceException {
            return IronJacamarHelper.Factory.newNoTxDataSource(consumer);
        }

        @Override
        public DataSource newDataSource(Consumer<org.teiid.embedded.helper.ironJacamar.Configuration> consumer) throws ResourceException {
            return IronJacamarHelper.Factory.newDataSource(consumer);
        }
        
        @Override
        public TransactionManager transactionManager() {
            return NarayanaHelper.Factory.transactionManager();
        }

        @Override
        public TransactionManager transactionManager(Consumer<org.teiid.embedded.helper.narayana.Configuration> consumer) {
            return NarayanaHelper.Factory.transactionManager(consumer);
        }

        @Override
        public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
            return NarayanaHelper.Factory.transactionSynchronizationRegistry();
        }

        @Override
        public AuthenticationManager authenticationManager(String securityDomainName, String configFile) {
            return PicketboxHelper.Factory.authenticationManager(securityDomainName, configFile);
        }
        
        
    }
}
