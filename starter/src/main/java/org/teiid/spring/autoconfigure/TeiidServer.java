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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.adminapi.Admin;
import org.teiid.adminapi.Admin.TranlatorPropertyType;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.PropertyDefinition;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.SourceMappingMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dialect.TeiidDialect;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Schema;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.annotations.ExcelTable;
import org.teiid.spring.annotations.JsonTable;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.TextTable;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.views.ExcelTableView;
import org.teiid.spring.views.JsonTableView;
import org.teiid.spring.views.SimpleView;
import org.teiid.spring.views.TextTableView;
import org.teiid.translator.TranslatorException;

public class TeiidServer extends EmbeddedServer {
	private static final Log logger = LogFactory.getLog(TeiidServer.class);

	public void addDataSource(VDBMetaData vdb, String sourceBeanName, Object source, ApplicationContext context) {
		// only when user did not define a explicit VDB then build one.
		if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {

			addConnectionFactory(sourceBeanName, source);

			ModelMetaData model = null;
			if (source instanceof org.apache.tomcat.jdbc.pool.DataSource) {
				try {
					model = buildModelFromDataSource(sourceBeanName, (org.apache.tomcat.jdbc.pool.DataSource) source,
							context);
				} catch (AdminException e) {
					throw new IllegalStateException("Error adding the source, cause: " + e.getMessage());
				}
			} else if (source instanceof BaseConnectionFactory) {
				model = buildModelFromConnectionFactory(sourceBeanName, (BaseConnectionFactory) source, context);
			} else {
				throw new IllegalStateException("Auto detecting of sources currently only supports Tomcat Datasource");
			}

			if (model != null) {
				model.setVisible(false);
				vdb.addModel(model);
				logger.info("Added " + sourceBeanName + " to the Teiid Database");
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

			undeployVDB(VDBNAME, VDBVERSION);
			deployVDB(vdb);
		} else {
			for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
				for (SourceMappingMetadata smm : model.getSourceMappings()) {
					if (smm.getConnectionJndiName().equalsIgnoreCase(sourceBeanName)) {
						addConnectionFactory(smm.getName(), source);
					}
				}
			}
		}
	}

