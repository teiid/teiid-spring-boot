/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.sql.Driver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.cache.caffeine.CaffeineCacheFactory;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.deployers.VDBRepository;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.metadatastore.DeploymentBasedDatabaseStore;
import org.teiid.net.socket.SocketUtil;
import org.teiid.query.metadata.NioVirtualFile;
import org.teiid.query.metadata.NioZipFileSystem;
import org.teiid.query.metadata.VDBResources;
import org.teiid.query.metadata.VirtualFile;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.autoconfigure.TeiidPostProcessor.Registrar;
import org.teiid.spring.common.ExternalSources;
import org.teiid.spring.data.file.FileConnectionFactory;
import org.teiid.spring.identity.SpringSecurityHelper;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.WireProtocol;
import org.xml.sax.SAXException;

import com.zaxxer.hikari.pool.ProxyConnection;

@Configuration
@ConditionalOnClass({EmbeddedServer.class, ExecutionFactory.class})
@EnableConfigurationProperties(TeiidProperties.class)
@Import({ Registrar.class, TransactionManagerConfiguration.class })
@PropertySource("classpath:teiid.properties")
@AutoConfigureAfter(JtaAutoConfiguration.class)
@AutoConfigureBefore({ DataSourceAutoConfiguration.class })
public class TeiidAutoConfiguration {

    static final String IMPLICIT_VDB = "implicit";
    public static ThreadLocal<TeiidServer> serverContext = new ThreadLocal<>();
    private static final Log logger = LogFactory.getLog(TeiidAutoConfiguration.class);

    @Autowired(required = false)
    private EmbeddedConfiguration embeddedConfiguration;

    @Autowired(required = false)
    private PlatformTransactionManagerAdapter platformTransactionManagerAdapter;

    @Autowired
    private TeiidProperties properties;

