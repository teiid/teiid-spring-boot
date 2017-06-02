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

package org.jboss.teiid.springboot;

import static org.jboss.teiid.springboot.TeiidEmbeddedConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.resource.cci.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.logging.LogManager;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.security.SecurityHelper;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.transport.SocketConfiguration;

/**
 * @author Kylin Soong
 */
@Configuration
@ConditionalOnClass({EmbeddedServer.class, ExecutionFactory.class})
@EnableConfigurationProperties(TeiidConnectorProperties.class)
public class TeiidEmbeddedAutoConfiguration {
    
    @Autowired(required = false)
    private Integer port;
    
    @Autowired(required = false)
    private SocketConfiguration socketConfiguration;
    
    @Autowired(required = false)
    private EmbeddedConfiguration embeddedConfiguration;
    
    @Autowired(required = false)
    private TransactionManager transactionManager;
    
    @Autowired(required = false)
    private SecurityHelper securityHelper;
    
    @Autowired(required = false)
    private String securityDomain;
    
    @Autowired(required = false)
    private Map<String, ExecutionFactory<?, ?>> translators = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private Set<String> connectionFactoryNames = Collections.synchronizedSet(new HashSet<>());
    
    @Autowired(required = false)
    private Map<String, ConnectionFactory> connectionFactories = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private Map<String, DataSource> datasources = new ConcurrentHashMap<>();
    
    @Autowired
    private TeiidConnectorProperties config;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Bean
    public EmbeddedServer embeddedServer() {

        deduceTranslators();
                
        final EmbeddedServer server = new EmbeddedServer(); 
        
        translators.forEach((name, ef) -> {
            LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42005, name));
            server.addTranslator(name, ef);
        });
        
        Map<String, Object> connectors = new HashMap<>();
        
        // This for re-use spring based bean injection
        connectionFactoryNames.forEach(name -> {
            Object factory = applicationContext.getBean(name);
            connectors.put(name, factory);
        });
        connectionFactories.forEach((name, factory) -> connectors.put(name, factory));
        datasources.forEach((name, ds) -> connectors.put(name, ds));
        
        connectors.entrySet().stream()
              .filter(entry -> entry.getValue() instanceof DataSource || entry.getValue() instanceof ConnectionFactory)
              .collect(Collectors.toMap(k -> k.getKey(), u -> u.getValue()))
              .forEach((name, factory) -> {
                  LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42006, name));
                  server.addConnectionFactory(name, factory);
              });
        
        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
        }
        
        if(socketConfiguration == null) {
            socketConfiguration = new SocketConfiguration();
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.port == null ? DEFAULT_PORT : port);
            embeddedConfiguration.addTransport(socketConfiguration);
        }
        
        if(embeddedConfiguration.getTransports() == null || embeddedConfiguration.getTransports().size() == 0) {
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.port == null ? DEFAULT_PORT : port);
            embeddedConfiguration.addTransport(socketConfiguration);
        }
        
        if(transactionManager != null && embeddedConfiguration.getTransactionManager() == null) {
            embeddedConfiguration.setTransactionManager(transactionManager);
        }
        
        if(securityDomain != null) {
            embeddedConfiguration.setSecurityDomain(securityDomain);
        }
        
        if(securityHelper != null) {
            embeddedConfiguration.setSecurityHelper(securityHelper);
        }
                
        server.start(embeddedConfiguration);
        
        LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42008, socketConfiguration.getHostName(), String.valueOf(socketConfiguration.getPortNumber())));
        
        config.getVdbs().forEach(vdb -> {
            if(resourceLoader.getResource(vdb).exists() && resourceLoader.getResource(vdb).isReadable()) {
                try (InputStream is = resourceLoader.getResource(vdb).getInputStream()) {
                    server.deployVDB(is);
                } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                    LogManager.logError(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, vdb));
                }
            } else if(resourceLoader.getResource("file:" + vdb).exists() && resourceLoader.getResource("file:" + vdb).isReadable()){
                try (InputStream is = resourceLoader.getResource("file:" + vdb).getInputStream()) {
                    server.deployVDB(is);
                } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                    LogManager.logError(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, vdb));
                }
            }
        });
        
        config.getDdls().forEach(ddl -> {
            if(resourceLoader.getResource(ddl).exists() && resourceLoader.getResource(ddl).isReadable()){
                try (InputStream is = resourceLoader.getResource(ddl).getInputStream()){
                    server.deployVDB(is, true);
                } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                    LogManager.logError(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, ddl));
                } 
            } else if(resourceLoader.getResource("file:" + ddl).exists() && resourceLoader.getResource("file:" + ddl).isReadable()) {
                try (InputStream is = resourceLoader.getResource("file:" + ddl).getInputStream()){
                    server.deployVDB(is, true);
                } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                    LogManager.logError(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, ddl));
                } 
            } 
        });
        
        return server;
    }

    private void deduceTranslators() {
        
        if(this.translators.size() > 0) {
            return;
        }

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Translator.class));
        Set<BeanDefinition> components = provider.findCandidateComponents(FILTER_PACKAGE_TRANSLATOR);
        components.forEach(c -> {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                String name = clazz.getAnnotation(Translator.class).name();
                if(config.getTranslators().size() > 0) { // The translators be set manually
                    if(config.getTranslators().contains(name)) {
                        getTranslatorInstances(clazz, name);
                    }
                } else if (hasMappedDriver(name)) {
                    getTranslatorInstances(clazz, name);
                } 
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LogManager.logWarning(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42007));
            }
        });
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

    /**
     * Use to initialize Translator {@link ExecutionFactory}, and invoke it's start() method.
     * @param clazz - translator class name auto-detected from class path.
     * @param name - translator name
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private void getTranslatorInstances(Class<?> clazz, String name) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ExecutionFactory<?, ?> ef = (ExecutionFactory<?, ?>) BeanUtils.instantiate(clazz);
        Method method = clazz.getMethod("start", new Class[]{});
        method.invoke(ef, new Object[]{});
        this.translators.put(name, ef);
    }

}
