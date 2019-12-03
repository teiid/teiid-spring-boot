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
package org.teiid.spring.common;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

public enum ExternalSource {
    ACTIAN("Actian", new String[] { "com.ingres.jdbc.IngresDriver" }, new String[] {}, "actian-vector",
            "org.hibernate.dialect.Ingres10Dialect", new String[] {"com.ingres.jdbc:iijdbc"}),
    DB2("DB2", new String[] { "com.ibm.db2.jcc.DB2Driver" }, new String[] { "com.ibm.db2.jcc.DB2XADataSource" }, "db2",
            "org.hibernate.dialect.DB2Dialect", new String[] {"com.ibm.db2.jcc:db2jcc4"}),
    DERBY("Derby", new String[] { "org.apache.derby.jdbc.ClientDriver" }, new String[] {}, "derby",
            "org.hibernate.dialect.DerbyTenSevenDialect", new String[] {"org.apache.derby:derbyclient"}),
    EXASOL("Exasol", new String[] { "com.exasol.jdbc.EXADriver" }, new String[] {}, "exasol",
            null, new String[] {"com.exasol:exasol-jdbc"}),
    H2("H2", new String[] { "org.h2.Driver" }, new String[] { "org.h2.jdbcx.JdbcDataSource" }, "h2",
            "org.hibernate.dialect.H2Dialect", new String[] {"com.h2database:h2"}),
    HANA("Hana", new String[] { "com.sap.db.jdbc.Driver" }, new String[] {}, "hana",
            "org.hibernate.dialect.HANARowStoreDialect", new String[] {"com.sap.cloud.db.jdbc:ngdbc"}),
    HBASE("HBase", new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "hbase", null,
            new String[] {"org.apache.phoenix:phoenix-queryserver-client"}),
    HIVE("Hive", new String[] { "org.apache.hive.jdbc.HiveDriver" }, new String[] {}, "hive", null,
            new String[] {"org.apache.hive:hive-jdbc"}),
    HSQL("HSQL", new String[] { "org.hsqldb.jdbc.JDBCDriver" },
            new String[] { "org.hsqldb.jdbc.pool.JDBCXADataSource" }, "hsql", "org.hibernate.dialect.HSQLDialect",
            new String[] {"org.hsqldb:hsqldb"}),
    IMPALA("Impala", new String[] { "org.apache.hadoop.hive.jdbc.HiveDriver" }, new String[] {}, "impala", null,
            new String[] {"com.cloudera.impala:jdbc"}),
    INFORMIX("Informix", new String[] { "com.informix.jdbc.IfxDriver" }, new String[] {}, "informix",
            "org.hibernate.dialect.InformixDialect", new String[] {"com.ibm.informix:jdbc"}),
    INGRES("Ingres", new String[] { "com.ingres.jdbc.IngresDriver" }, new String[] {}, "ingres",
            "org.hibernate.dialect.Ingres10Dialect", new String[] {"com.ingres.jdbc:iijdbc"}),
    JTDS("MS-SQL Server", new String[] { "net.sourceforge.jtds.jdbc.Driver" }, new String[] {}, "sqlserver",
            "org.hibernate.dialect.SQLServer2012Dialect", new String[] {"net.sourceforge.jtds:jtds"}),
    MYSQL("MySQL", new String[] { "com.mysql.jdbc.Driver" },
            new String[] { "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource" }, "mysql5",
            "org.hibernate.dialect.MySQL5InnoDBDialect", new String[] {"mysql:mysql-connector-java"}),
    NETEZZA("Netezza", new String[] { "org.netezza.Driver " }, new String[] {}, "netezza", null, null),
    ORACLE("Oracle", new String[] { "oracle.jdbc.OracleDriver" },
            new String[] { "oracle.jdbc.xa.client.OracleXADataSource" }, "oracle",
            "org.hibernate.dialect.Oracle12cDialect", null),
    OSISOFTPI("OSISOFT PI", new String[] { "com.osisoft.jdbc.Driver" }, new String[] {}, "osisoft-pi", null, null),
    POSTGRESQL("PostgreSQL", new String[] { "org.postgresql.Driver" },
            new String[] { "org.postgresql.xa.PGXADataSource" }, "postgresql",
            "org.hibernate.dialect.PostgreSQL9Dialect", new String[] {"org.postgresql:postgresql"}),
    PHOENIX("Phoenix", new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "phoenix", null,
            new String[] {"org.apache.phoenix:phoenix-queryserver-client"}),
    PRESTODB("PrestoDB", new String[] { "com.facebook.presto.jdbc.PrestoDriver" }, new String[] {}, "prestodb", null,
            new String[] {"com.facebook.presto:presto-jdbc"}),
    REDSHIFT("RedShift", new String[] { "com.amazon.redshift.jdbc42.Driver" },
            new String[] {}, "redshift", null, new String[] {"com.amazon.redshift:redshift-jdbc42"}),
    SAPIQ("SapIQ", new String[] { "com.sybase.jdbc4.jdbc.SybDriver" },new String[] {}, "sap-iq", null, null),
    SQLSERVER("MS-SQL Server", new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
            new String[] { "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" }, "sqlserver",
            "org.hibernate.dialect.SQLServer2012Dialect", new String[] {"com.microsoft.sqlserver:mssql-jdbc"}),
    SYBASE("Sybase", new String[] { "com.sybase.jdbc2.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDriver" },
            new String[] {}, "sybase", "org.hibernate.dialect.SybaseDialect", null),
    TEIID("Teiid", new String[] { "org.teiid.jdbc.TeiidDriver" }, new String[] {}, "teiid",
            "org.teiid.dialect.TeiidDialect", new String[] {"org.teiid:teiid"}),
    TERADATA("Teradata", new String[] { "com.teradata.jdbc.TeraDriver" }, new String[] {}, "teradata",
            "org.hibernate.dialect.Teradata14Dialect", new String[] {"com.teradata.jdbc:terajdbc4"}),
    UCANACCESS("UCanAccess", new String[] { "net.ucanaccess.jdbc.UcanaccessDriver" }, new String[] {}, "ucanaccess", null,
            new String[] {"net.sf.ucanaccess:ucanaccess"}),
    VERTICA("Vertica", new String[] { "com.vertica.jdbc.Driver" }, new String[] {}, "vertica", null,
            new String[] {"org.clojars.erp12:jdbc-vertica"}),

