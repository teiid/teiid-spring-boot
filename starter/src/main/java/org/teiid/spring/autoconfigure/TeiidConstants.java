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

public interface TeiidConstants {

    String CTX_EMBEDDED = "org.teiid.EMBEDDED";
    
    String DEFAULT_ADDRESS = "0.0.0.0";
    Integer DEFAULT_PORT = 31000;
    
    String FILTER_PACKAGE_TRANSLATOR = "org.teiid.translator";
    
    String VDBNAME = "spring";
    String VDBVERSION = "1.0.0";
    
    //Translators
    String access = "access";
    String accumulo = "accumulo";
    String actian_vector  = "actian-vector";
    String cassandra = "cassandra";
    String couchbase = "couchbase";
    String db2 = "db2";
    String derby = "derby";
    String excel = "excel";
    String file = "file";
    String google_spreadsheet = "google-spreadsheet";
    
    String greenplum = "greenplum";
    String h2 = "h2";
    String hana = "hana";
    String hbase = "hbase";
    String hive = "hive";
    String hsql = "hsql";
    String impala = "impala";
    String infinispan_hotrod = "infinispan-hotrod";
    String informix = "informix";
    String ingres = "ingres";
    
    String ingres93 = "ingres93";
    String intersystems_cache = "intersystems-cache";
    String jdbc_ansi  = "jdbc-ansi";
    String jdbc_simple = "jdbc-simple";
    String jpa2 = "jpa2";
    String ldap = "ldap";
    String loopback = "loopback";
    String metamatrix = "metamatrix";
    String modeshape = "modeshape";
    String mongodb = "mongodb";
    
    String mysql = "mysql";
    String mysql5 = "mysql5";
    String netezza = "netezza";
    String odata = "odata";
    String odata4 = "odata4";
    String olap = "olap";
    String oracle = "oracle";
    String osisoft_pi = "osisoft-pi";
    String phoenix = "phoenix";
    String postgresql = "postgresql";
    
    String prestodb = "prestodb";
    String redshift = "redshift";
    String salesforce = "salesforce";
    String salesforce_34 = "salesforce-34";
    String sap_gateway = "sap-gateway";
    String sap_nw_gateway = "sap-nw-gateway";
    String simpledb = "simpledb";
    String solr = "solr";
    String sqlserver = "sqlserver";
    String swagger = "swagger";
    
    String sybase = "sybase";
    String sybaseiq = "sybaseiq";
    String teiid = "teiid";
    String teradata = "teradata";
    String ucanaccess = "ucanaccess";
    String vertica = "vertica";
    String ws = "ws";
    String delegator = "delegator";
    
    // translators auto-detect class
    String[] actian_vector_classes = {"com.ingres.jdbc.IngresDriver"};
    String[] db2_classes = {"com.ibm.db2.jcc.DB2Driver"};
    String[] derby_classes = {"org.apache.derby.jdbc.ClientDriver"};
    String[] h2_classes = {"org.h2.Driver"};
    String[] hana_classes = {"com.sap.db.jdbc.Driver"};
    String[] hive_1_classes = {"org.apache.hadoop.hive.jdbc.HiveDriver"};
    String[] hive_classes = {"org.apache.hive.jdbc.HiveDriver"};
    String[] hsql_classes = {"org.hsqldb.jdbc.JDBCDriver"};
    String[] informix_classes = {"com.informix.jdbc.IfxDriver"};
    String[] ingres_classes = {"com.ingres.jdbc.IngresDriver"};
    String[] intersystems_cache_classes = {"com.intersys.jdbc.CacheDriver"};
    String[] mysql_classes = {"com.mysql.jdbc.Driver"};
    String[] olap_classes_xmla = {"org.olap4j.driver.xmla.XmlaOlap4jDriver"};
    String[] olap_classes_mondrian = {"mondrian.olap4j.MondrianOlap4jDriver"};
    
    String[] oracle_classes = {"oracle.jdbc.OracleDriver"};
    String[] osisoft_pi_classes = {"com.osisoft.jdbc.Driver"};
    String[] phoenix_classes = {"org.apache.phoenix.jdbc.PhoenixDriver"};
    String[] postgresql_classes = {"org.postgresql.Driver"};
    String[] prestodb_classes = {"com.facebook.presto.jdbc.PrestoDriver"};
    String[] sqlserver_classes_jtds = {"net.sourceforge.jtds.jdbc.Driver"};
    String[] sqlserver_classes_native = {"com.microsoft.sqlserver.jdbc.SQLServerDriver"};
    String[] sybase_classes = {"com.sybase.jdbc2.jdbc.SybDriver"};
    String[] sybase_classes_4 = {"com.sybase.jdbc4.jdbc.SybDriver"};
    String[] teiid_classes = {"org.teiid.jdbc.TeiidDriver", "org.teiid.core.types.JDBCSQLTypeInfo"};
    String[] ucanaccess_classes = {"net.ucanaccess.jdbc.UcanaccessDriver"};
    String[] vertica_classes = {"com.vertica.jdbc.Driver"};
    
    String[] accumulo_classes = {"org.teiid.resource.adapter.accumulo.AccumuloManagedConnectionFactory"};
    String[] cassandra_classes = {"org.teiid.resource.adapter.cassandra.CassandraManagedConnectionFactory"};
    String[] couchbase_classes = {"org.teiid.resource.adapter.couchbase.CouchbaseManagedConnectionFactory"};
    String[] file_classes = {"org.teiid.resource.adapter.file.FileManagedConnectionFactory"};
    String[] ftp_classes = {"org.teiid.resource.adapter.ftp.FtpManagedConnectionFactory"};
    String[] google_classes = {"org.teiid.resource.adapter.google.SpreadsheetManagedConnectionFactory"};
    String[] infinispan_classes = {"org.teiid.resource.adapter.infinispan.hotrod.InfinispanManagedConnectionFactory"};
    String[] ldap_classes = {"org.teiid.resource.adapter.ldap.LDAPManagedConnectionFactory"};
    String[] mongodb_classes = {"org.teiid.resource.adapter.mongodb.MongoDBManagedConnectionFactory"};
    String[] salesforce_classes = {"org.teiid.resource.adapter.salesforce.SalesForceManagedConnectionFactory"};
    
    String[] salesforce_34_classes = {"org.teiid.resource.adapter.salesforce.SalesForceManagedConnectionFactory", "org.teiid.resource.adapter.salesforce.transport.SalesforceCXFTransport"};
    String[] simpledb_classes = {"org.teiid.resource.adapter.simpledb.SimpleDBManagedConnectionFactory"};
    String[] solr_classes = {"org.teiid.resource.adapter.solr.SolrManagedConnectionFactory"};
    String[] webservice_classes = {"org.teiid.resource.adapter.ws.WSManagedConnectionFactory"};
    
}
