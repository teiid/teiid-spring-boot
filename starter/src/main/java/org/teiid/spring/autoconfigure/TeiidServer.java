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
import org.teiid.query.parser.QueryParser;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.annotations.ExcelTable;
import org.teiid.spring.annotations.JsonTable;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.TextTable;
import org.teiid.spring.annotations.UserDefinedFunctions;
import org.teiid.spring.data.BaseConnectionFactory;
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
    private PlatformTransactionManagerAdapter platformTransactionManagerAdapter = new PlatformTransactionManagerAdapter();
    private ConcurrentHashMap<String, ConnectionFactoryProvider<?>> connectionFactoryProviders = new ConcurrentHashMap<String, ConnectionFactoryProvider<?>>();

    public TeiidServer() {
        this.cmr = new SBConnectorManagerRepository();
    }

    @SuppressWarnings("rawtypes")
    public void addDataSource(VDBMetaData vdb, String sourceBeanName, Object source, ApplicationContext context) {
        // only when user did not define a explicit VDB then build one.
        if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {
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
                    model = buildModelFromDataSource(sourceBeanName, driverName, context,
                            redirectUpdates && sourceBeanName.equals(redirectedDSName));
                } catch (AdminException e) {
                    throw new IllegalStateException("Error adding the source, cause: " + e.getMessage());
                }
            } else if (source instanceof BaseConnectionFactory) {
                model = buildModelFromConnectionFactory(sourceBeanName, (BaseConnectionFactory) source, context);
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
            addTranslator(type, context);
            addTranslator(translator.getName(), type, translator.getPropertiesMap());
        } catch (TranslatorException e) {
            throw new IllegalStateException("Failed to load translator " + translator.getName(), e);
        }
    }

    void addTranslator(String translatorname, ApplicationContext context) {
        try {
            if (this.getExecutionFactory(translatorname) == null) {
                String basePackage = getBasePackage(context);
                Class<? extends ExecutionFactory<?, ?>> clazz = ExternalSource.translatorClass(translatorname,
                        basePackage);
                if (clazz != null) {
                    addTranslator(clazz);
                } else {
                    throw new IllegalStateException("Failed to load translator " + translatorname
                            + ". Check to make sure @Translator annotation is added on your custom translator "
                            + "and also set the 'spring.teiid.model.package' set to package where the translator "
                            + "is defined");
                }
            }
        } catch (ConnectorManagerException | TranslatorException e) {
            throw new IllegalStateException("Failed to load translator " + translatorname, e);
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
            if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {
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
                VDBMetadataParser.marshell(vdb, out);
                logger.debug("XML Form of VDB:\n" + prettyFormat(new String(out.toByteArray())));
            }

            // add any missing translators
            for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
                for (SourceMappingMetadata smm : model.getSourceMappings()) {
                    VDBTranslatorMetaData translator = vdb.getTranslator(smm.getTranslatorName());
                    if (translator != null) {
                        addOverrideTranslator(translator, context);
                    } else {
                        addTranslator(smm.getTranslatorName(), context);
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
            deployVDB(vdb, null);
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

    private ModelMetaData buildModelFromDataSource(String dsBeanName, String driverName, ApplicationContext context,
            boolean createInitTable) throws AdminException {

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

        String translatorName = ExternalSource.findTransaltorNameFromDriverName(driverName);
        source.setTranslatorName(translatorName);
        String dialect = ExternalSource.findDialectFromDriverName(driverName);
        if (dialect != null) {
            model.addProperty(DIALECT, dialect);
        }

        // load the translator class
        addTranslator(translatorName, context);

        // add the importer properties from the configuration
        // note that above defaults can be overridden with this too.
        Collection<? extends PropertyDefinition> importProperties = getAdmin()
                .getTranslatorPropertyDefinitions(source.getTranslatorName(), TranlatorPropertyType.IMPORT);
        importProperties.forEach(prop -> {
            String key = prop.getName();
            String value = context.getEnvironment().getProperty("spring.datasource." + dsBeanName + "." + key);
            if (value == null) {
                value = context.getEnvironment().getProperty("spring.xa.datasource." + dsBeanName + "." + key);
            }
            if (value != null) {
                model.addProperty(key, value);
            }
        });

        model.addSourceMapping(source);

        // This is to avoid failing on empty schema
        if (createInitTable) {
            model.addSourceMetadata("NATIVE", "");
            model.addSourceMetadata("DDL", "create foreign table dual(id integer);");
        }
        return model;
    }

    @SuppressWarnings("rawtypes")
    private ModelMetaData buildModelFromConnectionFactory(String sourceBeanName, BaseConnectionFactory factory,
            ApplicationContext context) {
        ModelMetaData model = new ModelMetaData();
        model.setName(sourceBeanName);
        model.setModelType(Model.Type.PHYSICAL);

        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(sourceBeanName);
        source.setConnectionJndiName(sourceBeanName);

        String translatorName = ExternalSource.findTransaltorNameFromAlias(factory.getTranslatorName());
        source.setTranslatorName(translatorName);
        try {
            if (this.getExecutionFactory(translatorName) == null) {
                addTranslator(translatorName, context);
            }
        } catch (ConnectorManagerException e) {
            throw new IllegalStateException("Failed to load translator " + translatorName, e);
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

        String basePackage = getBasePackage(context);

        // check to add any source models first based on the annotations
        boolean load = false;
        Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
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
                    new TextTableView(metadata).buildView(clazz, mf, textAnnotation);
                } else if (jsonAnnotation != null && entityAnnotation != null) {
                    new JsonTableView(metadata).buildView(clazz, mf, jsonAnnotation);
                } else if (selectAnnotation != null && entityAnnotation != null) {
                    new SimpleView(metadata).buildView(clazz, mf, selectAnnotation);
                } else if (excelAnnotation != null && entityAnnotation != null) {
                    new ExcelTableView(metadata).buildView(clazz, mf, excelAnnotation);
                } else if (udfAnnotation != null) {
                    udfProcessor.buildFunctions(clazz, mf, udfAnnotation);
                } else if (selectAnnotation == null && entityAnnotation != null) {
                    new EntityBaseView(metadata, vdb, this).buildView(clazz, mf, entityAnnotation);
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
                    vdb.addModel(buildModelFromDataSource(redirectedDSName, driverName, context, false));
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

    private String getBasePackage(ApplicationContext context) {
        String basePackage = context.getEnvironment().getProperty(TeiidConstants.ENTITY_SCAN_DIR);
        if (basePackage == null) {
            logger.warn("***************************************************************");
            logger.warn("\"" + TeiidConstants.ENTITY_SCAN_DIR
                    + "\" is NOT set, scanning entire classpath for @Entity classes.");
            logger.warn("consider setting this property to avoid time consuming scanning");
            logger.warn("***************************************************************");
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
        source.setConnectionJndiName("file");
        source.setTranslatorName(ExternalSource.EXCEL.getTranslatorName());
        try {
            if (this.getExecutionFactory(ExternalSource.EXCEL.getTranslatorName()) == null) {
                addTranslator(ExternalSource.EXCEL.getTranslatorName(), context);
            }
        } catch (ConnectorManagerException e) {
            throw new IllegalStateException("Failed to load translator " + ExternalSource.EXCEL.getTranslatorName(), e);
        }
        model.addSourceMapping(source);

        vdb.addModel(model);
    }

    public Schema getSchema(String modelName) {
        VDBMetaData vdb = getVDBRepository().getVDB(VDBNAME, VDBVERSION); // $NON-NLS-1$
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
}
