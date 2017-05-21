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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.core.TeiidRuntimeException;
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
@ComponentScan
@EnableConfigurationProperties
public class TeiidEmbeddedAutoConfiguration {
    
    private final String DEFAULT_ADDRESS = "0.0.0.0";
    private final Integer DEFAULT_PORT = 31000;
    
    @Autowired(required = false)
    private Integer jdbcPort;
    
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
    private Map<String, ExecutionFactory<?, ?>> translators = new HashMap<>();
    
    @Autowired(required = false)
    private Map<String, Object> connectionFactories = new HashMap<>();
    
    @Autowired
    private TeiidConnectorConfiguration config;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Bean
    public EmbeddedServer embeddedServer() {

        deduceTranslators();
        
        final EmbeddedServer server = new EmbeddedServer(); 
        
        translators.forEach((name, ef) -> {
            LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42006, name));
            server.addTranslator(name, ef);
        });
        connectionFactories.forEach((name, factory) -> server.addConnectionFactory(name, factory));
        
        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
        }
        
        if(socketConfiguration == null) {
            socketConfiguration = new SocketConfiguration();
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.jdbcPort == null ? DEFAULT_PORT : jdbcPort);
            embeddedConfiguration.addTransport(socketConfiguration);
        }
        
        if(embeddedConfiguration.getTransports() == null || embeddedConfiguration.getTransports().size() == 0) {
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.jdbcPort == null ? DEFAULT_PORT : jdbcPort);
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
                    new TeiidRuntimeException(TeiidEmbeddedPlugin.Event.TEIID42002, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, vdb));
                }
            } else if(resourceLoader.getResource("file:" + vdb).exists() && resourceLoader.getResource("file:pom.xml").isReadable()){
                try (InputStream is = resourceLoader.getResource("file:" + vdb).getInputStream()) {
                    server.deployVDB(is);
                } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                    new TeiidRuntimeException(TeiidEmbeddedPlugin.Event.TEIID42002, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, vdb));
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
        Set<BeanDefinition> components = provider.findCandidateComponents("org.teiid.translator");
        components.forEach(c -> {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                String name = clazz.getAnnotation(Translator.class).name();
                if(config.getTranslators().size() > 0) {
                    if(config.getTranslators().contains(name)) {
                        getTranslatorInstances(clazz, name);
                    }
                } else {
                    getTranslatorInstances(clazz, name);
                }
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LogManager.logWarning(CTX_EMBEDDED, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42007));
            }
        });
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
