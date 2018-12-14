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
package org.teiid.spring.views;

import javax.persistence.Entity;

import org.hibernate.boot.Metadata;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.metadata.Column;
import org.teiid.metadata.KeyRecord;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.spring.autoconfigure.RedirectionSchemaBuilder;
import org.teiid.spring.autoconfigure.TeiidServer;

/**
 * This view generates a base layer view for any source table.
 */
public class EntityBaseView extends ViewBuilder<Entity> {
    private VDBMetaData vdb;
    private TeiidServer server;

    public EntityBaseView(Metadata metadata, VDBMetaData vdb, TeiidServer server) {
        super(metadata);
        this.vdb = vdb;
        this.server = server;
    }

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, Entity annotation) {
        String sourceName = findSourceWhereEntityExists(view.getName());
        if (sourceName == null) {
            throw new IllegalStateException(
                    view.getName() + " not found in any datasource configured. " + "Failed to create view.");
        }

        view.setSelectTransformation(buildSelectPlan(view, sourceName));
        // view.setInsertPlan(buildInsertPlan(view, sourceName));
        // view.setUpdatePlan(buildUpdatePlan(view, sourceName));
        // view.setDeletePlan(buildDeletePlan(view, sourceName));
    }

    public static String buildDeletePlan(Table view, String sourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("FOR EACH ROW\n");
        sb.append("BEGIN ATOMIC\n");
        sb.append("DELETE FROM ").append(sourceName).append(".").append(view.getName());
        sb.append(" WHERE ");
        KeyRecord pk = RedirectionSchemaBuilder.getPK(view);
        for (int i = 0; i < pk.getColumns().size(); i++) {
            Column c = pk.getColumns().get(i);
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(c.getName()).append(" = ").append("OLD.").append(c.getName());
        }
        sb.append(";\n");
        sb.append("END");
        return sb.toString();
    }

    public static String buildUpdatePlan(Table view, String sourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("FOR EACH ROW\n");
        sb.append("BEGIN ATOMIC\n");
        sb.append("UPDATE ").append(sourceName).append(".").append(view.getName()).append(" SET ");

        for (int i = 0; i < view.getColumns().size(); i++) {
            Column c = view.getColumns().get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(c.getName()).append(" = ").append("NEW.").append(c.getName());
        }

        KeyRecord pk = RedirectionSchemaBuilder.getPK(view);
        sb.append(" WHERE ");
        for (int i = 0; i < pk.getColumns().size(); i++) {
            Column c = pk.getColumns().get(i);
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(c.getName()).append(" = ").append("OLD.").append(c.getName());
        }
        sb.append(";\n");
        sb.append("END");
        return sb.toString();
    }

    public static String buildInsertPlan(Table view, String sourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("FOR EACH ROW\n");
        sb.append("BEGIN ATOMIC\n");
        sb.append("INSERT INTO ").append(sourceName).append(".").append(view.getName()).append("( ");
        appendColumnNames(view, sb, null);
        sb.append(") VALUES ( ");
        appendColumnNames(view, sb, "NEW");
        sb.append(");\n");
        sb.append("END");
        return sb.toString();
    }

    public static String buildSelectPlan(Table view, String sourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        appendColumnNames(view, sb, null);
        sb.append("FROM ").append(sourceName).append(".").append(view.getName());
        return sb.toString();
    }

    private String findSourceWhereEntityExists(String tableName) {
        String foundIn = null;
        boolean found = false;
        for (Model model : vdb.getModels()) {
            Schema s = this.server.getSchema(model.getName());
            Table table = s.getTable(tableName);
            if (table != null) {
                if (!found) {
                    foundIn = model.getName();
                    found = true;
                } else {
                    throw new IllegalStateException(tableName + " table found in more than single data source, failed "
                            + "to create view due to ambiguity. You can define @SelectQuery on the Entity class "
                            + "with transformation to fix the issue.");
                }
            }
        }
        return foundIn;
    }

    private static void appendColumnNames(Table srcTable, StringBuilder sb, String alias) {
        boolean first = true;
        for (Column srcColumn : srcTable.getColumns()) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            if (alias != null) {
                sb.append(alias).append(".");
            }
            sb.append(srcColumn.getName());
        }
        sb.append(" ");
    }
}