    EXCEL("excel", new String[] { "org.teiid.spring.data.excel.ExcelConnectionFactory" }, new String[] {}, "excel",
            null, new String[] {"org.teiid:spring-data-excel"}),
    FILE("file", new String[] { "org.teiid.spring.data.fileFileConnectionFactory" }, new String[] {}, "file", null,
            new String[] {"org.teiid:teiid-spring-boot-starter"}),
    GOOGLESHEETS("google-spreadsheet", new String[] { "org.teiid.spring.data.google.SpreadsheetConnectionFactory" },
            new String[] {}, "google-spreadsheet", null, new String[] {"org.teiid:spring-data-google"}),
    INFINISPAN("infinispan-hotrod", new String[] { "org.teiid.spring.data.infinispan.InfinispanConnectionFactory" },
            new String[] {}, "infinispan-hotrod", null, new String[] {"org.teiid:spring-data-infinispan"}),
    LOOPBACK("loopback", new String[] {}, new String[] {}, "loopback", null, new String[] {"org.teiid:teiid-spring-boot-starter"}),
    MONGODB("mongodb", new String[] { "org.teiid.spring.data.mongodb.MongoDBConnectionFactory" }, new String[] {},
            "mongodb", null, new String[] {"org.teiid:spring-data-mongodb"}),
    ODATA("odata", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {}, "odata", null,
            new String[] {"org.teiid:spring-data-rest", "org.teiid.connectors:translator-odata"}),
    ODATA4("odata4", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {}, "odata4",
            null, new String[] {"org.teiid:spring-data-rest", "org.teiid.connectors:translator-odata4"}),
    OPENAPI("openapi", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {}, "openapi",
            null, new String[] {"org.teiid:spring-data-rest", "org.teiid.connectors:translator-swagger"}),
    REST("rest", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {}, "rest", null,
            new String[] {"org.teiid:spring-data-rest"}),
    SALESFORCE("salesforce", new String[] { "org.teiid.spring.data.salesforce.SalesforceConnectionFactory" },
            new String[] {}, "salesforce", null, new String[] {"org.teiid:spring-data-salesforce"}),
    SAP_GATEWAY("sap-gateway", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {}, "sap-gateway", null,
            new String[] {"org.teiid:spring-data-rest", "org.teiid.connectors:translator-odata"});

