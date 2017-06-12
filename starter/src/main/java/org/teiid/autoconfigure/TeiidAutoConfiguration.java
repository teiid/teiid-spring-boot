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

package org.teiid.autoconfigure;

import static org.teiid.autoconfigure.TeiidConstants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.autoconfigure.TeiidPostProcessor.Registrar;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.deployers.VDBRepository;
import org.teiid.metadatastore.DeploymentBasedDatabaseStore;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

@Configuration
@ConditionalOnClass({EmbeddedServer.class, ExecutionFactory.class})
@EnableConfigurationProperties(TeiidProperties.class)
@Import({ Registrar.class })
public class TeiidAutoConfiguration implements Ordered {
    
    static ThreadLocal<TeiidServer> serverContext = new ThreadLocal<>(); 
    
    private static final Log logger = LogFactory.getLog(TeiidAutoConfiguration.class);    
        
    @Autowired(required = false)
    private EmbeddedConfiguration embeddedConfiguration;
    
    @Autowired
    private TeiidProperties properties;
    
    @Autowired
    ApplicationContext context;
    
    // TODO: need to configure with spring-tx
    //@Autowired(required = false)
    //private TransactionManager transactionManager;
    
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TeiidInitializer teiidInitializer(ApplicationContext applicationContext) {
        return new TeiidInitializer(applicationContext);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public VDBMetaData teiidVDB() {
        List<Resource> resources = TeiidInitializer.getScripts("teiid.vdb-file", this.properties.getVdbFile(),
                "teiid.ddl", this.context);
        
        VDBMetaData vdb = null;
        if (!resources.isEmpty()) {
            try {                
                DeploymentBasedDatabaseStore store = new DeploymentBasedDatabaseStore(new VDBRepository());
                vdb = store.getVDBMetadata(ObjectConverterUtil.convertToString(resources.get(0).getInputStream()));
                logger.info("Predefined VDB found :"+resources.get(0).getFilename());
            } catch (FileNotFoundException e) {
                // no-op
            } catch (IOException  e) {
                throw new IllegalStateException("Failed to parse the VDB defined"); 
            }
        }

        if (vdb == null) {
            vdb =  new VDBMetaData();
            vdb.addProperty("implicit", "true");
        }
        vdb.setName(VDBNAME);
        vdb.setVersion(VDBVERSION);            
        return vdb;
    }    
    
    @Bean(name = "teiid")
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TeiidServer teiidServer() {
        logger.info("Starting Teiid Server.");
        final TeiidServer server = new TeiidServer();
        
        deduceTranslators(server);
        
        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
        }
        
        server.start(embeddedConfiguration);
        server.deployVDB(teiidVDB());        
        serverContext.set(server);
        return server;
    }

