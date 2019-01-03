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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.metadata.Schema;
import org.teiid.translator.TypeFacility;

class RedirectionSchemaInitializer extends MultiDataSourceInitializer {
    private static final Log logger = LogFactory.getLog(RedirectionSchemaInitializer.class);

    private Dialect dialect;
    private Schema schema;
    private Metadata metadata;
    private ServiceRegistry registry;

    RedirectionSchemaInitializer(DataSource dataSource, String sourceName, Dialect dialect, Metadata metadata,
            ServiceRegistry registry, Schema schema, ApplicationContext applicationContext) {
        super(dataSource, sourceName, applicationContext);
        this.dialect = dialect;
        this.metadata = metadata;
        this.schema = schema;
        this.registry = registry;
    }

    @Override
    List<Resource> getScripts(String propertyName, List<String> resources, String fallback) {
        List<Resource> found = super.getScripts(propertyName, resources, fallback);
        String key = "spring.datasource." + sourceName + ".schema";
        if (key.equals(propertyName)) {
            // if scripts are found do not run them again, as they would have executed first
            // time data source
            // is registered.
            if (!found.isEmpty()) {
                return Collections.emptyList();
            }
            found = generatedScripts();
        }
        return found;
    }

    List<Resource> generatedScripts() {
        List<Resource> resources = Collections.emptyList();

        for (PersistentClass clazz : metadata.getEntityBindings()) {
            org.hibernate.mapping.Table ormTable = clazz.getTable();
            String tableName = ormTable.getQuotedName();
            if (this.schema.getTable(tableName) != null) {
                org.hibernate.mapping.Column c = new org.hibernate.mapping.Column(
                        RedirectionSchemaBuilder.ROW_STATUS_COLUMN);
                c.setSqlTypeCode(TypeFacility.getSQLTypeFromRuntimeType(Integer.class));
                c.setSqlType(JDBCSQLTypeInfo.getTypeName(TypeFacility.getSQLTypeFromRuntimeType(Integer.class)));
                ormTable.addColumn(c);
                ormTable.setName(tableName + TeiidConstants.REDIRECTED_TABLE_POSTFIX);
            }
        }

        List<String> statements = createScript(metadata, dialect, true);
        StringBuilder sb = new StringBuilder();
        for (String s : statements) {
            // we have no need for sequences in the redirected scenario, they are fed from
            // other side.
            if (s.startsWith("drop sequence") || s.startsWith("create sequence")) {
                continue;
            }
            sb.append(s).append(";\n");
        }
        logger.debug("Redirected Schema:\n" + sb.toString());
        resources = Arrays.asList(new ByteArrayResource(sb.toString().getBytes()));
        return resources;
    }

    List<String> createScript(Metadata metadata, Dialect d, boolean includeDrops) {
        final JournalingGenerationTarget target = new JournalingGenerationTarget();

        final ExecutionOptions options = new ExecutionOptions() {
            @Override
            public boolean shouldManageNamespaces() {
                return false;
            }

            @Override
            public Map getConfigurationValues() {
                return Collections.emptyMap();
            }

            @Override
            public ExceptionHandler getExceptionHandler() {
                return ExceptionHandlerHaltImpl.INSTANCE;
            }
        };
        HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
        tool.injectServices((ServiceRegistryImplementor) this.registry);
        SourceDescriptor sd = new SourceDescriptor() {
            @Override
            public SourceType getSourceType() {
                return SourceType.METADATA;
            }

            @Override
            public ScriptSourceInput getScriptSourceInput() {
                return null;
            }
        };
        if (includeDrops) {
            new SchemaDropperImpl(tool).doDrop(metadata, options, d, sd, target);
        }
        new SchemaCreatorImpl(tool).doCreation(metadata, d, options, sd, target);
        return target.commands;
    }

    private static class JournalingGenerationTarget implements GenerationTarget {
        private final ArrayList<String> commands = new ArrayList<String>();

        @Override
        public void prepare() {
        }

        @Override
        public void accept(String command) {
            commands.add(command);
        }

        @Override
        public void release() {
        }
    }
}
