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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.teiid.spring.identity.SpringSecurityHelper;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.WireProtocol;
import org.xml.sax.SAXException;

import com.github.benmanes.caffeine.cache.Caffeine;

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
    private String hibernateNamingClass;

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
    public DataSource getDataSource(TeiidServer server, VDBMetaData vdb) {
        EmbeddedDatabaseFactory edf = new EmbeddedDatabaseFactory();
        edf.setDatabaseConfigurer(new TeiidDatabaseConfigurer(server));
        edf.setDataSourceFactory(new DataSourceFactory() {
            @Override
            public DataSource getDataSource() {
                String url = context.getEnvironment().getProperty("spring.datasource.teiid.url");
                return new SimpleDriverDataSource(new TeiidSpringDriver(server.getDriver(), server, vdb), url);
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
                        String db = ObjectConverterUtil.convertToString(resources.get(0).getInputStream());
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
            vdb.setName(VDBNAME);
            vdb.setVersion(VDBVERSION);
        }
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
    public TeiidServer teiidServer(SpringSecurityHelper securityHelper) {
        logger.info("Starting Teiid Server.");

        // turning off PostgreSQL support
        System.setProperty("org.teiid.addPGMetadata", "false");

        final TeiidServer server = new TeiidServer();

        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
            embeddedConfiguration.setCacheFactory(new CaffeineCacheFactory());
            // add ability for remote jdbc connections
            if (this.properties.isJdbcEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setPortNumber(this.properties.getJdbcPort());
                sc.setProtocol(WireProtocol.teiid);
                embeddedConfiguration.addTransport(sc);
            }

            if (this.properties.isOdbcEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setPortNumber(this.properties.getOdbcPort());
                sc.setProtocol(WireProtocol.pg);
                embeddedConfiguration.addTransport(sc);
            }
        }

        if (embeddedConfiguration.getTransactionManager() == null) {
            PlatformTransactionManagerAdapter ptma = server.getPlatformTransactionManagerAdapter();
            ptma.setJTATransactionManager(this.transactionManager);
            embeddedConfiguration.setTransactionManager(ptma);
        }

        if (embeddedConfiguration.getSecurityHelper() == null) {
            embeddedConfiguration.setSecurityDomain(TeiidConstants.SPRING_SECURITY);
            embeddedConfiguration.setSecurityHelper(securityHelper);
        }

        server.start(embeddedConfiguration);

        // this is dummy vdb to satisfy the boot process to create the connections
        VDBMetaData vdb =  new VDBMetaData();
        vdb.setName(VDBNAME);
        vdb.setVersion(VDBVERSION);
        server.deployVDB(vdb, false, this.context);

        serverContext.set(server);
        return server;
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SpringSecurityHelper securityHelper() {
        return new SpringSecurityHelper();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class CaffeineCacheFactory implements CacheFactory {
        Map<String, Cache> map = new HashMap<>();
        @Override
        public <K, V> Cache<K, V> get(String name) {
            map.put(name, new CaffeineCache<K,V>(name, 256));
            return map.get(name);
        }
        @Override
        public void destroy() {
            Set<String> keys = new HashSet<>(map.keySet());
            keys.forEach(k -> map.get(k).clear());
            map.clear();
        }
    }
    static class CaffeineCache<K, V> implements Cache<K, V> {
        private String name;
        private com.github.benmanes.caffeine.cache.Cache<K,V> delegate;

        CaffeineCache(String cacheName, int maxSize) {
            this.name = cacheName;
            this.delegate = Caffeine.newBuilder()
                    .weakKeys()
                    .weakValues()
                    .maximumSize(maxSize < 0 ? 10000 : maxSize)
                    .build();
        }

        @Override
        public V put(K key, V value, Long ttl) {
            delegate.put(key, value);
            return delegate.getIfPresent(key);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean isTransactional() {
            return false;
        }

        @Override
        public V get(K key) {
            return delegate.getIfPresent(key);
        }

        @Override
        public V remove(K key) {
            V v = delegate.getIfPresent(key);
            delegate.invalidate(key);
            return v;
        }

        @Override
        public int size() {
            return Math.toIntExact(delegate.estimatedSize());
        }

        @Override
        public void clear() {
            delegate.invalidateAll();
        }

        @Override
        public Set<K> keySet() {
            return delegate.asMap().keySet();
        }
    }

    @Bean(name="file")
    public FileConnectionFactory fileConnectionFactory() {
        return new FileConnectionFactory();
    }

    @Bean(name = "teiidNamingStrategy")
    public PhysicalNamingStrategy teiidNamingStrategy() {
        try {
            return (PhysicalNamingStrategy) Class.forName(hibernateNamingClass).getDeclaredConstructors()[0]
                    .newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                | InvocationTargetException e) {
            return null;
        }
    }
}
