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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
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
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.TranslatorException;

public class TeiidServer extends EmbeddedServer {
    private static final Log logger = LogFactory.getLog(TeiidServer.class);
    
    public void addDataSource(VDBMetaData vdb, String sourceName, Object source, ApplicationContext context) {
        // only when user did not define a explicit VDB then build one.
        if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {
            
            addConnectionFactory(sourceName, source);
            
            ModelMetaData model = null;            
            if (source instanceof org.apache.tomcat.jdbc.pool.DataSource) {
                try {
                    model = buildModelFromDataSource(sourceName,
                            (org.apache.tomcat.jdbc.pool.DataSource) source, context);
                } catch (AdminException e) {
                    throw new IllegalStateException("Error adding the source, cause: "+e.getMessage());
                }
            } else if (source instanceof BaseConnectionFactory) {
                model = buildModelFromConnectionFactory(sourceName, (BaseConnectionFactory)source);
            } else {
                throw new IllegalStateException(
                        "Auto detecting of sources currently only supports Tomcat Datasource");
            }

            if (model != null) {
                vdb.addModel(model);
                logger.info("Added "+sourceName+" to the Teiid Database");
            }
            
            // since each time a data source is added the vdb is reloaded
            // this is a cheap way not to do the reload of the metadata from source. a.k.a metadata caching
            try {
                final Admin admin = getAdmin();
                VDBMetaData previous = (VDBMetaData)admin.getVDB(VDBNAME, VDBVERSION);
                for (Map.Entry<String, ModelMetaData> entry:previous.getModelMetaDatas().entrySet()) {
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
            for (ModelMetaData model:vdb.getModelMetaDatas().values()) {
                for (SourceMappingMetadata smm : model.getSourceMappings()) {
                    if (smm.getConnectionJndiName().equalsIgnoreCase(sourceName)) {
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
            throw new IllegalStateException("Failed to deploy the VDB file, cause: "+e.getMessage());
        }
    }    
    
    private ModelMetaData buildModelFromDataSource(String dsName, org.apache.tomcat.jdbc.pool.DataSource ds,
            ApplicationContext context) throws AdminException {

        // This teiid database, ignore 
        if (ds.getUrl().startsWith("jdbc:teiid:"+VDBNAME)) {
            return null;
        }
        
        ModelMetaData model = new ModelMetaData();
        model.setName(dsName);
        model.setModelType(Model.Type.PHYSICAL);
        
        // note these can be overridden by specific ones in the configuration
        // TODO: need come up most sensible properties for this.
        model.addProperty("importer.useQualifiedName", "false"); 
        model.addProperty("importer.tableTypes", "TABLE,VIEW");
        
        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(dsName);
        source.setConnectionJndiName(dsName);

        String driverName = ds.getDriverClassName();
        String translatorName = ExternalSource.findTransaltorNameFromDriverName(driverName);
        source.setTranslatorName(translatorName);
        try {
            if (this.getExecutionFactory(translatorName) == null) {
                addTranslator(ExternalSource.translatorClass(translatorName));
            }
        } catch (ConnectorManagerException | TranslatorException e) {
            throw new IllegalStateException("Failed to load translator "+ translatorName, e);
        } 
        
        // add the importer properties from the configuration
        // note that above defaults can be overridden with this too.
        Collection<? extends PropertyDefinition> importProperties = getAdmin()
                .getTranslatorPropertyDefinitions(source.getTranslatorName(), TranlatorPropertyType.IMPORT);
        importProperties.forEach(prop -> {
            String key = prop.getName();
            String value = context.getEnvironment().getProperty("spring.datasource."+dsName+"."+key);
            if (value != null) {
                model.addProperty(key, value); 
            }
        });
        
        model.addSourceMapping(source);
        return model;
    }


    public ModelMetaData buildModelFromConnectionFactory(String factoryName, BaseConnectionFactory factory) {
        ModelMetaData model = new ModelMetaData();
        model.setName(factoryName);
        model.setModelType(Model.Type.PHYSICAL);
        
        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName(factoryName);
        source.setConnectionJndiName(factoryName);
        
        String translatorName = ExternalSource.findTransaltorNameFromSourceName(factory.getSourceName());
        source.setTranslatorName(translatorName);
        try {
            if (this.getExecutionFactory(translatorName) == null) {
                addTranslator(ExternalSource.translatorClass(translatorName));
            }
        } catch (ConnectorManagerException | TranslatorException e) {
            throw new IllegalStateException("Failed to load translator "+ translatorName, e);
        } 
        
        model.addSourceMapping(source);
        
        return model;
    }    
}