    private void deduceTranslators(TeiidServer server) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
                false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Translator.class));
        Set<BeanDefinition> components = provider.findCandidateComponents(FILTER_PACKAGE_TRANSLATOR);
        components.forEach(c -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends ExecutionFactory<?, ?>> clazz = (Class<? extends ExecutionFactory<?, ?>>) Class
                        .forName(c.getBeanClassName());
                String name = clazz.getAnnotation(Translator.class).name();
                if (hasMappedDriver(name)) {
                    server.addTranslator(name, initTranslator(clazz));
                }
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                logger.warn("Error loading translators");
            } 
        });
    }

    private ExecutionFactory<?, ?> initTranslator(Class<?> clazz) throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ExecutionFactory<?, ?> ef = (ExecutionFactory<?, ?>) BeanUtils.instantiate(clazz);
        Method method = clazz.getMethod("start", new Class[] {});
        method.invoke(ef, new Object[] {});
        return ef;
    }
    
    /**
     * If a translator has reference connector, then it will be initialize and start
     * @param name
     * @return
     */
    private boolean hasMappedDriver(String name) {
        
        if(name.equals(actian_vector)) {
            return isPresent(actian_vector_classes);
        } else if(name.equals(db2)) {
            return isPresent(db2_classes);
        } else if(name.equals(derby)) {
            return isPresent(derby_classes);
        } else if(name.equals(h2)) {
            return isPresent(h2_classes);
        } else if(name.equals(hana)) {
            return isPresent(hana_classes);
        } else if(name.equals(hive) | name.equals(impala)) {
            return isPresent(hive_classes) || isPresent(hive_1_classes);
        } else if(name.equals(hsql)) {
            return isPresent(hsql_classes);
        } else if(name.equals(informix)) {
            return isPresent(informix_classes);
        } else if(name.equals(ingres) || name.equals(ingres93)) {
            return isPresent(ingres_classes);
        } else if(name.equals(intersystems_cache)) {
            return isPresent(intersystems_cache_classes);
        } else if(name.equals(mysql) || name.equals(mysql5)) {
            return isPresent(mysql_classes);
        } else if(name.equals(olap)) {
            return isPresent(olap_classes_xmla) || isPresent(olap_classes_mondrian);
        } else if(name.equals(oracle)) {
            return isPresent(oracle_classes);
        } else if(name.equals(osisoft_pi)) {
            return isPresent(osisoft_pi_classes);
        } else if(name.equals(hbase) || name.equals(phoenix)) {
            return isPresent(phoenix_classes);
        } else if(name.equals(postgresql) || name.equals(redshift) || name.equals(greenplum)) {
            return isPresent(postgresql_classes);
        } else if(name.equals(prestodb)) {
            return isPresent(prestodb_classes);
        } else if(name.equals(sqlserver)) {
            return isPresent(sqlserver_classes_jtds) || isPresent(sqlserver_classes_native);
        } else if(name.equals(sybase) || name.equals(sybaseiq)) {
            return isPresent(sybase_classes) || isPresent(sybase_classes_4);
        } else if(name.equals(teiid)) {
            return isPresent(teiid_classes);
        } else if(name.equals(ucanaccess)) {
            return isPresent(ucanaccess_classes);
        } else if(name.equals(vertica)) {
            return isPresent(vertica_classes);
        }  else if(name.equals(loopback) || name.equals(jpa2)) {
            return true;
        } else if(name.equals(access) || name.equals(metamatrix) || name.equals(modeshape) || name.equals(netezza) || name.equals(teradata)) {
            return false; // todo-- need auto detect class 
        } else if(name.equals(accumulo)) {
            return isPresent(accumulo_classes);
        } else if(name.equals(cassandra)) {
            return isPresent(cassandra_classes);
        } else if(name.equals(couchbase)) {
            return isPresent(couchbase_classes);
        } else if(name.equals(file) || name.equals(excel)) {
            return isPresent(file_classes) || isPresent(ftp_classes);
        } else if(name.equals(google_spreadsheet)) {
            return isPresent(google_classes);
        } else if(name.equals(infinispan_hotrod)) {
            return isPresent(infinispan_classes);
        } else if(name.equals(ldap)) {
            return isPresent(ldap_classes);
        } else if(name.equals(mongodb)) {
            return isPresent(mongodb_classes);
        } else if(name.equals(salesforce)) {
            return isPresent(salesforce_classes);
        } else if(name.equals(salesforce_34)) {
            return isPresent(salesforce_34_classes);
        } else if(name.equals(simpledb)) {
            return isPresent(simpledb_classes);
        } else if(name.equals(solr)) {
            return isPresent(solr_classes);
        } else if(name.equals(ws) || name.equals(odata) || name.equals(odata4) || name.equals(swagger)) {
            return isPresent(webservice_classes);
        } else {
            return false;
        }
    }

    private boolean isPresent(String[] classes) {
        for (String className : classes) {
            if(!ClassUtils.isPresent(className, null)) {
                return false;
            }
        }
        return true;
    }
}