    private String name;
    private String[] driverNames;
    private String[] datasourceNames;
    private String translatorName;
    private String dialect;
    private String[] gav;

    ExternalSource(String name, String[] driverNames, String[] datasourceNames, String translatorName,
            String dialect, String[] gav) {
        this.name = name;
        this.driverNames = driverNames;
        this.datasourceNames = datasourceNames;
        this.translatorName = translatorName;
        this.dialect = dialect;
        this.gav = gav;
    }

    public String getName() {
        return name;
    }

    public String[] getDriverNames() {
        return driverNames;
    }

    public String getTranslatorName() {
        return translatorName;
    }

    public String getDialect() {
        return dialect;
    }

    public String[] getGav() {
        return gav;
    }

    public static String findTransaltorNameFromDriverName(String driverName) {
        for (ExternalSource source : ExternalSource.values()) {
            for (String driver : source.driverNames) {
                if (driver.equals(driverName)) {
                    return source.getTranslatorName();
                }
            }
            for (String driver : source.datasourceNames) {
                if (driver.equals(driverName)) {
                    return source.getTranslatorName();
                }
            }
        }
        return "loopback";
    }

    public static String findDialectFromDriverName(String driverName) {
        for (ExternalSource source : ExternalSource.values()) {
            for (String driver : source.driverNames) {
                if (driver.equals(driverName)) {
                    return source.getDialect();
                }
            }
            for (String driver : source.datasourceNames) {
                if (driver.equals(driverName)) {
                    return source.getDialect();
                }
            }
        }
        return null;
    }

    public static Class<? extends ExecutionFactory<?, ?>> translatorClass(String translatorName, String basePackage) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Translator.class));
        Class<? extends ExecutionFactory<?, ?>> clazz = findTranslatorInPackage(translatorName, provider,
                "org.teiid.translator");
        if (clazz == null) {
            clazz = findTranslatorInPackage(translatorName, provider,basePackage);
        }
        return clazz;
    }

    static Class<? extends ExecutionFactory<?, ?>> findTranslatorInPackage(String translatorName,
            ClassPathScanningCandidateComponentProvider provider, String packageName) {
        Set<BeanDefinition> components = provider.findCandidateComponents(packageName);
        for (BeanDefinition c : components) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends ExecutionFactory<?, ?>> clazz = (Class<? extends ExecutionFactory<?, ?>>) Class
                .forName(c.getBeanClassName());
                String name = clazz.getAnnotation(Translator.class).name();
                if (name.equals(translatorName)) {
                    return clazz;
                }
            } catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {
                throw new IllegalStateException("Error loading translators", e);
            }
        }
        return null;
    }

    public static String findTransaltorNameFromAlias(String sourceName) {
        for (ExternalSource source : ExternalSource.values()) {
            if (source.name.equalsIgnoreCase(sourceName)) {
                return source.getTranslatorName();
            }
        }
        return null;
    }

    public static ExternalSource find(String sourceName) {
        for (ExternalSource source : ExternalSource.values()) {
            if (source.name.equalsIgnoreCase(sourceName)) {
                return source;
            }
        }
        return null;
    }

    public static String[] findDriverNameFromAlias(String sourceName) {
        for (ExternalSource source : ExternalSource.values()) {
            if (source.name.equalsIgnoreCase(sourceName)) {
                return source.getDriverNames();
            }
        }
        return null;
    }
}
