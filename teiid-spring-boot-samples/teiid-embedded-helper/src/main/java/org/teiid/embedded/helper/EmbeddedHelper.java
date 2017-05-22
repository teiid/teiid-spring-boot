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
package org.teiid.embedded.helper;

import java.util.function.Consumer;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.security.AuthenticationManager;
import org.teiid.resource.adapter.accumulo.AccumuloManagedConnectionFactory;
import org.teiid.resource.adapter.cassandra.CassandraManagedConnectionFactory;
import org.teiid.resource.adapter.couchbase.CouchbaseManagedConnectionFactory;
import org.teiid.resource.adapter.file.FileManagedConnectionFactory;
import org.teiid.resource.adapter.ftp.FtpManagedConnectionFactory;
import org.teiid.resource.adapter.google.SpreadsheetManagedConnectionFactory;
import org.teiid.resource.adapter.infinispan.hotrod.InfinispanManagedConnectionFactory;
import org.teiid.resource.adapter.ldap.LDAPManagedConnectionFactory;
import org.teiid.resource.adapter.mongodb.MongoDBManagedConnectionFactory;
import org.teiid.resource.adapter.salesforce.SalesForceManagedConnectionFactory;
import org.teiid.resource.adapter.simpledb.SimpleDBManagedConnectionFactory;
import org.teiid.resource.adapter.solr.SolrManagedConnectionFactory;
import org.teiid.resource.adapter.ws.WSManagedConnectionFactory;

public interface EmbeddedHelper extends TeiidConnectorHelper, IronJacamarHelper, NarayanaHelper, PicketboxHelper {
    
    EmbeddedHelper Factory = new EmbeddedHelperImpl();

    class EmbeddedHelperImpl implements EmbeddedHelper {
        
        @Override
        public ConnectionFactory accumuloConnectionFactory(Consumer<AccumuloManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.accumuloConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory cassandraConnectionFactory(Consumer<CassandraManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.cassandraConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory couchbaseConnectionFactory(Consumer<CouchbaseManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.couchbaseConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory fileConnectionFactory(Consumer<FileManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.fileConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory ftpConnectionFactory(Consumer<FtpManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.ftpConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory googleConnectionFactory(Consumer<SpreadsheetManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.googleConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory infinispanConnectionFactory(Consumer<InfinispanManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.infinispanConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory ldapConnectionFactory(Consumer<LDAPManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.ldapConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory mongodbConnectionFactory(Consumer<MongoDBManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.mongodbConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory salesforceConnectionFactory(Consumer<SalesForceManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.salesforceConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory simpledbConnectionFactory(Consumer<SimpleDBManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.simpledbConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory solrConnectionFactory(Consumer<SolrManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.solrConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer) throws ResourceException {
            return TeiidConnectorHelper.Factory.wsConnectionFactory(consumer);
        }

        @Override
        public ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer, ConnectionManager cm) throws ResourceException {
            return TeiidConnectorHelper.Factory.wsConnectionFactory(consumer, cm);
        }

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
