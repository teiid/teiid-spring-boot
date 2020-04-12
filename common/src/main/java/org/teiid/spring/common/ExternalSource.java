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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

public class ExternalSource {
    public static ExternalSource ATHENA = new ExternalSource("amazon-athena",
            new String[] { "com.simba.athena.jdbc.Driver" }, new String[] {}, "jdbc-ansi", null, null, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource ACTIAN = new ExternalSource("actian", new String[] { "com.ingres.jdbc.IngresDriver" },
            new String[] {}, "actian-vector", "org.hibernate.dialect.Ingres10Dialect",
            new String[] { "com.ingres.jdbc:iijdbc" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource DB2 = new ExternalSource("db2", new String[] { "com.ibm.db2.jcc.DB2Driver" },
            new String[] { "com.ibm.db2.jcc.DB2XADataSource" }, "db2", "org.hibernate.dialect.DB2Dialect",
            new String[] { "com.ibm.db2.jcc:db2jcc4" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource DERBY = new ExternalSource("derby",
            new String[] { "org.apache.derby.jdbc.ClientDriver" }, new String[] {}, "derby",
            "org.hibernate.dialect.DerbyTenSevenDialect", new String[] { "org.apache.derby:derbyclient" },
            SourceType.Jdbc, "spring.datasource");

    public static ExternalSource EXASOL = new ExternalSource("exasol", new String[] { "com.exasol.jdbc.EXADriver" },
            new String[] {}, "exasol", null, new String[] { "com.exasol:exasol-jdbc" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource H2 = new ExternalSource("h2", new String[] { "org.h2.Driver" },
            new String[] { "org.h2.jdbcx.JdbcDataSource" }, "h2", "org.hibernate.dialect.H2Dialect",
            new String[] { "com.h2database:h2" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource HANA = new ExternalSource("hana", new String[] { "com.sap.db.jdbc.Driver" },
            new String[] {}, "hana", "org.hibernate.dialect.HANARowStoreDialect",
            new String[] { "com.sap.cloud.db.jdbc:ngdbc" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource HBASE = new ExternalSource("hbase",
            new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "hbase", null,
            new String[] { "org.apache.phoenix:phoenix-queryserver-client" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource HIVE = new ExternalSource("hive", new String[] { "org.apache.hive.jdbc.HiveDriver" },
            new String[] {}, "hive", null, new String[] { "org.apache.hive:hive-jdbc" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource HSQL = new ExternalSource("hsql", new String[] { "org.hsqldb.jdbc.JDBCDriver" },
            new String[] { "org.hsqldb.jdbc.pool.JDBCXADataSource" }, "hsql", "org.hibernate.dialect.HSQLDialect",
            new String[] { "org.hsqldb:hsqldb" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource IMPALA = new ExternalSource("impala",
            new String[] { "org.apache.hadoop.hive.jdbc.HiveDriver" }, new String[] {}, "impala", null,
            new String[] { "com.cloudera.impala:jdbc" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource INFORMIX = new ExternalSource("informix",
            new String[] { "com.informix.jdbc.IfxDriver" }, new String[] {}, "informix",
            "org.hibernate.dialect.InformixDialect", new String[] { "com.ibm.informix:jdbc" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource INGRES = new ExternalSource("ingres", new String[] { "com.ingres.jdbc.IngresDriver" },
            new String[] {}, "ingres", "org.hibernate.dialect.Ingres10Dialect",
            new String[] { "com.ingres.jdbc:iijdbc" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource JDBCSIMPLE = new ExternalSource("jdbc-simple", new String[] { "java.sql.Driver" },
            new String[] {}, "jdbc-simple", null, null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource JDBCANSI = new ExternalSource("jdbc-ansi", new String[] { "java.sql.Driver" },
            new String[] {}, "jdbc-ansi", null, null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource JTDS = new ExternalSource("jtds", new String[] { "net.sourceforge.jtds.jdbc.Driver" },
            new String[] {}, "sqlserver", "org.hibernate.dialect.SQLServer2012Dialect",
            new String[] { "net.sourceforge.jtds:jtds" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource MYSQL = new ExternalSource("mysql", new String[] { "com.mysql.jdbc.Driver" },
            new String[] { "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource" }, "mysql5",
            "org.hibernate.dialect.MySQL5InnoDBDialect", new String[] { "mysql:mysql-connector-java" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource NETEZZA = new ExternalSource("netezza", new String[] { "org.netezza.Driver " },
            new String[] {}, "netezza", null, null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource ORACLE = new ExternalSource("oracle", new String[] { "oracle.jdbc.OracleDriver" },
            new String[] { "oracle.jdbc.xa.client.OracleXADataSource" }, "oracle",
            "org.hibernate.dialect.Oracle12cDialect", null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource OSISOFTPI = new ExternalSource("osisoft-pi",
            new String[] { "com.osisoft.jdbc.Driver" }, new String[] {}, "osisoft-pi", null, null, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource POSTGRESQL = new ExternalSource("postgresql", new String[] { "org.postgresql.Driver" },
            new String[] { "org.postgresql.xa.PGXADataSource" }, "postgresql",
            "org.hibernate.dialect.PostgreSQL9Dialect", new String[] { "org.postgresql:postgresql" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource PHOENIX = new ExternalSource("phoenix",
            new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "phoenix", null,
            new String[] { "org.apache.phoenix:phoenix-queryserver-client" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource PRESTODB = new ExternalSource("prestodb",
            new String[] { "com.facebook.presto.jdbc.PrestoDriver" }, new String[] {}, "prestodb", null,
            new String[] { "com.facebook.presto:presto-jdbc" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource REDSHIFT = new ExternalSource("redshift",
            new String[] { "com.amazon.redshift.jdbc42.Driver" }, new String[] {}, "redshift", null,
            new String[] { "com.amazon.redshift:redshift-jdbc42" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource SAPIQ = new ExternalSource("spaiq", new String[] { "com.sybase.jdbc4.jdbc.SybDriver" },
            new String[] {}, "sap-iq", null, null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource MSSQLSERVER = new ExternalSource("mssql-server",
            new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
            new String[] { "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" }, "sqlserver",
            "org.hibernate.dialect.SQLServer2012Dialect", new String[] { "com.microsoft.sqlserver:mssql-jdbc" },
            SourceType.Jdbc, "spring.datasource");

    public static ExternalSource SQLSERVER = new ExternalSource("sqlserver",
            new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
            new String[] { "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" }, "sqlserver",
            "org.hibernate.dialect.SQLServer2012Dialect", new String[] { "com.microsoft.sqlserver:mssql-jdbc" },
            SourceType.Jdbc, "spring.datasource");

    public static ExternalSource SYBASE = new ExternalSource("sybase",
            new String[] { "com.sybase.jdbc2.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDriver" }, new String[] {},
            "sybase", "org.hibernate.dialect.SybaseDialect", null, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource TEIID = new ExternalSource("teiid", new String[] { "org.teiid.jdbc.TeiidDriver" },
            new String[] {}, "teiid", "org.teiid.dialect.TeiidDialect", new String[] { "org.teiid:teiid" },
            SourceType.Jdbc, "spring.datasource");

    public static ExternalSource TERADATA = new ExternalSource("teradata",
            new String[] { "com.teradata.jdbc.TeraDriver" }, new String[] {}, "teradata",
            "org.hibernate.dialect.Teradata14Dialect", new String[] { "com.teradata.jdbc:terajdbc4" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource UCANACCESS = new ExternalSource("ucanaccess",
            new String[] { "net.ucanaccess.jdbc.UcanaccessDriver" }, new String[] {}, "ucanaccess", null,
            new String[] { "net.sf.ucanaccess:ucanaccess" }, SourceType.Jdbc, "spring.datasource");

    public static ExternalSource VERTICA = new ExternalSource("vertica", new String[] { "com.vertica.jdbc.Driver" },
            new String[] {}, "vertica", null, new String[] { "org.clojars.erp12:jdbc-vertica" }, SourceType.Jdbc,
            "spring.datasource");

    public static ExternalSource LOOPBACK = new ExternalSource("loopback", new String[] {}, new String[] {}, "loopback",
            null, new String[] { "org.teiid:teiid-spring-boot-starter" }, null, "spring.teiid.data.loopback");


    private static ExternalSource[] STATICSOURCES = new ExternalSource[] { ATHENA, ACTIAN, DB2, DERBY, EXASOL, H2, HANA,
            HBASE, HIVE, HSQL, IMPALA, INFORMIX, INGRES, JDBCSIMPLE, JDBCANSI, JTDS, MYSQL, NETEZZA, ORACLE, OSISOFTPI,
            POSTGRESQL, PHOENIX, PRESTODB, REDSHIFT, SAPIQ, MSSQLSERVER, SQLSERVER, SYBASE, TEIID, TERADATA, UCANACCESS,
            VERTICA, LOOPBACK };

    public static ArrayList<ExternalSource> SOURCES = new ArrayList<>(Arrays.asList(STATICSOURCES));

    private String name;
    private String[] driverNames;
    private String[] datasourceNames;
    private String translatorName;
    private String dialect;
    private String[] gav;
    private SourceType sourceType;
    private String springBootPropertyPrefix;

    public ExternalSource(String name, String[] driverNames, String[] datasourceNames, String translatorName,
            String dialect, String[] gav, SourceType sourceType, String prefix) {
        this.name = name;
        this.driverNames = driverNames;
        this.datasourceNames = datasourceNames;
        this.translatorName = translatorName;
        this.dialect = dialect;
        this.gav = gav;
        this.sourceType = sourceType;
        this.springBootPropertyPrefix = prefix;
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

    public String getSpringBootPropertyPrefix() {
        return springBootPropertyPrefix;
    }

    public static ExternalSource findByDriverName(String driverName) {
        for (ExternalSource source : ExternalSource.SOURCES) {
            for (String driver : source.driverNames) {
                if (driver.equals(driverName)) {
                    return source;
                }
            }
            for (String driver : source.datasourceNames) {
                if (driver.equals(driverName)) {
                    return source;
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

    public static ExternalSource find(String sourceName) {
        for (ExternalSource source : ExternalSource.SOURCES) {
            if (source.name.equalsIgnoreCase(sourceName)) {
                return source;
            }
        }
        return null;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public static synchronized void addSource(ExternalSource source) {
        if (find(source.getName()) == null) {
            SOURCES.add(source);
        }
    }

    public static ExternalSource build(ConnectionFactoryConfiguration annotation, String className) {
        String dialect = annotation.dialect();
        ExternalSource source = new ExternalSource(annotation.alias(), new String[] { className },
                new String[] {}, annotation.translatorName(), dialect.isEmpty() ? null : dialect,
                        annotation.dependencies(), annotation.sourceType(), annotation.propertyPrefix());
        return source;
    }
}
