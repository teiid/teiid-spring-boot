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

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JDBCBinder;
import org.hibernate.cfg.reveng.DatabaseCollector;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.MappingsDatabaseCollector;
import org.hibernate.cfg.reveng.ReverseEngineeringRuntimeInfo;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.TableIdentifier;
import org.hibernate.cfg.reveng.dialect.JDBCMetaDataDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2x.ArtifactCollector;
import org.hibernate.tool.hbm2x.HibernateMappingExporter;
import org.hibernate.type.Type;
import org.springframework.context.ApplicationContext;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.dialect.TeiidDialect;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.ForeignKey;
import org.teiid.metadata.KeyRecord;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.views.EntityBaseView;

public class SchemaBuilderUtility {
    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir")+"/teiid");

    public void generateVBLSchema(ApplicationContext context, MetadataFactory source, MetadataFactory target,
            Dialect dialect, MetadataSources metadataSources) {
        generateVBLSchema(source, target);
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(
                (BootstrapServiceRegistry) metadataSources.getServiceRegistry())
                .applySetting(AvailableSettings.DIALECT, dialect).build();
        ArtifactCollector files = generateHibernateModel(source, serviceRegistry);
        for (File f : files.getFiles("hbm.xml")) {
            metadataSources.addFile(f);
        }
    }

    public void generateVBLSchema(MetadataFactory source, MetadataFactory target) {

        for (Table srcTable : source.getSchema().getTables().values()) {
            Table table = target.addTable(srcTable.getName());
            for (Column srcColumn : srcTable.getColumns()) {
                Column c = target.addColumn(srcColumn.getName(), srcColumn.getRuntimeType(), table);
                c.setUpdatable(true);
                if (srcColumn.isAutoIncremented()) {
                    c.setAutoIncremented(true);
                }
                c.setLength(srcColumn.getLength());
                c.setScale(srcColumn.getScale());
                c.setPrecision(srcColumn.getPrecision());
                c.setNullType(srcColumn.getNullType());
                c.setDefaultValue(srcColumn.getDefaultValue());
            }

            table.setVirtual(true);
            table.setSupportsUpdate(true);

            if (srcTable.getPrimaryKey() != null) {
                target.addPrimaryKey(srcTable.getPrimaryKey().getName(),
                        RedirectionSchemaBuilder.getColumnNames(srcTable.getPrimaryKey().getColumns()), table);
            }

            if (!srcTable.getForeignKeys().isEmpty()) {
                for (ForeignKey fk : srcTable.getForeignKeys()) {
                    target.addForeignKey(fk.getName(), RedirectionSchemaBuilder.getColumnNames(fk.getColumns()),
                            fk.getReferenceColumns(), fk.getReferenceTableName(), table);
                }
            }

            if (!srcTable.getUniqueKeys().isEmpty()) {
                for (KeyRecord kr : srcTable.getUniqueKeys()) {
                    target.addIndex(kr.getName(), false, RedirectionSchemaBuilder.getColumnNames(kr.getColumns()),
                            table);
                }
            }

            if (!srcTable.getIndexes().isEmpty()) {
                for (KeyRecord kr : srcTable.getIndexes()) {
                    target.addIndex(kr.getName(), true, RedirectionSchemaBuilder.getColumnNames(kr.getColumns()),
                            table);
                }
            }

            table.setSelectTransformation(EntityBaseView.buildSelectPlan(table, source.getSchema().getName()));
        }
    }

    // this is not used currently
    public static Metadata generateHbmModel(ConnectionProvider provider, Dialect dialect) throws SQLException {
        MetadataSources metadataSources = new MetadataSources();
        ServiceRegistry registry = metadataSources.getServiceRegistry();

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(
                (BootstrapServiceRegistry) registry).applySetting(AvailableSettings.DIALECT, TeiidDialect.class)
                .addService(ConnectionProvider.class, provider).addService(JdbcEnvironment.class,
                        new JdbcEnvironmentImpl(provider.getConnection().getMetaData(), dialect))
                .build();

        MetadataBuildingOptions options = new MetadataBuildingOptionsImpl(serviceRegistry);
        BootstrapContext bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );

