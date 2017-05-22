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

import java.util.Objects;
import java.util.function.Consumer;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;

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

public interface TeiidConnectorHelper {

    ConnectionFactory accumuloConnectionFactory(Consumer<AccumuloManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory cassandraConnectionFactory(Consumer<CassandraManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory couchbaseConnectionFactory(Consumer<CouchbaseManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory fileConnectionFactory(Consumer<FileManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory ftpConnectionFactory(Consumer<FtpManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory googleConnectionFactory(Consumer<SpreadsheetManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory infinispanConnectionFactory(Consumer<InfinispanManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory ldapConnectionFactory(Consumer<LDAPManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory mongodbConnectionFactory(Consumer<MongoDBManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory salesforceConnectionFactory(Consumer<SalesForceManagedConnectionFactory> consumer) throws ResourceException;
        
    ConnectionFactory simpledbConnectionFactory(Consumer<SimpleDBManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory solrConnectionFactory(Consumer<SolrManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer) throws ResourceException;
    
    ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer, ConnectionManager cm) throws ResourceException;
    
    TeiidConnectorHelper Factory = new TeiidConnectorHelperImpl();
    
    
    class TeiidConnectorHelperImpl implements TeiidConnectorHelper {

        @Override
        public ConnectionFactory accumuloConnectionFactory(Consumer<AccumuloManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            AccumuloManagedConnectionFactory mcf = new AccumuloManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory cassandraConnectionFactory(Consumer<CassandraManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            CassandraManagedConnectionFactory mcf = new CassandraManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory couchbaseConnectionFactory(Consumer<CouchbaseManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            CouchbaseManagedConnectionFactory mcf = new CouchbaseManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory fileConnectionFactory(Consumer<FileManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            FileManagedConnectionFactory mcf = new FileManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory ftpConnectionFactory(Consumer<FtpManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            FtpManagedConnectionFactory mcf = new FtpManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory googleConnectionFactory(Consumer<SpreadsheetManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            SpreadsheetManagedConnectionFactory mcf = new SpreadsheetManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory infinispanConnectionFactory(Consumer<InfinispanManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            InfinispanManagedConnectionFactory mcf = new InfinispanManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory ldapConnectionFactory(Consumer<LDAPManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            LDAPManagedConnectionFactory mcf = new LDAPManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory mongodbConnectionFactory(Consumer<MongoDBManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            MongoDBManagedConnectionFactory mcf = new MongoDBManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory salesforceConnectionFactory(Consumer<SalesForceManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            SalesForceManagedConnectionFactory mcf = new SalesForceManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory simpledbConnectionFactory(Consumer<SimpleDBManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            SimpleDBManagedConnectionFactory mcf = new SimpleDBManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory solrConnectionFactory(Consumer<SolrManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            SolrManagedConnectionFactory mcf = new SolrManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }

        @Override
        public ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer) throws ResourceException {
            Objects.requireNonNull(consumer);
            WSManagedConnectionFactory mcf = new WSManagedConnectionFactory();
            consumer.accept(mcf);
            return mcf.createConnectionFactory();
        }
        
        @Override
        public ConnectionFactory wsConnectionFactory(Consumer<WSManagedConnectionFactory> consumer, ConnectionManager cm) throws ResourceException {
            Objects.requireNonNull(consumer);
            WSManagedConnectionFactory mcf = new WSManagedConnectionFactory();
            consumer.accept(mcf);
            return (ConnectionFactory)mcf.createConnectionFactory(cm);
        }
        
    }
}
