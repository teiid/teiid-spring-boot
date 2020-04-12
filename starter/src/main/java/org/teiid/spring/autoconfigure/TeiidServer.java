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

import static org.teiid.spring.autoconfigure.TeiidConstants.EXPOSED_VIEW;
import static org.teiid.spring.autoconfigure.TeiidConstants.REDIRECTED;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBNAME;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBVERSION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Entity;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.teiid.PreParser;
import org.teiid.adminapi.Admin;
import org.teiid.adminapi.Admin.TranlatorPropertyType;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.PropertyDefinition;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.SourceMappingMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.adminapi.impl.VDBTranslatorMetaData;
import org.teiid.core.TeiidException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.core.util.ReflectionHelper;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dialect.TeiidDialect;
import org.teiid.dqp.internal.datamgr.ConnectorManager;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Schema;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.metadata.VDBResources;
import org.teiid.query.parser.QueryParser;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.annotations.ExcelTable;
import org.teiid.spring.annotations.JsonTable;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.TextTable;
import org.teiid.spring.annotations.UserDefinedFunctions;
import org.teiid.spring.common.ExternalSource;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.spring.data.excel.ExcelConnectionFactory;
import org.teiid.spring.views.EntityBaseView;
import org.teiid.spring.views.ExcelTableView;
import org.teiid.spring.views.JsonTableView;
import org.teiid.spring.views.SimpleView;
import org.teiid.spring.views.TextTableView;
import org.teiid.spring.views.UDFProcessor;
import org.teiid.spring.views.ViewBuilder;
import org.teiid.spring.xa.XADataSourceBuilder;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.zaxxer.hikari.HikariDataSource;

public class TeiidServer extends EmbeddedServer {
    static final String DIALECT = "dialect";
    private static final Log logger = LogFactory.getLog(TeiidServer.class);
    private MetadataSources metadataSources = new MetadataSources();
    private PlatformTransactionManagerAdapter platformTransactionManagerAdapter;
    private ConcurrentHashMap<String, ConnectionFactoryProvider<?>> connectionFactoryProviders = new ConcurrentHashMap<String, ConnectionFactoryProvider<?>>();

    public TeiidServer() {
        this.cmr = new SBConnectorManagerRepository();
    }

