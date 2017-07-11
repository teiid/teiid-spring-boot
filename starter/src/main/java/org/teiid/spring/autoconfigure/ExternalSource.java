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

import static org.teiid.spring.autoconfigure.TeiidConstants.*;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.data.file.FileConnectionFactory;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

public enum ExternalSource {

    ACTIAN("Actian", new String[] {"com.ingres.jdbc.IngresDriver"}, "actian-vector"), 
    DB2("DB2", new String[] {"com.ibm.db2.jcc.DB2Driver"} , "db2"), 
    DERBY("Derby", new String[] {"org.apache.derby.jdbc.ClientDriver"}, "derby"), 
    H2("H2", new String[] {"org.h2.Driver"}, "h2"), 
    HANA("Hana", new String[] {"com.sap.db.jdbc.Driver"}, "hana"), 
    HIVE("Hive", new String[] {"org.apache.hive.jdbc.HiveDriver"}, "hive"), 
    IMPALA("Impala", new String[] {"org.apache.hadoop.hive.jdbc.HiveDriver"}, "impala"), // TODO: Fix me
    HSQL("HSQL", new String[] {"org.hsqldb.jdbc.JDBCDriver"}, "hsql"), 
    INFORMIX("Informix", new String[] {"com.informix.jdbc.IfxDriver"}, "informix"), 
    INGRES("Ingres", new String[] {"com.ingres.jdbc.IngresDriver"}, "ingres"), 
    INTERSYSTEMSCACHE("Intersystems Cache", new String[] {"com.intersys.jdbc.CacheDriver"}, "intersystems-cache"), 
    MYSQL("MySQL", new String[] {"com.mysql.jdbc.Driver"}, "mysql5"), 
    OLAP("OLAP", new String[] {"org.olap4j.driver.xmla.XmlaOlap4jDriver"}, "olap"), 
    MONDRIAN("Mondrian",new String[] {"mondrian.olap4j.MondrianOlap4jDriver"}, "olap"), 
    ORACLE("Oracle", new String[] {"oracle.jdbc.OracleDriver"}, "oracle"), 
    OSISOFTPI("OSISOFT PI", new String[] {"com.osisoft.jdbc.Driver"}, "osisoft-pi"), 
    PHOENIX("Phoenix", new String[] {"org.apache.phoenix.jdbc.PhoenixDriver"}, "phoenix"), 
    POSTGRESQL("PostgreSQL",new String[] {"org.postgresql.Driver"}, "postgresql"), 
    PRESTODB("PrestoDB", new String[] {"com.facebook.presto.jdbc.PrestoDriver"}, "prestodb"), 
    SQLSERVER("MS-SQL Server", new String[] {"com.microsoft.sqlserver.jdbc.SQLServerDriver"}, "sqlserver"), 
    JTDS("MS-SQL Server", new String[] {"net.sourceforge.jtds.jdbc.Driver"}, "sqlserver"), 
    SYBASE("Sybase", new String[] {"com.sybase.jdbc2.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDriver"}, "sybase"), 
    TEIID("Teiid", new String[] {"org.teiid.jdbc.TeiidDriver"}, "teiid"), 
    ACCESS("MS Access",new String[] {"net.ucanaccess.jdbc.UcanaccessDriver"}, "ucanaccess"), 
    VERTICA("Vertica", new String[] {"com.vertica.jdbc.Driver"}, "vertica"), 
    NETEZZA("Netezza",new String[] {"org.netezza.Driver "},"netezza"), 
    TERADATA("Teradata",new String[] {"com.teradata.jdbc.TeraDriver" }, "teradata"),

    FILE("file", new String[] { FileConnectionFactory.class.getName() }, "file"),
    REST("rest", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, "ws"),
	EXCEL("excel", new String[] { "org.teiid.spring.data.excel.ExcelConnectionFactory" }, "excel");
	
    // } else if(name.equals(accumulo)) {
    // return isPresent(accumulo_classes);
    // } else if(name.equals(cassandra)) {
    // return isPresent(cassandra_classes);
    // } else if(name.equals(couchbase)) {
    // return isPresent(couchbase_classes);
    // } else if(name.equals(file) || name.equals(excel)) {
    // return isPresent(file_classes) || isPresent(ftp_classes);
    // } else if(name.equals(google_spreadsheet)) {
    // return isPresent(google_classes);
    // } else if(name.equals(infinispan_hotrod)) {
    // return isPresent(infinispan_classes);
    // } else if(name.equals(ldap)) {
    // return isPresent(ldap_classes);
    // } else if(name.equals(mongodb)) {
    // return isPresent(mongodb_classes);
    // } else if(name.equals(salesforce)) {
    // return isPresent(salesforce_classes);
    // } else if(name.equals(salesforce_34)) {
    // return isPresent(salesforce_34_classes);
    // } else if(name.equals(simpledb)) {
    // return isPresent(simpledb_classes);
    // } else if(name.equals(solr)) {
    // return isPresent(solr_classes);
    // } else if(name.equals(ws) || name.equals(odata) || name.equals(odata4) ||
    // name.equals(swagger)) {
    // return isPresent(webservice_classes);
    // } else {
    // return false;
    // }

    private String name;
    private String[] driverNames;
    private String translatorName;

    private ExternalSource(String name, String[] driverNames, String translatorName) {
        this.name = name;
        this.driverNames = driverNames;
        this.translatorName = translatorName;
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

    public static String findTransaltorNameFromDriverName(String driverName) {
        for (ExternalSource source : ExternalSource.values()) {
            for (String driver : source.driverNames) {
                if (driver.equals(driverName)) {
                    return source.getTranslatorName();
                }
            }
        }
        return "loopback";
    }

    public static Class<? extends ExecutionFactory<?, ?>> translatorClass(String translatorName) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Translator.class));
        Set<BeanDefinition> components = provider.findCandidateComponents(FILTER_PACKAGE_TRANSLATOR);
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
}
