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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

public class ExternalSources implements Serializable{
    private static final long serialVersionUID = 4872582926073134433L;
    private Map<String, ExternalSource> items = Collections.synchronizedMap(new TreeMap<String, ExternalSource>());

    public ExternalSources() {
        items.put("amazon-athena", new ExternalSource("amazon-athena", new String[] { "com.simba.athena.jdbc.Driver" },
                new String[] {}, "jdbc-ansi", null, null, "spring.datasource", true));

        items.put("actian", new ExternalSource("actian", new String[] { "com.ingres.jdbc.IngresDriver" },
                new String[] {}, "actian-vector", "org.hibernate.dialect.Ingres10Dialect",
                new String[] { "com.ingres.jdbc:iijdbc" },  "spring.datasource", true));

        items.put("db2",  new ExternalSource("db2", new String[] { "com.ibm.db2.jcc.DB2Driver" },
                new String[] { "com.ibm.db2.jcc.DB2XADataSource" }, "db2", "org.hibernate.dialect.DB2Dialect",
                null,  "spring.datasource", true));

        items.put("derby",  new ExternalSource("derby",
                new String[] { "org.apache.derby.jdbc.ClientDriver" }, new String[] {}, "derby",
                "org.hibernate.dialect.DerbyTenSevenDialect", new String[] { "org.apache.derby:derbyclient" },
                "spring.datasource", true));

        items.put("exasol",  new ExternalSource("exasol", new String[] { "com.exasol.jdbc.EXADriver" },
                new String[] {}, "exasol", null, new String[] { "com.exasol:exasol-jdbc" },
                "spring.datasource", true));

        items.put("h2", new ExternalSource("h2", new String[] { "org.h2.Driver" },
                new String[] { "org.h2.jdbcx.JdbcDataSource" }, "h2", "org.hibernate.dialect.H2Dialect",
                new String[] { "com.h2database:h2" },  "spring.datasource", true));

        items.put("hana",  new ExternalSource("hana", new String[] { "com.sap.db.jdbc.Driver" },
                new String[] {}, "hana", "org.hibernate.dialect.HANARowStoreDialect",
                new String[] { "com.sap.cloud.db.jdbc:ngdbc" },  "spring.datasource", true));

        items.put("hbase",  new ExternalSource("hbase",
                new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "hbase", null,
                new String[] { "org.apache.phoenix:phoenix-queryserver-client" },  "spring.datasource", true));

        items.put("hive", new ExternalSource("hive", new String[] { "org.apache.hive.jdbc.HiveDriver" },
                new String[] {}, "hive", null, new String[] { "org.apache.hive:hive-jdbc" },
                "spring.datasource", true));

        items.put("hsql", new ExternalSource("hsql", new String[] { "org.hsqldb.jdbc.JDBCDriver" },
                new String[] { "org.hsqldb.jdbc.pool.JDBCXADataSource" }, "hsql", "org.hibernate.dialect.HSQLDialect",
                new String[] { "org.hsqldb:hsqldb" },  "spring.datasource", true));

        items.put("impala", new ExternalSource("impala",
                new String[] { "org.apache.hadoop.hive.jdbc.HiveDriver" }, new String[] {}, "impala", null,
                new String[] { "com.cloudera.impala:jdbc" },  "spring.datasource", true));

        items.put("informix", new ExternalSource("informix",
                new String[] { "com.informix.jdbc.IfxDriver" }, new String[] {}, "informix",
                "org.hibernate.dialect.InformixDialect", new String[] { "com.ibm.informix:jdbc" },
                "spring.datasource", true));

        items.put("ingres", new ExternalSource("ingres", new String[] { "com.ingres.jdbc.IngresDriver" },
                new String[] {}, "ingres", "org.hibernate.dialect.Ingres10Dialect",
                new String[] { "com.ingres.jdbc:iijdbc" },  "spring.datasource", true));

        items.put("jdbc-simple", new ExternalSource("jdbc-simple", new String[] { "java.sql.Driver" },
                new String[] {}, "jdbc-simple", null, null,  "spring.datasource", true));

        items.put("jdbc-ansi", new ExternalSource("jdbc-ansi", new String[] { "java.sql.Driver" },
                new String[] {}, "jdbc-ansi", null, null,  "spring.datasource", true));

        items.put("jtds", new ExternalSource("jtds", new String[] { "net.sourceforge.jtds.jdbc.Driver" },
                new String[] {}, "sqlserver", "org.hibernate.dialect.SQLServer2012Dialect",
                new String[] { "net.sourceforge.jtds:jtds" },  "spring.datasource", true));

        items.put("mysql", new ExternalSource("mysql", new String[] { "com.mysql.jdbc.Driver" },
                new String[] { "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource" }, "mysql5",
                "org.hibernate.dialect.MySQL5InnoDBDialect", new String[] { "mysql:mysql-connector-java" },
                "spring.datasource", true));

        items.put("netezza", new ExternalSource("netezza", new String[] { "org.netezza.Driver " },
                new String[] {}, "netezza", null, null,  "spring.datasource", true));

        items.put("oracle", new ExternalSource("oracle", new String[] { "oracle.jdbc.OracleDriver" },
                new String[] { "oracle.jdbc.xa.client.OracleXADataSource" }, "oracle",
                "org.hibernate.dialect.Oracle12cDialect", null,  "spring.datasource", true));

        items.put("osisoft-pi", new ExternalSource("osisoft-pi",
                new String[] { "com.osisoft.jdbc.Driver" }, new String[] {}, "osisoft-pi", null, null,
                "spring.datasource", true));

        items.put("postgresql", new ExternalSource("postgresql", new String[] { "org.postgresql.Driver" },
                new String[] { "org.postgresql.xa.PGXADataSource" }, "postgresql",
                "org.hibernate.dialect.PostgreSQL9Dialect", new String[] { "org.postgresql:postgresql" },
                "spring.datasource", true));

        items.put("phoenix", new ExternalSource("phoenix",
                new String[] { "org.apache.phoenix.jdbc.PhoenixDriver" }, new String[] {}, "phoenix", null,
                new String[] { "org.apache.phoenix:phoenix-queryserver-client" },  "spring.datasource", true));

        items.put("prestodb", new ExternalSource("prestodb",
                new String[] { "com.facebook.presto.jdbc.PrestoDriver" }, new String[] {}, "prestodb", null,
                new String[] { "com.facebook.presto:presto-jdbc" },  "spring.datasource", true));

        items.put("redshift",  new ExternalSource("redshift",
                new String[] { "com.amazon.redshift.jdbc42.Driver" }, new String[] {}, "redshift", null,
                new String[] { "com.amazon.redshift:redshift-jdbc42" },  "spring.datasource", true));

        items.put("spaiq",  new ExternalSource("spaiq", new String[] { "com.sybase.jdbc4.jdbc.SybDriver" },
                new String[] {}, "sap-iq", null, null,  "spring.datasource", true));

        items.put("mssql-server",  new ExternalSource("mssql-server",
                new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
                new String[] { "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" }, "sqlserver",
                "org.hibernate.dialect.SQLServer2012Dialect", new String[] { "com.microsoft.sqlserver:mssql-jdbc" },
                "spring.datasource", true));

        items.put("sqlserver",  new ExternalSource("sqlserver",
                new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
                new String[] { "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" }, "sqlserver",
                "org.hibernate.dialect.SQLServer2012Dialect", new String[] { "com.microsoft.sqlserver:mssql-jdbc" },
                "spring.datasource", true));

        items.put("sybase",  new ExternalSource("sybase",
                new String[] { "com.sybase.jdbc2.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDriver" }, new String[] {},
                "sybase", "org.hibernate.dialect.SybaseDialect", null,  "spring.datasource", true));

        items.put("teiid",  new ExternalSource("teiid", new String[] { "org.teiid.jdbc.TeiidDriver" },
                new String[] {}, "teiid", "org.teiid.dialect.TeiidDialect", null,
                "spring.datasource", true));

        items.put("teradata",  new ExternalSource("teradata",
                new String[] { "com.teradata.jdbc.TeraDriver" }, new String[] {}, "teradata",
                "org.hibernate.dialect.Teradata14Dialect", new String[] { "com.teradata.jdbc:terajdbc4" },
                "spring.datasource", true));

        items.put("ucanaccess",  new ExternalSource("ucanaccess",
                new String[] { "net.ucanaccess.jdbc.UcanaccessDriver" }, new String[] {}, "ucanaccess", null,
                new String[] { "net.sf.ucanaccess:ucanaccess" },  "spring.datasource", true));

        items.put("vertica",  new ExternalSource("vertica", new String[] { "com.vertica.jdbc.Driver" },
                new String[] {}, "vertica", null, new String[] { "org.clojars.erp12:jdbc-vertica" },
                "spring.datasource", true));
        loadConnctionFactories(this.getClass().getClassLoader(), "org.teiid.spring.data");
    }