    @SuppressWarnings("rawtypes")
    public void addDataSource(VDBMetaData vdb, String sourceBeanName, Object source, ApplicationContext context) {
        // only when user did not define a explicit VDB then build one.
        if (Boolean.valueOf(vdb.getPropertyValue(TeiidAutoConfiguration.IMPLICIT_VDB))) {
            boolean redirectUpdates = isRedirectUpdatesEnabled(context);
            String redirectedDSName = getRedirectedDataSource(context);

            addConnectionFactoryProvider(sourceBeanName, new SBConnectionFactoryProvider(source));

            ModelMetaData model = null;

            if (source instanceof DataSource) {
                String driverName = getDriverName(source);
                if (driverName == null) {
                    throw new IllegalStateException("Failed to determine the type of data source defined with bean name "
                            + sourceBeanName + " use Tomcat/Hikari based DataSource and XA DataSources are supported. "
                            + "Add the following to your pom.xml\n"
                            + "  <dependency>\n" +
                            "      <groupId>org.apache.tomcat</groupId>\n" +
                            "      <artifactId>tomcat-jdbc</artifactId>\n" +
                            "    </dependency>"
                            );
                }
                try {
                    model = buildModelFromDataSource(vdb, sourceBeanName, driverName, context,
                            redirectUpdates && sourceBeanName.equals(redirectedDSName));
                } catch (AdminException e) {
                    throw new IllegalStateException("Error adding the source, cause: " + e.getMessage());
                }
            } else if (source instanceof BaseConnectionFactory) {
                try {
                    model = buildModelFromConnectionFactory(vdb, sourceBeanName, (BaseConnectionFactory) source, context);
                } catch (AdminException e) {
                    throw new IllegalStateException("Error adding the source, cause: " + e.getMessage());
                }
            } else {
                throw new IllegalStateException("Unknown source type is being added");
            }

            // since each time a data source is added the vdb is reloaded
            // this is a cheap way not to do the reload of the metadata from source. a.k.a
            // metadata caching
            try {
                final Admin admin = getAdmin();
                VDBMetaData previous = (VDBMetaData) admin.getVDB(VDBNAME, VDBVERSION);
                for (Map.Entry<String, ModelMetaData> entry : previous.getModelMetaDatas().entrySet()) {
                    String metadata = admin.getSchema(VDBNAME, VDBVERSION, entry.getKey(), null, null);
                    if (vdb.getModel(entry.getKey()).getSourceMetadataType().isEmpty()) {
                        vdb.getModel(entry.getKey()).addSourceMetadata("DDL", metadata);
                    }
                }
            } catch (AdminException e) {
                // no-op. if failed redo
            }

            // add the new model
            if (model != null) {
                model.setVisible(false);
                vdb.addModel(model);
                logger.info("Added " + sourceBeanName + " to the Teiid Database");
            }

            undeployVDB(vdb.getName(), vdb.getVersion());
            deployVDB(vdb, false, context);
        } else {
            for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
                for (SourceMappingMetadata smm : model.getSourceMappings()) {
                    if (smm.getConnectionJndiName() != null && smm.getName().equalsIgnoreCase(sourceBeanName)) {
                        addConnectionFactory(smm.getConnectionJndiName(), source);
                        break;
                    }
                }
            }
        }
    }

    void addOverrideTranslator(VDBTranslatorMetaData translator, ApplicationContext context) {
        try {
            String type = translator.getType();
            ExternalSource es = ExternalSource.find(type);
            addTranslator(es, context);
            addTranslator(translator.getName(), type, translator.getPropertiesMap());
        } catch (TranslatorException e) {
            throw new IllegalStateException("Failed to load translator " + translator.getName(), e);
        }
    }

    void addTranslator(ExternalSource source, ApplicationContext context) {
        try {
            if (this.getExecutionFactory(source.getTranslatorName()) == null) {
                String basePackage = getBasePackage(context, true);
                Class<? extends ExecutionFactory<?, ?>> clazz = ExternalSource
                        .translatorClass(source.getTranslatorName(), basePackage);
                if (clazz != null) {
                    addTranslator(clazz);
                } else {
                    throw new IllegalStateException("Failed to load translator " + source.getName()
                    + ". Check to make sure @Translator annotation is added on your custom translator "
                    + "and also set the 'spring.teiid.model.package' set to package where the translator "
                    + "is defined. Otherwise, the following Dependencies are missing,\n"
                    + source.getGav()
                    + "\n\n in your pom.xml. Please add these dependencies. ");
                }
            }
        } catch (ConnectorManagerException | TranslatorException e) {
            throw new IllegalStateException("Failed to load translator " + source.getName(), e);
        }
    }

    static class SBConnectionFactoryProvider implements ConnectionFactoryProvider<Object> {
        private Object bean;

        SBConnectionFactoryProvider(Object bean) {
            this.bean = bean;
        }

        @Override
        public Object getConnectionFactory() throws TranslatorException {
            if (this.bean instanceof DataSource) {
                return new TransactionAwareDataSourceProxy((DataSource) bean);
            }
            return bean;
        }

        public Object getBean() {
            return bean;
        }
    }

    String getDriverName(Object source) {
        String driverName = null;
        if (source instanceof org.apache.tomcat.jdbc.pool.DataSource) {
            driverName = ((org.apache.tomcat.jdbc.pool.DataSource) source).getDriverClassName();
        } else if (source instanceof HikariDataSource) {
            driverName = ((HikariDataSource) source).getDriverClassName();
        } else {
            if (source instanceof DataSource) {
                try {
                    XADataSource xads = ((DataSource) source).unwrap(XADataSource.class);
                    if (xads != null) {
                        if (xads instanceof XADataSourceBuilder) {
                            driverName = ((XADataSourceBuilder) xads).dataSourceClassName();
                        } else {
                            driverName = xads.getClass().getName();
                        }
                    }
                } catch (SQLException e1) {
                    // ignore.
                }
            }
        }
        return driverName;
    }

    void deployVDB(VDBMetaData vdb, boolean last, ApplicationContext context) {
        try {
            if (Boolean.valueOf(vdb.getPropertyValue(TeiidAutoConfiguration.IMPLICIT_VDB))) {
                // if there is no view model, then keep all the other models as visible.
                if (vdb.getModel("teiid") == null) {
                    for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
                        model.setVisible(true);
                    }
                } else {
                    for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
                        if (!model.getName().equals("teiid")) {
                            model.setVisible(false);
                        }
                    }
                }
            }
            if (last && logger.isDebugEnabled()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                VDBMetadataParser.marshall(vdb, out);
                logger.debug("XML Form of VDB:\n" + prettyFormat(new String(out.toByteArray())));
            }

            // add any missing translators
            for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
                for (SourceMappingMetadata smm : model.getSourceMappings()) {
                    VDBTranslatorMetaData translator = vdb.getTranslator(smm.getTranslatorName());
                    if (translator != null) {
                        addOverrideTranslator(translator, context);
                    } else {
                        ExternalSource es = ExternalSource.find(smm.getTranslatorName());
                        addTranslator(es, context);
                    }

                    if (smm.getConnectionJndiName() != null) {
                        if (this.connectionFactoryProviders.get(smm.getConnectionJndiName()) == null){
                            throw new IllegalStateException("A Data source with JNDI name "
                                    + smm.getConnectionJndiName()
                                    + " is used but not configured, check your DataSources.java "
                                    + "file and configure it.");
                        }
                    }
                }
            }
            deployVDB(vdb, vdb.getAttachment(VDBResources.class));
        } catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException | XMLStreamException
                | IOException e) {
            throw new IllegalStateException("Failed to deploy the VDB file", e);
        }
    }

    private static String prettyFormat(String xml) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            DOMSource source = new DOMSource(db.parse(is));
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (IllegalArgumentException | TransformerFactoryConfigurationError | ParserConfigurationException
                | SAXException | IOException | TransformerException e) {
            return xml;
        }
    }

    private ModelMetaData buildModelFromDataSource(VDBMetaData vdb, String dsBeanName, String driverName,
            ApplicationContext context, boolean createInitTable) throws AdminException {

        ModelMetaData model = new ModelMetaData();
        model.setName(dsBeanName);
        model.setModelType(Model.Type.PHYSICAL);

        // note these can be overridden by specific ones in the configuration
        // TODO: need come up most sensible properties for this.
        model.addProperty("importer.useQualifiedName", "false");
        model.addProperty("importer.tableTypes", "TABLE,VIEW");

        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(dsBeanName);
        source.setConnectionJndiName(dsBeanName);

        ExternalSource es = ExternalSource.findByDriverName(driverName);
        source.setTranslatorName(es.getTranslatorName());

        String dialect = es.getDialect();
        if (dialect != null) {
            model.addProperty(DIALECT, dialect);
        }

        // load the translator class
        addTranslator(es, context);

        Properties overrideProperties = getTranslatorProperties(context, source.getTranslatorName(), dsBeanName,
                TranlatorPropertyType.OVERRIDE, new String[] {"spring.datasource", "spring.xa.datasource"});
        if (!overrideProperties.isEmpty()) {
            source.setTranslatorName(dsBeanName);
            VDBTranslatorMetaData t = new VDBTranslatorMetaData();
            t.setName(dsBeanName);
            t.setType(es.getTranslatorName());
            t.setProperties(overrideProperties);
            vdb.addOverideTranslator(t);
        }

        // add the importer properties from the configuration
        // note that above defaults can be overridden with this too.
        Properties importProperties = getTranslatorProperties(context, source.getTranslatorName(), dsBeanName,
                TranlatorPropertyType.IMPORT, new String[] {"spring.datasource", "spring.xa.datasource"});
        for (String k : importProperties.stringPropertyNames()) {
            model.addProperty(k, importProperties.getProperty(k));
        }
        model.addSourceMapping(source);

        // This is to avoid failing on empty schema
        if (createInitTable) {
            model.addSourceMetadata("NATIVE", "");
            model.addSourceMetadata("DDL", "create foreign table dual(id integer);");
        }
        return model;
    }

    Properties getTranslatorProperties(ApplicationContext context, String translatorName, String beanName,
            TranlatorPropertyType propertyType, String[] propertyPrefix) throws AdminException {
        Properties read = new Properties();
        Collection<? extends PropertyDefinition> importProperties = getAdmin()
                .getTranslatorPropertyDefinitions(translatorName, propertyType);
        importProperties.forEach(prop -> {
            String key = prop.getName();
            for (String prefix : propertyPrefix) {
                String envKey = prefix + "." + beanName + "." + key;
                String value = context.getEnvironment().getProperty(envKey.toLowerCase());
                if (value != null) {
                    read.setProperty(key, value);
                    break;
                }
            }
        });
        return read;
    }

    @SuppressWarnings("rawtypes")
    private ModelMetaData buildModelFromConnectionFactory(VDBMetaData vdb, String sourceBeanName,
            BaseConnectionFactory factory, ApplicationContext context) throws AdminException {
        ModelMetaData model = new ModelMetaData();
        model.setName(sourceBeanName);
        model.setModelType(Model.Type.PHYSICAL);

        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(sourceBeanName);
        source.setConnectionJndiName(sourceBeanName);

        ExternalSource es = ExternalSource.find(factory.getAlias());
        source.setTranslatorName(es.getTranslatorName());
        try {
            if (this.getExecutionFactory(es.getTranslatorName()) == null) {
                addTranslator(es, context);
            }
        } catch (ConnectorManagerException e) {
            throw new IllegalStateException("Failed to load translator " + es.getTranslatorName()
            + ", make sure maven dependency \n" + es.getGav() + "\n\n is available in your pom.xml file", e);
        }

        Properties overrideProperties = getTranslatorProperties(context, source.getTranslatorName(), sourceBeanName,
                TranlatorPropertyType.OVERRIDE, new String[] {factory.getConfigurationPrefix()});
        if (!overrideProperties.isEmpty()) {
            source.setTranslatorName(sourceBeanName);
            VDBTranslatorMetaData t = new VDBTranslatorMetaData();
            t.setName(sourceBeanName);
            t.setType(source.getTranslatorName());
            t.setProperties(overrideProperties);
            vdb.addOverideTranslator(t);
        }

        // add the importer properties from the configuration
        // note that above defaults can be overridden with this too.
        Properties importProperties = getTranslatorProperties(context, source.getTranslatorName(), sourceBeanName,
                TranlatorPropertyType.IMPORT, new String[] {factory.getConfigurationPrefix()});
        for (String k : importProperties.stringPropertyNames()) {
            model.addProperty(k, importProperties.getProperty(k));
        }

        model.addSourceMapping(source);
        return model;
    }

    boolean findAndConfigureViews(VDBMetaData vdb, ApplicationContext context, PhysicalNamingStrategy namingStrategy) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Entity.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Embeddable.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(SelectQuery.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(UserDefinedFunctions.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(ConnectionFactoryConfiguration.class));

        String basePackage = getBasePackage(context, false);

        // check to add any source models first based on the annotations
        boolean load = false;
        Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                ConnectionFactoryConfiguration cfc = clazz.getAnnotation(ConnectionFactoryConfiguration.class);
                if(cfc != null) {
                    ExternalSource.addSource(ExternalSource.build(cfc, c.getBeanClassName()));
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Error loading entity classes");
            }
        }
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                ExcelTable excelAnnotation = clazz.getAnnotation(ExcelTable.class);
                if (excelAnnotation != null) {
                    addExcelModel(vdb, clazz, excelAnnotation, context);
                    load = true;
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Error loading entity classes");
            }
        }


        ModelMetaData model = new ModelMetaData();
        model.setName(EXPOSED_VIEW);
        model.setModelType(Model.Type.VIRTUAL);
        MetadataFactory mf = new MetadataFactory(VDBNAME, VDBVERSION, SystemMetadata.getInstance().getRuntimeTypeMap(),
                model);

        if (components.isEmpty()) {
            if (isRedirectUpdatesEnabled(context)) {
                // when no @entity classes are defined then this is service based, sniff the
                // metadata
                // from Teiid models that are defined and build Hibernate metadata from it.
                buildVirtualBaseLayer(vdb, context, mf);
            } else {
                return false;
            }
        }

        Metadata metadata = getMetadata(components, namingStrategy, mf);
        UDFProcessor udfProcessor = new UDFProcessor(metadata, vdb);
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                Entity entityAnnotation = clazz.getAnnotation(Entity.class);
                SelectQuery selectAnnotation = clazz.getAnnotation(SelectQuery.class);
                TextTable textAnnotation = clazz.getAnnotation(TextTable.class);
                JsonTable jsonAnnotation = clazz.getAnnotation(JsonTable.class);
                ExcelTable excelAnnotation = clazz.getAnnotation(ExcelTable.class);
                UserDefinedFunctions udfAnnotation = clazz.getAnnotation(UserDefinedFunctions.class);

                if (textAnnotation != null && entityAnnotation != null) {
                    new TextTableView(metadata).buildView(clazz, mf, textAnnotation, context);
                } else if (jsonAnnotation != null && entityAnnotation != null) {
                    new JsonTableView(metadata).buildView(clazz, mf, jsonAnnotation, context);
                } else if (selectAnnotation != null && entityAnnotation != null) {
                    new SimpleView(metadata).buildView(clazz, mf, selectAnnotation, context);
                } else if (excelAnnotation != null && entityAnnotation != null) {
                    new ExcelTableView(metadata).buildView(clazz, mf, excelAnnotation, context);
                } else if (udfAnnotation != null) {
                    udfProcessor.buildFunctions(clazz, mf, udfAnnotation);
                } else if (selectAnnotation == null && entityAnnotation != null) {
                    new EntityBaseView(metadata, vdb, this).buildView(clazz, mf, entityAnnotation, context);
                }

                // check for sequence
                if (entityAnnotation != null) {
                    udfProcessor.buildSequence(clazz, mf, entityAnnotation);
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Error loading entity classes");
            }
        }
        udfProcessor.finishProcessing();

        // check if the redirection is in play
        if (isRedirectUpdatesEnabled(context)) {
            String redirectedDSName = getRedirectedDataSource(context);
            try {
                // rename current view model to something else
                model.setName("internal");
                model.setVisible(false);

                DataSource redirectedDS = (DataSource) ((SBConnectionFactoryProvider) getConnectionFactoryProviders()
                        .get(redirectedDSName)).getBean();
                String driverName = getDriverName(redirectedDS);
                if (driverName == null) {
                    throw new IllegalStateException("Redirection of updates enabled, however datasource"
                            + " configured for redirection is not recognized.");
                }

                RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(context, redirectedDSName);
                // if none of the annotations defined, create layer with tables from all data
                // sources
                if (mf.getSchema().getTables().isEmpty()) {
                    throw new IllegalStateException("Redirection of updates enabled, however there are no "
                            + "@Entity found. There must be atleast one @Entity for this feature to work.");
                }

                // now add the modified model that does the redirection
                ModelMetaData exposedModel = mg.buildRedirectionLayer(mf, EXPOSED_VIEW);
                vdb.addModel(exposedModel);

                // we need to create the schema in the redirected data source to store the
                // ephemeral data, will use
                // hibernate metadata for schema generation techniques.
                ModelMetaData redirectedModel = vdb.getModel(redirectedDSName);
                assert (redirectedModel != null);
                String dialect = redirectedModel.getPropertyValue(DIALECT);
                if (dialect == null) {
                    throw new IllegalStateException(
                            "Redirection is enabled, however data source named \"" + redirectedDSName
                            + "\" cannot be used with schema initialization, choose a different data source"
                            + "as there are no schema generation facilities for this data source.");
                }
                new RedirectionSchemaInitializer(redirectedDS, redirectedDSName, getDialect(dialect), metadata,
                        this.metadataSources.getServiceRegistry(), mf.getSchema(), context).init();

                // reload the redirection model as it has new entries now after schema
                // generation.
                try {
                    vdb.addModel(buildModelFromDataSource(vdb, redirectedDSName, driverName, context, false));
                } catch (AdminException e) {
                    throw new IllegalStateException("Error adding the source, cause: " + e.getMessage());
                }
                load = true;
            } catch (BeansException e) {
                throw new IllegalStateException("Redirection is enabled, however data source named \""
                        + redirectedDSName + "\" is not configured. Please configure a data source.");
            }
        }
        if (!mf.getSchema().getTables().isEmpty()) {
            load = true;
            String ddl = DDLStringVisitor.getDDLString(mf.getSchema(), null, null);
            model.addSourceMetadata("DDL", ddl);
            vdb.addModel(model);
        }
        return load;
    }

    private String getBasePackage(ApplicationContext context, boolean translator) {
        String basePackage = context.getEnvironment().getProperty(TeiidConstants.ENTITY_SCAN_DIR);
        if (basePackage == null) {
            if (translator) {
                logger.warn("***************************************************************");
                logger.warn("\"" + TeiidConstants.ENTITY_SCAN_DIR
                        + "\" is NOT set, If you are using any custom translators, it is advised "
                        + "to that this property is set. ");
                logger.warn("consider setting this property to avoid time consuming scanning");
                logger.warn("***************************************************************");

            } else {
                logger.warn("***************************************************************");
                logger.warn("\"" + TeiidConstants.ENTITY_SCAN_DIR
                        + "\" is NOT set, scanning entire classpath for @Entity classes.");
                logger.warn("consider setting this property to avoid time consuming scanning");
                logger.warn("***************************************************************");
            }
            basePackage = "*";
        }
        return basePackage;
    }

    boolean isRedirectUpdatesEnabled(ApplicationContext context) {
        return Boolean.parseBoolean(context.getEnvironment().getProperty(REDIRECTED));
    }

    private void buildVirtualBaseLayer(VDBMetaData vdb, ApplicationContext context, MetadataFactory target) {
        // Build Virtual Base Layer first
        String redirectedDSName = getRedirectedDataSource(context);
        SchemaBuilderUtility builder = new SchemaBuilderUtility();
        for (ModelMetaData m : vdb.getModelMetaDatas().values()) {
            if (ViewBuilder.isBuiltInModel(m.getName()) || m.getModelType() != Model.Type.PHYSICAL
                    || m.getName().equals(redirectedDSName)) {
                continue;
            }
            String dialect = m.getPropertyValue(DIALECT);
            MetadataFactory source = new MetadataFactory("x", 1, m.getName(),
                    SystemMetadata.getInstance().getRuntimeTypeMap(), new Properties(), null);
            for (String ddl : m.getSourceMetadataText()) {
                QueryParser.getQueryParser().parseDDL(source, ddl);
            }
            builder.generateVBLSchema(context, source, target, getDialect(dialect), this.metadataSources);
        }
    }

    public String getRedirectedDataSource(ApplicationContext context) {
        return context.getEnvironment().getProperty(TeiidConstants.REDIRECTED + ".datasource", "redirected");
    }

    private Metadata getMetadata(Set<BeanDefinition> components, PhysicalNamingStrategy namingStrategy,
            MetadataFactory mf) {
        ServiceRegistry registry = metadataSources.getServiceRegistry();
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(
                (BootstrapServiceRegistry) registry).applySetting(AvailableSettings.DIALECT, TeiidDialect.class)
                .build();
        // Generate Hibernate model based on @Entity definitions
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                metadataSources.addAnnotatedClass(clazz);
            } catch (ClassNotFoundException e) {
            }
        }
        return metadataSources.getMetadataBuilder(serviceRegistry).applyPhysicalNamingStrategy(namingStrategy).build();
    }

    private void addExcelModel(VDBMetaData vdb, Class<?> clazz, ExcelTable excelAnnotation, ApplicationContext context) {
        ModelMetaData model = new ModelMetaData();
        model.setName(clazz.getSimpleName().toLowerCase());
        model.setModelType(Model.Type.PHYSICAL);
        model.addProperty("importer.DataRowNumber", String.valueOf(excelAnnotation.dataRowStartsAt()));
        model.addProperty("importer.ExcelFileName", excelAnnotation.file());
        model.addProperty("importer.IgnoreEmptyHeaderCells", String.valueOf(excelAnnotation.ignoreEmptyCells()));
        if (excelAnnotation.headerRow() != -1) {
            model.addProperty("importer.HeaderRowNumber", String.valueOf(excelAnnotation.headerRow()));
        }

        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(clazz.getSimpleName().toLowerCase());
        source.setConnectionJndiName(excelAnnotation.source());
        source.setTranslatorName("excel");
        try {
            if (this.getExecutionFactory("excel") == null) {
                ConnectionFactoryConfiguration cfc = ExcelConnectionFactory.class.getAnnotation(ConnectionFactoryConfiguration.class);
                addTranslator(ExternalSource.build(cfc, ExcelConnectionFactory.class.getName()), context);
            }
        } catch (ConnectorManagerException e) {
            throw new IllegalStateException("Failed to load translator " + "excel", e);
        }
        model.addSourceMapping(source);

        vdb.addModel(model);
    }

    public Schema getSchema(VDBMetaData vdb, String modelName) {
        if (vdb == null) {
            return null;
        }
        TransformationMetadata metadata = vdb.getAttachment(TransformationMetadata.class);
        if (metadata == null) {
            return null;
        }
        Schema schema = metadata.getMetadataStore().getSchema(modelName);
        return schema;
    }

    private Dialect getDialect(String className) {
        try {
            return Dialect.class.cast(ReflectionHelper.create(className, null, this.getClass().getClassLoader()));
        } catch (TeiidException e) {
            throw new TeiidRuntimeException(
                    className + " could not be loaded. Add the dependecy required " + "dependency to your classpath"); //$NON-NLS-2$
        }
    }

    public PlatformTransactionManagerAdapter getPlatformTransactionManagerAdapter() {
        return platformTransactionManagerAdapter;
    }

    @SuppressWarnings("serial")
    protected class SBConnectorManagerRepository extends ConnectorManagerRepository {
        public SBConnectorManagerRepository() {
            super(true);
        }

        @Override
        protected ConnectorManager createConnectorManager(String translatorName, String connectionName,
                ExecutionFactory<Object, Object> ef) throws ConnectorManagerException {
            return new ConnectorManager(translatorName, connectionName, ef) {
                @Override
                public Object getConnectionFactory() throws TranslatorException {
                    if (getConnectionName() == null) {
                        return null;
                    }
                    ConnectionFactoryProvider<?> connectionFactoryProvider = connectionFactoryProviders
                            .get(getConnectionName());
                    if (connectionFactoryProvider != null) {
                        return connectionFactoryProvider.getConnectionFactory();
                    }
                    return super.getConnectionFactory();
                }
            };
        }
    }

    @Override
    public void addConnectionFactoryProvider(String jndiName, ConnectionFactoryProvider<?> connectionFactoryProvider) {
        this.connectionFactoryProviders.put(jndiName, connectionFactoryProvider);
    }

    @Override
    public void addConnectionFactory(String jndiName, Object connectionFactory) {
        this.connectionFactoryProviders.put(jndiName, new SimpleConnectionFactoryProvider<Object>(connectionFactory));
    }

    public ConnectionFactoryProvider<?> removeConnectionFactoryProvider(String jndiName) {
        return this.connectionFactoryProviders.remove(jndiName);
    }

    @Override
    protected boolean allowOverrideTranslators() {
        return true;
    }

    public void setPreParser(PreParser bean) {
        getConfiguration().setPreParser(bean);
    }

    public boolean isUsingPlatformTransactionManager() {
        return platformTransactionManagerAdapter != null;
    }

    public void setPlatformTransactionManagerAdapter(
            PlatformTransactionManagerAdapter platformTransactionManagerAdapter) {
        this.platformTransactionManagerAdapter = platformTransactionManagerAdapter;
    }

    public void registerSource(Object bean, ApplicationContext context) {
        if (bean.getClass().isAnnotationPresent(ConnectionFactoryConfiguration.class)) {
            ConnectionFactoryConfiguration annotation = bean.getClass().getAnnotation(ConnectionFactoryConfiguration.class);
            ExternalSource source = ExternalSource.build(annotation, bean.getClass().getName());
            ExternalSource.addSource(source);
        }
    }
}
