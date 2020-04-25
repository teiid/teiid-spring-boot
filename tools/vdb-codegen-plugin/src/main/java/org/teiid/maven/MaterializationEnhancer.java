/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.teiid.maven;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.Database;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Server;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;

public class MaterializationEnhancer {
    static final String CACHE_STORE = "cacheStore";

    private String type;
    private Log log;

    public MaterializationEnhancer(String type, Log log) {
        this.type = type;
        this.log = log;
    }

    public boolean isMaterializationRequired(PluginDatabaseStore databaseStore) {
        Database database = databaseStore.db();
        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            for (Table table : schema.getTables().values()) {
                if (table.isMaterialized() && table.getMaterializedTable() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addSchema(PluginDatabaseStore databaseStore, File resourcesDir) throws Exception {
        Database database = databaseStore.db();
        String schemaName = "materialized";
        Schema matSchema = new Schema();
        matSchema.setName(schemaName);
        matSchema.setPhysical(true);
        Server server = new Server(CACHE_STORE);
        server.setDataWrapper(this.type);
        matSchema.addServer(server);
        database.addSchema(matSchema);
        database.addServer(server);

        MetadataFactory factory = new MetadataFactory(database.getName(), database.getVersion(), schemaName,
                database.getMetadataStore().getDatatypes(), null, null);

        // if not infinispan based materialization add table directly
        Table statusTable = buildStatusTable(factory);
        matSchema.addTable(statusTable);

        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            for (Table table : schema.getTables().values()) {
                if (table.isMaterialized() && table.getMaterializedTable() == null) {
                    Table matTable = cloneTable(factory, table);
                    matSchema.addTable(matTable);

                    // set auto-management
                    table.setProperty("MATERIALIZED_TABLE", matTable.getFullName());
                    table.setProperty("teiid_rel:ALLOW_MATVIEW_MANAGEMENT", "true");
                    table.setProperty("teiid_rel:MATVIEW_LOADNUMBER_COLUMN","LoadNumber");
                    table.setProperty("teiid_rel:MATVIEW_STATUS_TABLE", statusTable.getFullName());
                }
            }
        }

        // Write the DDL out, however for the Infinispan we need to generate the
        databaseStore.importSchema(schemaName, null, CACHE_STORE, null, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyMap());
        ImportSchemaAwareDDLStringVisitor visitor = new ImportSchemaAwareDDLStringVisitor(databaseStore, null, null);
        visitor.visit(databaseStore.db());
        String vdbDDL = visitor.toString();
        this.log.info("Materialization based VDB: " + vdbDDL);

        // write the materialized database file, this should be the one runtime should deploy
        File file = new File(resourcesDir, schemaName + ".ddl");
        this.log.info("Materialization Written to : " + file.getAbsolutePath());
        FileWriter fw = new FileWriter(file);
        fw.write(vdbDDL);
        fw.close();
    }

    private Table buildStatusTable(MetadataFactory factory) {
        Table tbl = factory.addTable(factory.getVdbName()+"_status");
        Column c = factory.addColumn("VDBName","string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(50);

        c = factory.addColumn("VDBVersion", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(50);

        c = factory.addColumn("SchemaName", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(50);

        c = factory.addColumn("Name", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(256);

        c = factory.addColumn("TargetSchemaName", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(50);

        c = factory.addColumn("TargetName", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(256);

        c = factory.addColumn("Valid", "boolean", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(50);

        c = factory.addColumn("LoadState", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(25);

        c = factory.addColumn("Cardinality", "long", tbl);

        c = factory.addColumn("Updated", "timestamp", tbl);
        c.setNullType(NullType.No_Nulls);

        c = factory.addColumn("LoadNumber", "long", tbl);
        c.setNullType(NullType.No_Nulls);

        c = factory.addColumn("NodeName", "string", tbl);
        c.setNullType(NullType.No_Nulls);
        c.setLength(25);

        c = factory.addColumn("StaleCount", "long", tbl);

        factory.addPrimaryKey("PK", Arrays.asList(new String[] {"VDBName", "VDBVersion", "SchemaName", "Name"}), tbl);
        tbl.setSupportsUpdate(true);

        for (Column col : tbl.getColumns()) {
            col.setUpdatable(true);
        }

        // if this is going to Infinispan assign the cache name
        if (this.type.equalsIgnoreCase(VdbCodeGeneratorMojo.ISPN)) {
            tbl.setProperty("teiid_ispn:cache", tbl.getName());
        }
        return tbl;
    }

    private Table cloneTable(MetadataFactory factory, Table table) throws Exception {
        Schema tempSchema = new Schema();
        tempSchema.setName("temp");
        tempSchema.addTable(table);

        Database db = new Database("temp");
        db.addSchema(tempSchema);
        String ddl = DDLStringVisitor.getDDLString(db);
        Map<String, Datatype> typeMap = SystemMetadata.getInstance().getRuntimeTypeMap();
        PluginDatabaseStore store = new PluginDatabaseStore(typeMap);
        store.parse(ddl);
        db = store.db();

        Table matTable = db.getSchema("temp").getTable(table.getName());
        matTable.setVirtual(false);
        matTable.setMaterialized(false);
        matTable.setSupportsUpdate(true);
        matTable.setName(factory.getVdbName()+"_"+table.getName());
        factory.addColumn("LoadNumber", "long", matTable);
        //matTable.setUUID(UUID.randomUUID().toString());

        for (Column c : matTable.getColumns()) {
            c.setUpdatable(true);
        }

        // remove any matview specific properties that carried over from the original table
        for (String key : matTable.getProperties().keySet()) {
            if (key.startsWith("teiid_rel:MATVIEW")) {
                matTable.setProperty(key, null);
            }
        }
        // if this is going to Infinispan assign the cache name
        if (this.type.equalsIgnoreCase(VdbCodeGeneratorMojo.ISPN)) {
            matTable.setProperty("teiid_ispn:cache", matTable.getName());
        }
        return matTable;
    }
}