    public void loadConnctionFactories(ClassLoader classloader, String packageName) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.setResourceLoader(new ResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                return null;
            }
            @Override
            public ClassLoader getClassLoader() {
                return classloader;
            }
        });
        provider.addIncludeFilter(new AnnotationTypeFilter(ConnectionFactoryConfiguration.class));
        Set<BeanDefinition> components = provider.findCandidateComponents(packageName);
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName(), false, classloader);
                ConnectionFactoryConfiguration cfc = clazz.getAnnotation(ConnectionFactoryConfiguration.class);
                if(cfc != null) {
                    ExternalSource source = build(cfc, c.getBeanClassName());
                    items.put(source.getName(), source);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("failed to load " + c.getBeanClassName());
            }
        }
    }

    private static ExternalSource build(ConnectionFactoryConfiguration annotation, String className) {
        String dialect = annotation.dialect();
        String[] dependencies = new String[] {"org.teiid:spring-data-"+annotation.alias()};
        ExternalSource source = new ExternalSource(annotation.alias(), new String[] { className }, new String[] {},
                annotation.translatorName(), dialect.isEmpty() ? null : dialect,
                        annotation.dependencies().length == 0 ? dependencies : annotation.dependencies(), null);
        return source;
    }

    public ExternalSource findByDriverName(String driverName) {
        for (ExternalSource source : this.items.values()) {
            for (String driver : source.getDriverNames()) {
                if (driver.equals(driverName)) {
                    return source;
                }
            }
            for (String driver : source.getDatasourceNames()) {
                if (driver.equals(driverName)) {
                    return source;
                }
            }
        }
        return null;
    }

    public ExternalSource find(String sourceName) {
        return this.items.get(sourceName);
    }

    public void addSource(ExternalSource source) {
        if (find(source.getName()) == null) {
            this.items.put(source.getName(), source);
        }
    }

    public void addSource(ConnectionFactoryConfiguration annotation, String className) {
        ExternalSource source = build(annotation, className);
        if (find(source.getName()) == null) {
            this.items.put(source.getName(), source);
        }
    }

    public Map<String, ExternalSource> getItems() {
        return items;
    }
}
