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

    ACTIAN("Actian", actian_vector_classes, actian_vector), 
    DB2("DB2", db2_classes, db2), 
    DERBY("Derby", derby_classes, derby), 
    H2("H2", h2_classes, h2), 
    HANA("Hana", hana_classes,hana), 
    HIVE("Hive", hive_classes, hive), 
    IMPALA("Impala", hive_1_classes, impala), // TODO: Fix me
    HSQL("HSQL", hsql_classes, hsql), 
    INFORMIX("Informix", informix_classes, informix), 
    INGRES("Ingres", ingres_classes, ingres), 
    INTERSYSTEMSCACHE("Intersystems Cache", intersystems_cache_classes, intersystems_cache), 
    MYSQL("MySQL", mysql_classes, mysql5), 
    OLAP("OLAP", olap_classes_mondrian, olap), 
    MONDRIAN("Mondrian",olap_classes_mondrian,olap), 
    ORACLE("Oracle", oracle_classes, oracle), 
    OSISOFTPI("OSISOFT PI", osisoft_pi_classes,osisoft_pi), 
    PHOENIX("Phoenix", phoenix_classes, phoenix), 
    POSTGRESQL("PostgreSQL",postgresql_classes,postgresql), 
    PRESTODB("PrestoDB", prestodb_classes, prestodb), 
    SQLSERVER("MS-SQL Server", sqlserver_classes_native,sqlserver), 
    JTDS("MS-SQL Server", sqlserver_classes_jtds,sqlserver), 
    SYBASE("Sybase", sybase_classes, sybase), 
    TEIID("Teiid", teiid_classes, teiid), 
    ACCESS("MS Access",ucanaccess_classes, ucanaccess), 
    VERTICA("Vertica", vertica_classes,vertica), 
    NETEZZA("Netezza",new String[] {"org.netezza.Driver "},"netezza"), 
    TERADATA("Teradata",new String[] {"com.teradata.jdbc.TeraDriver" }, "teradata"),

    FILE("file", new String[] { FileConnectionFactory.class.getName() }, "file"),
    REST("rest", new String[] { "org.teiid.spring.data.rest.RestConnectionFactory" }, "ws");

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

    public static String findTransaltorNameFromSourceName(String sourceName) {
        for (ExternalSource source : ExternalSource.values()) {
                if (source.name.equalsIgnoreCase(sourceName)) {
                    return source.getTranslatorName();
                }
        }
        return null;
    }
}
