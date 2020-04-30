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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.teiid.adminapi.Admin.SchemaObjectType;
import org.teiid.metadata.Schema;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.sql.symbol.Constant;
import org.teiid.query.sql.visitor.SQLStringVisitor;

public class ImportSchemaAwareDDLStringVisitor extends DDLStringVisitor {
    private PluginDatabaseStore store;

    public ImportSchemaAwareDDLStringVisitor(PluginDatabaseStore store, EnumSet<SchemaObjectType> types,
            String regexPattern) {
        super(types, regexPattern);
        this.store = store;
    }

    @Override
    protected void visit(Schema schema) {
        super.visit(schema);
        if (schema.isPhysical()) {
            List<PluginDatabaseStore.ImportSchema> importSchemas =  this.store.getImportSchemas(schema.getName());
            if (importSchemas == null) {
                return;
            }
            for (PluginDatabaseStore.ImportSchema importSchema : importSchemas) {
                if (importSchema != null) {
                    //IMPORT FOREIGN SCHEMA public FROM SERVER sampledb INTO accounts OPTIONS("importer.useFullSchemaName" 'false');
                    append("IMPORT ");
                    if (importSchema.foreignSchemaName != null) {
                        append("FOREIGN SCHEMA ");
                        append(SQLStringVisitor.escapeSinglePart(importSchema.foreignSchemaName));
                        append(" ");
                    }
                    append("FROM SERVER ");
                    append(SQLStringVisitor.escapeSinglePart(importSchema.serverName));
                    append(" INTO ");
                    append(SQLStringVisitor.escapeSinglePart(importSchema.schemaName));
                    if (!importSchema.includeTables.isEmpty() || !importSchema.excludeTables.isEmpty()
                            || !importSchema.properties.isEmpty()) {
                        append(" OPTIONS( ");
                        boolean useComma = false;
                        if (!importSchema.includeTables.isEmpty()) {
                            append("\"importer.includeTables\" '");
                            append(String.join(",", importSchema.includeTables));
                            append("'");
                            useComma = true;
                        }
                        if (!importSchema.excludeTables.isEmpty()) {
                            if (useComma) {
                                append(", ");
                            }
                            append("\"importer.excludeTables\" '");
                            append(String.join(",", importSchema.excludeTables));
                            append("'");
                            useComma = true;
                        }
                        if (!importSchema.properties.isEmpty()) {
                            if (useComma) {
                                append(", ");
                            }
                            int i = 0;
                            for (Map.Entry<String, String> entry : importSchema.properties.entrySet()) {
                                if (i > 0) {
                                    append(", ");
                                }
                                append(SQLStringVisitor.escapeSinglePart(entry.getKey()));
                                append(" ");
                                Object value = entry.getValue();
                                if (value != null) {
                                    value = new Constant(value);
                                } else {
                                    value = Constant.NULL_CONSTANT;
                                }
                                append(value);
                                i++;
                            }
                        }
                        append(")");
                    }
                    append(";\n");
                }
            }
        }
    }
}