        ReverseEngineeringStrategy strategy = new DefaultReverseEngineeringStrategy();

        InFlightMetadataCollectorImpl metadataCollector =  new InFlightMetadataCollectorImpl(bootstrapContext, options);
        MetadataBuildingContext buildingContext = new MetadataBuildingContextRootImpl(bootstrapContext, options,
                metadataCollector);

        JDBCBinder binder = new JDBCBinder(serviceRegistry, new Properties(), buildingContext, strategy, false);
        Metadata metadata = metadataCollector.buildMetadataInstance(buildingContext);
        binder.readFromDatabase(null, null, buildMapping(metadata));
        HibernateMappingExporter exporter = new HibernateMappingExporter() {
            @Override
            public Metadata getMetadata() {
                return metadata;
            }
        };
        exporter.start();
        return metadata;
    }

    private static Mapping buildMapping(final Metadata metadata) {
        return new Mapping() {
            /**
             * Returns the identifier type of a mapped class
             */
            @Override
            public Type getIdentifierType(String persistentClass) throws MappingException {
                final PersistentClass pc = metadata.getEntityBinding(persistentClass);
                if (pc == null) {
                    throw new MappingException("persistent class not known: " + persistentClass);
                }
                return pc.getIdentifier().getType();
            }

            @Override
            public String getIdentifierPropertyName(String persistentClass) throws MappingException {
                final PersistentClass pc = metadata.getEntityBinding(persistentClass);
                if (pc == null) {
                    throw new MappingException("persistent class not known: " + persistentClass);
                }
                if (!pc.hasIdentifierProperty()) {
                    return null;
                }
                return pc.getIdentifierProperty().getName();
            }

            @Override
            public Type getReferencedPropertyType(String persistentClass, String propertyName) throws MappingException {
                final PersistentClass pc = metadata.getEntityBinding(persistentClass);
                if (pc == null) {
                    throw new MappingException("persistent class not known: " + persistentClass);
                }
                Property prop = pc.getProperty(propertyName);
                if (prop == null) {
                    throw new MappingException("property not known: " + persistentClass + '.' + propertyName);
                }
                return prop.getType();
            }

            @Override
            public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
                return null;
            }
        };
    }

    static class TeiidJDBCBinder extends JDBCBinder {
        private InFlightMetadataCollectorImpl metadataCollector;
        private MetadataFactory source;
        private ReverseEngineeringStrategy strategy;

        TeiidJDBCBinder(ServiceRegistry serviceRegistry, Properties properties, MetadataBuildingContext mdbc,
                ReverseEngineeringStrategy revengStrategy, boolean preferBasicCompositeIds, InFlightMetadataCollectorImpl metadataCollector, MetadataFactory source) {
            super(serviceRegistry, properties, mdbc, revengStrategy, preferBasicCompositeIds);
            this.metadataCollector = metadataCollector;
            this.source = source;
            this.strategy = revengStrategy;
        }

        @Override
        public DatabaseCollector readDatabaseSchema(String catalog, String schema) throws SQLException {
            DatabaseCollector dbs = new MappingsDatabaseCollector(metadataCollector, new JDBCMetaDataDialect());

            Map<String, org.hibernate.mapping.Table> foundTables = new java.util.HashMap<>();
            for (Table table : source.getSchema().getTables().values()) {
                org.hibernate.mapping.Table ht = metadataCollector.addTable(null,
                        null, table.getName(), null, false);
                dbs.addTable(null, null, table.getName());

                // Add columns
                for (Column column : table.getColumns()) {
                    org.hibernate.mapping.Column hc = new org.hibernate.mapping.Column();
                    hc.setName(column.getName());
                    hc.setSqlTypeCode(Integer.valueOf(JDBCSQLTypeInfo.getSQLType(column.getRuntimeType())));
                    hc.setLength(column.getLength());
                    hc.setPrecision(column.getPrecision());
                    hc.setScale(column.getScale());
                    hc.setNullable(column.getNullType() == NullType.Nullable);
                    ht.addColumn(hc);
                }

                // Add primary key
                KeyRecord pk = table.getPrimaryKey();
                if (pk != null) {
                    PrimaryKey hpk = new PrimaryKey(ht);
                    hpk.setName(pk.getName());
                    for (Column column : pk.getColumns()) {
                        hpk.addColumn(ht.getColumn(new org.hibernate.mapping.Column(column.getName())));
                    }
                    ht.setPrimaryKey(hpk);
                }

                // add unique keys, indexes etc.
                if (!table.getUniqueKeys().isEmpty()) {
                    for (KeyRecord key : table.getUniqueKeys()) {
                        UniqueKey uk = new UniqueKey();
                        uk.setName(key.getName());
                        uk.setTable(ht);
                        ht.addUniqueKey(uk);
                        for (Column column : key.getColumns()) {
                            uk.addColumn(ht.getColumn(new org.hibernate.mapping.Column(column.getName())));
                        }
                    }
                }

                if (!table.getIndexes().isEmpty()) {
                    for (KeyRecord key : table.getIndexes()) {
                        Index idx = new Index();
                        idx.setName(key.getName());
                        idx.setTable(ht);
                        ht.addIndex(idx);
                        for (Column column : key.getColumns()) {
                            idx.addColumn(ht.getColumn(new org.hibernate.mapping.Column(column.getName())));
                        }
                    }
                }

                foundTables.put(table.getName(), ht);
            }

            // Add foreign keys
            Map<String, List<org.hibernate.mapping.ForeignKey>> oneToManyCandidates = new HashMap<String, List<org.hibernate.mapping.ForeignKey>>();
            for (Table table : source.getSchema().getTables().values()) {
                org.hibernate.mapping.Table ht = foundTables.get(table.getName());

                for (ForeignKey fk : table.getForeignKeys()) {
                    org.hibernate.mapping.Table refht = foundTables.get(fk.getReferenceTableName());
                    List<org.hibernate.mapping.Column> columns = new ArrayList<>();
                    List<org.hibernate.mapping.Column> refColumns = new ArrayList<>();
                    for (Column column : fk.getColumns()) {
                        columns.add(ht.getColumn(new org.hibernate.mapping.Column(column.getName())));
                    }
                    for (String column : fk.getReferenceColumns()) {
                        refColumns.add(refht.getColumn(new org.hibernate.mapping.Column(column)));
                    }
                    String className = strategy.tableToClassName(TableIdentifier.create(ht));
                    org.hibernate.mapping.ForeignKey key = ht.createForeignKey(fk.getName(), columns, className, null, refColumns);
                    key.setReferencedTable(refht);
                    List<org.hibernate.mapping.ForeignKey> existing = oneToManyCandidates.get(className);
                    if(existing == null) {
                        existing = new ArrayList<org.hibernate.mapping.ForeignKey>();
                        oneToManyCandidates.put(className, existing);
                    }
                    existing.add(key);
                }
            }
            dbs.setOneToManyCandidates(oneToManyCandidates);
            strategy.configure(ReverseEngineeringRuntimeInfo.createInstance(null, null, dbs));
            return dbs;
        }
    }

    public static ArtifactCollector generateHibernateModel(MetadataFactory source,
            StandardServiceRegistry serviceRegistry) {
        ReverseEngineeringStrategy strategy = new DefaultReverseEngineeringStrategy();
        MetadataBuildingOptions options = new MetadataBuildingOptionsImpl(serviceRegistry);

        BootstrapContext bootstrapContext = new BootstrapContextImpl(serviceRegistry, options);

        InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(bootstrapContext, options);
        MetadataBuildingContext buildingContext = new MetadataBuildingContextRootImpl(bootstrapContext, options,
                metadataCollector);

        TeiidJDBCBinder binder = new TeiidJDBCBinder(serviceRegistry, new Properties(), buildingContext, strategy,
                false, metadataCollector, source);
        Metadata metadata = metadataCollector.buildMetadataInstance(buildingContext);
        binder.readFromDatabase(null, null, buildMapping(metadata));

        HibernateMappingExporter exporter = new HibernateMappingExporter() {
            @Override
            public Metadata getMetadata() {
                return metadata;
            }
        };
        exporter.setOutputDirectory(TMP_DIR);
        exporter.start();
        return exporter.getArtifactCollector();
    }
}