    @Autowired
    ApplicationContext context;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy}")
    private String hibernateNamingClass;

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
        List<Resource> resources = null;
        if (this.properties.getVdbFile() != null) {
            resources = TeiidInitializer.getClasspathResources(this.context, this.properties.getVdbFile());
            if (resources.isEmpty()) {
                throw new IllegalStateException("Failed to find" + this.properties.getVdbFile());
            }
        } else {
            resources = TeiidInitializer.getClasspathResources(this.context, "teiid.ddl", "teiid.vdb");
        }

        VDBMetaData vdb = null;
        if (!resources.isEmpty()) {
            Resource resource = resources.iterator().next();
            if (resource.getFilename().endsWith(".ddl")) {
                try {
                    DeploymentBasedDatabaseStore store = new DeploymentBasedDatabaseStore(new VDBRepository());
                    String db = ObjectConverterUtil.convertToString(resources.get(0).getInputStream());
                    vdb = store.getVDBMetadata(db);
                    VDBResources vdbResources = buildVdbResources(getParent(resource));
                    vdb.addAttachment(VDBResources.class, vdbResources);
                    logger.info("Predefined VDB found = " + resource.getFilename());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to parse the VDB defined");
                }
            } else if (resource.getFilename().endsWith("-vdb.xml")) {
                try {
                    vdb = VDBMetadataParser.unmarshall(resource.getInputStream());
                    VDBResources vdbResources = buildVdbResources(getParent(resource));
                    vdb.addAttachment(VDBResources.class, vdbResources);
                    logger.info("Predefined VDB found = " + resource.getFilename());
                } catch (XMLStreamException | IOException e) {
                    throw new IllegalStateException("Failed to load the VDB defined", e);
                }
            } else if (resource.getFilename().endsWith(".vdb")) {
                try {
                    vdb = loadVDB(resource);
                    logger.info("Predefined VDB found = " + resource.getFilename());
                } catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException | IOException
                        | URISyntaxException e) {
                    throw new IllegalStateException("Failed to load the VDB defined", e);
                }
            }
        }

        if (vdb == null) {
            vdb = new VDBMetaData();
            vdb.addProperty(IMPLICIT_VDB, "true");
            vdb.setName(VDBNAME);
            vdb.setVersion(VDBVERSION);
        }
        return vdb;
    }

    private String getParent(Resource resource) throws IOException {
        String resourcePath = resource.getURI().toString();
        if (resource instanceof FileSystemResource) {
            resourcePath = ((FileSystemResource)resource).getFile().getPath().toString();
        }
        int indexColon = resourcePath.lastIndexOf(':');
        if (indexColon != -1) {
            // this is a url based resource
            resourcePath = resourcePath.substring(indexColon+1);
        }
        int index = resourcePath.lastIndexOf('/');
        return index > 0 ? resourcePath.substring(0, index + 1) : "/";
    }

    private VDBResources buildVdbResources(String curdir) throws IOException {
        List<Resource> resources = TeiidInitializer.getClasspathResources(this.context, "*.ddl", "*.sql", "*/*.ddl",
                "*/*.sql");
        LinkedHashMap<String, VDBResources.Resource> files = new LinkedHashMap<>();
        for (Resource r : resources) {
            if (r instanceof FileSystemResource) {
                Path p = r.getFile().toPath();
                String path = p.toString().replace(curdir, "");
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                files.put(path, new VDBResources.Resource(new NioVirtualFile(p)));
            } else if (r instanceof UrlResource) {
                String resourcePath = r.getURI().toString();
                int indexColon = resourcePath.lastIndexOf(':');
                if (indexColon != -1) {
                    resourcePath = resourcePath.substring(indexColon+1);
                }
                String shortenedPath = resourcePath.replace(curdir, "");
                files.put(shortenedPath, new VDBResources.Resource(new UrlResourceVirtualFile(shortenedPath, (UrlResource)r)));
            }
        }
        VDBResources vdbResources = new VDBResources(new NioVirtualFile(Paths.get("application.properties")));
        vdbResources.getEntriesPlusVisibilities().putAll(files);
        return vdbResources;
    }

    private VDBMetaData loadVDB(Resource resource) throws VirtualDatabaseException, ConnectorManagerException,
            TranslatorException, IOException, URISyntaxException {

        File f = File.createTempFile("temp", null);
        ObjectConverterUtil.write(resource.getInputStream(), f);
        f.deleteOnExit();

        VirtualFile root = NioZipFileSystem.mount(f.toURI().toURL());
        VDBMetaData metadata;

        VirtualFile vdbMetadata = root.getChild("/vdb.xml"); //$NON-NLS-1$
        if (!vdbMetadata.exists()) {
            vdbMetadata = root.getChild("/META-INF/vdb.xml"); //$NON-NLS-1$
        }
        if (vdbMetadata.exists()) {
            try {
                VDBMetadataParser.validate(vdbMetadata.openStream());
            } catch (SAXException e) {
                throw new VirtualDatabaseException(e);
            }
            InputStream is = vdbMetadata.openStream();
            try {
                metadata = VDBMetadataParser.unmarshall(is);
            } catch (XMLStreamException e) {
                throw new VirtualDatabaseException(e);
            }
        } else {
            vdbMetadata = root.getChild("/vdb.ddl"); //$NON-NLS-1$
            if (!vdbMetadata.exists()) {
                vdbMetadata = root.getChild("/META-INF/vdb.ddl"); //$NON-NLS-1$
            }
            DeploymentBasedDatabaseStore store = new DeploymentBasedDatabaseStore(new VDBRepository());
            try {
                metadata = store.getVDBMetadata(ObjectConverterUtil.convertToString(vdbMetadata.openStream()));
            } catch (IOException e) {
                throw new VirtualDatabaseException("Could not find a vdb.xml or vdb.ddl file in " + resource.getFilename());
            }
        }
        metadata.addAttachment(VirtualFile.class, root); //for auto cleanup of zip fs
        VDBResources resources = new VDBResources(root);
        metadata.addAttachment(VDBResources.class, resources);
        return metadata;
    }

    @Bean(name = "teiid")
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TeiidServer teiidServer(SpringSecurityHelper securityHelper, TransactionManager transactionManager,
                                   ExternalSources sources) {
        logger.info("Starting Teiid Server.");

        // turning off PostgreSQL support
        System.setProperty("org.teiid.addPGMetadata", Boolean.toString(this.properties.isPgEnable() || this.properties.isPgSecureEnable()));
        System.setProperty("org.teiid.hiddenMetadataResolvable", "false");
        System.setProperty("org.teiid.allowAlter", Boolean.toString(this.properties.isAllowAlter()));

        final TeiidServer server = new TeiidServer(sources);

        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
            embeddedConfiguration.setCacheFactory(new CaffeineCacheFactory());

            if (this.properties.getNodeName() != null) {
                String nodeName = this.properties.getNodeName();
                if (this.properties.getPodName() != null) {
                    nodeName = this.properties.getPodName();
                }
                embeddedConfiguration.setNodeName(nodeName);
            }

            // add ability for remote jdbc connections
            if (this.properties.isJdbcEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setBindAddress(this.properties.getHostName());
                sc.setPortNumber(this.properties.getJdbcPort());
                sc.setProtocol(WireProtocol.teiid);
                embeddedConfiguration.addTransport(sc);
                logger.info("JDBC is opened on = " + this.properties.getHostName() + ":"
                        + this.properties.getJdbcPort());

            }

            if (this.properties.isJdbcSecureEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setBindAddress(this.properties.getHostName());
                sc.setPortNumber(this.properties.getJdbcSecurePort());
                sc.setProtocol(WireProtocol.teiid);
                sc.setSSLConfiguration(this.properties.getSsl());
                embeddedConfiguration.addTransport(sc);
                logger.info("Secure JDBC is opened on = " + this.properties.getHostName() + ":"
                        + this.properties.getJdbcSecurePort());
            }

            if (this.properties.isPgEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setBindAddress(this.properties.getHostName());
                sc.setPortNumber(this.properties.getPgPort());
                sc.setProtocol(WireProtocol.pg);
                embeddedConfiguration.addTransport(sc);
                logger.info("PG is opened on = " + this.properties.getHostName() + ":"
                        + this.properties.getPgPort());

            }

            if (this.properties.isPgSecureEnable()) {
                SocketConfiguration sc = new SocketConfiguration();
                sc.setBindAddress(this.properties.getHostName());
                sc.setPortNumber(this.properties.getPgSecurePort());
                sc.setProtocol(WireProtocol.pg);
                sc.setSSLConfiguration(this.properties.getSsl());
                embeddedConfiguration.addTransport(sc);
                logger.info("Secure PG is opened on = " + this.properties.getHostName() + ":"
                        + this.properties.getPgSecurePort());
            }
        }

        if (embeddedConfiguration.getTransactionManager() == null) {
            embeddedConfiguration.setTransactionManager(transactionManager);
            if (transactionManager == platformTransactionManagerAdapter) {
                server.setPlatformTransactionManagerAdapter(platformTransactionManagerAdapter);
            } else {
                logger.info("Transaction Manager found and being registed into Teiid.");
            }
        } else if (transactionManager != null && transactionManager != embeddedConfiguration.getTransactionManager()) {
            throw new IllegalStateException("TransactionManager defined in both Spring and on the EmbeddedConfiguration. Only one is expected.");
        }

        if (embeddedConfiguration.getSecurityHelper() == null) {
            embeddedConfiguration.setSecurityDomain(TeiidConstants.SPRING_SECURITY);
            embeddedConfiguration.setSecurityHelper(securityHelper);
        }

        server.start(embeddedConfiguration);

        // this is dummy vdb to satisfy the boot process to create the connections
        VDBMetaData vdb = new VDBMetaData();
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

    @Bean(name="file")
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix="spring.teiid.file")
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

    @PostConstruct
    private void applyErrorStateHack() {
        try {
            Field field = ProxyConnection.class.getDeclaredField("ERROR_STATES");
            field.setAccessible(true);
            Set<String> errorStates = (Set<String>) field.get(null);
            errorStates.remove("0A000");
        } catch (Exception sqle) {
            logger.warn("Unable to apply error state hack", sqle);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyManager keyManager() throws IOException {
        try {
            KeyManager[] km = this.properties.getSsl().getKeyManagers();
            if (km != null && km.length > 0) {
                return km[0];
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Bean
    @ConditionalOnMissingBean
    public TrustManager trustManager() throws IOException {
        try {
            TrustManager[] tm = this.properties.getSsl().getTrustManagers();
            if (tm != null && tm.length > 0) {
                return tm[0];
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
        // if there are no trust managers configured then trust-all?
        return SocketUtil.getTrustAllManagers()[0];
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalSources externalSources() {
        return new ExternalSources();
    }
}
