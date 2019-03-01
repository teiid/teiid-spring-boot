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

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.data.file.FileConnectionFactory;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

public enum ExternalSource {
    DB2("DB2", new String[] {"com.ibm.db2.jcc.DB2Driver"}, new String[] {"com.ibm.db2.jcc.DB2XADataSource"}, "db2", "org.hibernate.dialect.DB2Dialect"),
    DERBY("Derby", new String[] {"org.apache.derby.jdbc.ClientDriver"}, new String[] {}, "derby", "org.hibernate.dialect.DerbyTenSevenDialect"),
    H2("H2", new String[] {"org.h2.Driver"}, new String[] {"org.h2.jdbcx.JdbcDataSource"}, "h2", "org.hibernate.dialect.H2Dialect"),
    HANA("Hana", new String[] {"com.sap.db.jdbc.Driver"}, new String[] {}, "hana", "org.hibernate.dialect.HANARowStoreDialect"),
    HIVE("Hive", new String[] {"org.apache.hive.jdbc.HiveDriver"}, new String[] {}, "hive", null),
    HSQL("HSQL", new String[] {"org.hsqldb.jdbc.JDBCDriver"}, new String[] {"org.hsqldb.jdbc.pool.JDBCXADataSource"}, "hsql", "org.hibernate.dialect.HSQLDialect"),
    IMPALA("Impala", new String[] {"org.apache.hadoop.hive.jdbc.HiveDriver"}, new String[] {}, "impala", null), // TODO: Fix me
    INFORMIX("Informix", new String[] {"com.informix.jdbc.IfxDriver"}, new String[] {}, "informix", "org.hibernate.dialect.InformixDialect"),
    INGRES("Ingres", new String[] {"com.ingres.jdbc.IngresDriver"}, new String[] {}, "ingres", "org.hibernate.dialect.Ingres10Dialect"),
    MYSQL("MySQL", new String[] {"com.mysql.jdbc.Driver"}, new String[] {"com.mysql.jdbc.jdbc2.optional.MysqlXADataSource"}, "mysql5", "org.hibernate.dialect.MySQL5InnoDBDialect"),
    ORACLE("Oracle", new String[] {"oracle.jdbc.OracleDriver"}, new String[] {"oracle.jdbc.xa.client.OracleXADataSource"}, "oracle", "org.hibernate.dialect.Oracle12cDialect"),
    OSISOFTPI("OSISOFT PI", new String[] {"com.osisoft.jdbc.Driver"}, new String[] {}, "osisoft-pi", null),
    PHOENIX("Phoenix", new String[] {"org.apache.phoenix.jdbc.PhoenixDriver"}, new String[] {}, "phoenix", null),
    POSTGRESQL("PostgreSQL",new String[] {"org.postgresql.Driver"}, new String[] {"org.postgresql.xa.PGXADataSource"}, "postgresql", "org.hibernate.dialect.PostgreSQL9Dialect"),
    PRESTODB("PrestoDB", new String[] {"com.facebook.presto.jdbc.PrestoDriver"}, new String[] {}, "prestodb", null),
    SQLSERVER("MS-SQL Server", new String[] {"com.microsoft.sqlserver.jdbc.SQLServerDriver"}, new String[] {"com.microsoft.sqlserver.jdbc.SQLServerXADataSource"}, "sqlserver", "org.hibernate.dialect.SQLServer2012Dialect"),
    JTDS("MS-SQL Server", new String[] {"net.sourceforge.jtds.jdbc.Driver"}, new String[] {}, "sqlserver",  "org.hibernate.dialect.SQLServer2012Dialect"),
    SYBASE("Sybase", new String[] {"com.sybase.jdbc2.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDriver"}, new String[] {}, "sybase", "org.hibernate.dialect.SybaseDialect"),
    TEIID("Teiid", new String[] {"org.teiid.jdbc.TeiidDriver"}, new String[] {}, "teiid", "org.teiid.dialect.TeiidDialect"),
    VERTICA("Vertica", new String[] {"com.vertica.jdbc.Driver"}, new String[] {}, "vertica", null),
    NETEZZA("Netezza",new String[] {"org.netezza.Driver "},new String[] {}, "netezza", null),
    TERADATA("Teradata",new String[] {"com.teradata.jdbc.TeraDriver" }, new String[] {}, "teradata", "org.hibernate.dialect.Teradata14Dialect"),

    FILE("file", new String[] { FileConnectionFactory.class.getName() }, new String[] {},"file", null),
    LOOPBACK("loopback", new String[] {}, new String[] {}, "loopback", null),
    REST("rest", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, new String[] {},"ws", null),
    EXCEL("excel", new String[] { "org.teiid.spring.data.excel.ExcelConnectionFactory" }, new String[] {},"excel", null),
    MONGODB("mongodb", new String[] { "org.teiid.spring.data.mongodb.MongoDBConnectionFactory" }, new String[] {},"mongodb", null);

    private String name;
    private String[] driverNames;
    private String[] datasourceNames;
    private String translatorName;
    private String dialect;

    ExternalSource(String name, String[] driverNames, String[] datasourceNames, String translatorName,
            String dialect) {
        this.name = name;
        this.driverNames = driverNames;
        this.datasourceNames = datasourceNames;
        this.translatorName = translatorName;
        this.dialect = dialect;
    }

    public String getName() {
        return name;
    }

    public String[] getDriverName() {
        return driverNames;
    }

    public String getTranslatorName() {
        return translatorName;
    }

    public String getDialect() {
        return dialect;
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

    public static String[] findDriverNameFromAlias(String sourceName) {
        for (ExternalSource source : ExternalSource.values()) {
            if (source.name.equalsIgnoreCase(sourceName)) {
                return source.getDriverName();
            }
        }
        return null;
    }
}
