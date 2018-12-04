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

import static org.teiid.spring.autoconfigure.TeiidConstants.VDBNAME;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBVERSION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Driver;
import java.util.List;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.jboss.vfs.VirtualFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.cache.Cache;
import org.teiid.cache.CacheFactory;
import org.teiid.core.util.LRUCache;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.deployers.VDBRepository;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.metadatastore.DeploymentBasedDatabaseStore;
import org.teiid.query.metadata.PureZipFileSystem;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.autoconfigure.TeiidPostProcessor.Registrar;
import org.teiid.spring.data.file.FileConnectionFactory;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;
import org.xml.sax.SAXException;

@Configuration
@ConditionalOnClass({EmbeddedServer.class, ExecutionFactory.class})
@EnableConfigurationProperties(TeiidProperties.class)
@Import({ Registrar.class })
@PropertySource("classpath:teiid.properties")
public class TeiidAutoConfiguration implements Ordered {

    public static ThreadLocal<TeiidServer> serverContext = new ThreadLocal<>();

    private static final Log logger = LogFactory.getLog(TeiidAutoConfiguration.class);

    @Autowired(required = false)
    private EmbeddedConfiguration embeddedConfiguration;

    @Autowired
    private TeiidProperties properties;

    @Autowired
    ApplicationContext context;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy}")
    String hibernateNamingClass;

    @Autowired(required=false)
    private TransactionManager transactionManager;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public TeiidInitializer teiidInitializer(ApplicationContext applicationContext) {
        return new TeiidInitializer(applicationContext);
    }

    @Bean(name="dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource getDataSource(TeiidServer server) {
        EmbeddedDatabaseFactory edf = new EmbeddedDatabaseFactory();
        edf.setDatabaseConfigurer(new TeiidDatabaseConfigurer(server));
        edf.setDataSourceFactory(new DataSourceFactory() {
            @Override
            public DataSource getDataSource() {
                String url = context.getEnvironment().getProperty("spring.datasource.teiid.url");
                return new SimpleDriverDataSource(server.getDriver(), url);
            }

            @Override
            public ConnectionProperties getConnectionProperties() {
                return new ConnectionProperties() {
                    @Override
                    public void setDriverClass(Class<? extends Driver> driverClass) {
                    }
                    @Override
                    public void setUrl(String url) {
                    }
                    @Override
                    public void setUsername(String username) {
                    }
                    @Override
                    public void setPassword(String password) {
                    }
                };
            }
        });
        return edf.getDatabase();
    }

    @Bean
    @ConditionalOnMissingBean
    public VDBMetaData teiidVDB() {
        List<Resource> resources = TeiidInitializer.getScripts("teiid.vdb-file", this.properties.getVdbFile(),
                "teiid.ddl", this.context);

        VDBMetaData vdb = null;
        if (!resources.isEmpty()) {
            for (Resource resource : resources) {
                if (resource.getFilename().endsWith(".ddl")) {
                    try {
                        DeploymentBasedDatabaseStore store = new DeploymentBasedDatabaseStore(new VDBRepository());
                        String db = "CREATE DATABASE "+VDBNAME+" VERSION '"+VDBVERSION+"';\n";
                        db = db + "USE DATABASE "+VDBNAME+" VERSION '"+VDBVERSION+"';\n";
                        db = db + ObjectConverterUtil.convertToString(resources.get(0).getInputStream());
                        vdb = store.getVDBMetadata(db);
                        logger.info("Predefined VDB found :" + resources.get(0).getFilename());
                    } catch (FileNotFoundException e) {
                        // no-op
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to parse the VDB defined");
                    }
                    break;
                } else if (resource.getFilename().endsWith("-vdb.xml")) {
                    try {
                        vdb =  VDBMetadataParser.unmarshell(resources.get(0).getInputStream());
                    } catch (XMLStreamException | IOException e) {
                        throw new IllegalStateException("Failed to load the VDB defined", e);
                    }
                } else if (resource.getFilename().endsWith(".vdb")) {
                    try {
                        vdb = loadVDB(new File(resource.getFilename()).toURI().toURL());
                    } catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException | IOException
                            | URISyntaxException e) {
                        throw new IllegalStateException("Failed to load the VDB defined", e);
                    }
                }
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

    private VDBMetaData loadVDB(URL url) throws VirtualDatabaseException, ConnectorManagerException, TranslatorException,
            IOException, URISyntaxException {
        VirtualFile root = PureZipFileSystem.mount(url);
        VDBMetaData metadata;

        VirtualFile vdbMetadata = root.getChild("/META-INF/vdb.xml"); //$NON-NLS-1$
        if (vdbMetadata.exists()) {
            try {
                VDBMetadataParser.validate(vdbMetadata.openStream());
            } catch (SAXException e) {
                throw new VirtualDatabaseException(e);
            }
            InputStream is = vdbMetadata.openStream();
            try {
                metadata = VDBMetadataParser.unmarshell(is);
            } catch (XMLStreamException e) {
                throw new VirtualDatabaseException(e);
            }
        } else {
            vdbMetadata = root.getChild("/META-INF/vdb.ddl"); //$NON-NLS-1$
            DeploymentBasedDatabaseStore store = new DeploymentBasedDatabaseStore(new VDBRepository());
            metadata = store.getVDBMetadata(ObjectConverterUtil.convertToString(vdbMetadata.openStream()));
        }
        return metadata;
    }

    @Bean(name = "teiid")
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TeiidServer teiidServer() {
        logger.info("Starting Teiid Server.");

        // turning off PostgreSQL support
        System.setProperty("org.teiid.addPGMetadata", "false");

        final TeiidServer server = new TeiidServer();

        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
            embeddedConfiguration.setCacheFactory(new CacheFactory() {
                @Override
                public <K, V> Cache<K, V> get(String name) {
                    return new LocalCache<>(name, 10);
                }
                @Override
                public void destroy() {
                }
            });
        }

        if (embeddedConfiguration.getTransactionManager() == null) {
            PlatformTransactionManagerAdapter ptma = server.getPlatformTransactionManagerAdapter();
            ptma.setJTATransactionManager(this.transactionManager);
        	embeddedConfiguration.setTransactionManager(ptma);
        }

        server.start(embeddedConfiguration);

        // this is dummy vdb to satisfy the boot process to create the connections
        VDBMetaData vdb =  new VDBMetaData();
        vdb.setName(VDBNAME);
        vdb.setVersion(VDBVERSION);
        server.deployVDB(vdb, false);

        serverContext.set(server);
        return server;
    }

    static class LocalCache<K, V> extends LRUCache<K, V> implements Cache<K, V> {
        private static final long serialVersionUID = -7894312381042966398L;
        private String name;

        public LocalCache(String cacheName, int maxSize) {
            super(maxSize < 0 ? Integer.MAX_VALUE : maxSize);
            this.name = cacheName;
        }

        @Override
        public V put(K key, V value, Long ttl) {
            return put(key, value);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean isTransactional() {
            return false;
        }
    }

    @Bean(name="file")
    public FileConnectionFactory fileConnectionFactory() {
        return new FileConnectionFactory();
    }

    @Bean(name="teiidNamingStrategy")
    public PhysicalNamingStrategy teiidNamingStrategy() {
        try {
            return (PhysicalNamingStrategy)Class.forName(hibernateNamingClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            return null;
        }
    }
}