	void deployVDB(VDBMetaData vdb) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			VDBMetadataParser.marshell(vdb, out);
			deployVDB(new ByteArrayInputStream(out.toByteArray()));
		} catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException | XMLStreamException
				| IOException e) {
			throw new IllegalStateException("Failed to deploy the VDB file", e);
		}
	}

	private ModelMetaData buildModelFromDataSource(String dsBeanName, org.apache.tomcat.jdbc.pool.DataSource ds,
			ApplicationContext context) throws AdminException {

		// This teiid database, ignore
		if (ds.getUrl().startsWith("jdbc:teiid:" + VDBNAME)) {
			return null;
		}

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

		String driverName = ds.getDriverClassName();
		String translatorName = ExternalSource.findTransaltorNameFromDriverName(driverName);
		source.setTranslatorName(translatorName);
		try {
			if (this.getExecutionFactory(translatorName) == null) {
				addTranslator(ExternalSource.translatorClass(translatorName));
			}
		} catch (ConnectorManagerException | TranslatorException e) {
			throw new IllegalStateException("Failed to load translator " + translatorName, e);
		}

		// add the importer properties from the configuration
		// note that above defaults can be overridden with this too.
		Collection<? extends PropertyDefinition> importProperties = getAdmin()
				.getTranslatorPropertyDefinitions(source.getTranslatorName(), TranlatorPropertyType.IMPORT);
		importProperties.forEach(prop -> {
			String key = prop.getName();
			String value = context.getEnvironment().getProperty("spring.datasource." + dsBeanName + "." + key);
			if (value != null) {
				model.addProperty(key, value);
			}
		});

		model.addSourceMapping(source);
		return model;
	}

	public ModelMetaData buildModelFromConnectionFactory(String sourceBeanName, BaseConnectionFactory factory,
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
				addTranslator(ExternalSource.translatorClass(translatorName));
			}
		} catch (ConnectorManagerException | TranslatorException e) {
			throw new IllegalStateException("Failed to load translator " + translatorName, e);
		}

		model.addSourceMapping(source);
		return model;
	}

	boolean findAndConfigureViews(VDBMetaData vdb, ApplicationContext context, PhysicalNamingStrategy namingStrategy) {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Entity.class));
		provider.addIncludeFilter(new AnnotationTypeFilter(SelectQuery.class));

		String basePackage = context.getEnvironment().getProperty(TeiidConstants.ENTITY_SCAN_DIR);
		if (basePackage == null) {
			return false;
		}
		Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
		if (components.isEmpty()) {
			return false;
		}

		// check to add any source models first based on the annotations
		boolean load = false;
		for (BeanDefinition c : components) {
			try {
				Class<?> clazz = Class.forName(c.getBeanClassName());
				ExcelTable excelAnnotation = clazz.getAnnotation(ExcelTable.class);

				if (excelAnnotation != null) {
					addExcelModel(vdb, clazz, excelAnnotation);
					load = true;
				}
			} catch (ClassNotFoundException e) {
				logger.warn("Error loading entity classes");
			}
		}

		ModelMetaData model = new ModelMetaData();
		model.setName("teiid");
		model.setModelType(Model.Type.VIRTUAL);
		MetadataFactory mf = new MetadataFactory(VDBNAME, VDBVERSION, SystemMetadata.getInstance().getRuntimeTypeMap(),
				model);
		
		Metadata metadata = getMetadata(components, namingStrategy);
		for (BeanDefinition c : components) {
			try {
				Class<?> clazz = Class.forName(c.getBeanClassName());
				SelectQuery selectAnnotation = clazz.getAnnotation(SelectQuery.class);
				TextTable textAnnotation = clazz.getAnnotation(TextTable.class);
				JsonTable jsonAnnotation = clazz.getAnnotation(JsonTable.class);
				ExcelTable excelAnnotation = clazz.getAnnotation(ExcelTable.class);

				if (textAnnotation != null) {
					new TextTableView(metadata).buildView(clazz, mf, textAnnotation);
				} else if (jsonAnnotation != null) {
					new JsonTableView(metadata).buildView(clazz, mf, jsonAnnotation);
				} else if (selectAnnotation != null) {
					new SimpleView(metadata).buildView(clazz, mf, selectAnnotation);
				} else if (excelAnnotation != null) {
					new ExcelTableView(metadata).buildView(clazz, mf, excelAnnotation);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("Error loading entity classes");
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

	private Metadata getMetadata(Set<BeanDefinition> components, PhysicalNamingStrategy namingStrategy) {
		MetadataSources metadataSources = new MetadataSources();
		for (BeanDefinition c : components) {
			try {
				Class<?> clazz = Class.forName(c.getBeanClassName());
				metadataSources.addAnnotatedClass(clazz);
			} catch (ClassNotFoundException e) {
			}
		}		
		ServiceRegistry registry = metadataSources.getServiceRegistry();
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(
				(BootstrapServiceRegistry) registry).applySetting(AvailableSettings.DIALECT, TeiidDialect.class)
						.build();
		Metadata metadata = metadataSources.getMetadataBuilder(serviceRegistry)
				.applyPhysicalNamingStrategy(namingStrategy).build();
		return metadata;
	}	
	
	private void addExcelModel(VDBMetaData vdb, Class<?> clazz, ExcelTable excelAnnotation) {
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
				addTranslator(ExternalSource.translatorClass(ExternalSource.EXCEL.getTranslatorName()));
			}
		} catch (ConnectorManagerException | TranslatorException e) {
			throw new IllegalStateException("Failed to load translator " + ExternalSource.EXCEL.getTranslatorName(), e);
		}
		model.addSourceMapping(source);

		vdb.addModel(model);
	}

	public Schema getSchema(String modelName) {
		VDBMetaData vdb = getVDBRepository().getVDB(VDBNAME, VDBVERSION); //$NON-NLS-1$
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
}
