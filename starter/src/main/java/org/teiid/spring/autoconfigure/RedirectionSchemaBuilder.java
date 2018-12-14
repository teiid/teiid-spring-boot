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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.metadata.Column;
import org.teiid.metadata.ForeignKey;
import org.teiid.metadata.KeyRecord;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;

public class RedirectionSchemaBuilder {
    static String ROW_STATUS_COLUMN = "ROW__STATUS";
    private static String TAB = "\t";

    private ApplicationContext context;
    private String redirectedDS;
    private HashMap<String, List<Table>> relations = new HashMap<>();

    public RedirectionSchemaBuilder(ApplicationContext context, String redirectedDS) {
        this.context = context;
        this.redirectedDS = redirectedDS;
    }

    public ModelMetaData buildRedirectionLayer(MetadataFactory source, String modelName) {
        ModelMetaData model = new ModelMetaData();
        model.setName(modelName);
        model.setModelType(Model.Type.VIRTUAL);
        MetadataFactory target = new MetadataFactory(VDBNAME, VDBVERSION,
                SystemMetadata.getInstance().getRuntimeTypeMap(), model);
        populateRedirectionSchema(source, target);
        model.addAttchment(MetadataFactory.class, target);
        String ddl = DDLStringVisitor.getDDLString(target.getSchema(), null, null);
        model.addSourceMetadata("DDL", ddl);
        return model;
    }

    private String buildSelectPlan(Table srcTable, String redirected) {
        StringBuilder plan = new StringBuilder();
        plan.append("SELECT ");
        appendColumnNames(srcTable, plan, "o");

        plan.append("FROM ");
        plan.append("internal.").append(srcTable.getName());
        plan.append(" AS o LEFT OUTER JOIN ");
        plan.append(redirectedTable(srcTable, redirected)).append(" AS m ");
        plan.append("ON (");
        KeyRecord pk = getPK(srcTable);
        if (pk == null) {
            throw new IllegalStateException("Redirection on table "+srcTable.getName() + " can not be performed, "
                    + "becuase there is no PK. You can skip redirection of this table by setting property\n"
                    + TeiidConstants.REDIRECTED+"."+srcTable.getName()+".skip=true");
        } else {
            boolean first = true;
            for (String str:getColumnNames(pk.getColumns())) {
                if (first) {
                    first = false;
                } else {
                    plan.append(" AND ");
                }
                plan.append("o.").append(str).append(" = m.").append(str);
            }
        }
        plan.append(") WHERE m.");
        plan.append(ROW_STATUS_COLUMN).append(" IS NULL \n UNION ALL \n");
        plan.append("SELECT ");
        appendColumnNames(srcTable, plan, null);
        plan.append("FROM ");
        plan.append(redirectedTable(srcTable, redirected));
        plan.append(" WHERE ").append(ROW_STATUS_COLUMN).append(" <> 3");
        return plan.toString();
    }

    public static KeyRecord getPK(Table srcTable) {
        KeyRecord pk = srcTable.getPrimaryKey();
        if (pk == null) {
            if (!srcTable.getUniqueKeys().isEmpty()) {
                pk = srcTable.getUniqueKeys().get(0);
            }
        }
        return pk;
    }

    private String buildInsertPlan(Table srcTable, String redirected) {
        StringBuilder plan = new StringBuilder();
        plan.append("FOR EACH ROW\n");
        plan.append("BEGIN ATOMIC\n");

        Consumer<StringBuilder> ifPlan = (sb) -> {
            sb.append(TAB);
            sb.append(TAB);
            sb.append("RAISE SQLEXCEPTION 'duplicate key';\n");
        };

        Consumer<StringBuilder> elsePlan = (sb) -> {
            sb.append(TAB);
            sb.append(TAB);
            sb.append("INSERT INTO ").append(redirectedTable(srcTable, redirected)).append(" (");
            for (int i = 0; i < srcTable.getColumns().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Column c = srcTable.getColumns().get(i);
                sb.append(c.getName());
            }
            sb.append(", ").append(ROW_STATUS_COLUMN).append(") VALUES (");
            for (int i = 0; i < srcTable.getColumns().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Column c = srcTable.getColumns().get(i);
                sb.append("NEW.").append(c.getName());
            }
            sb.append(", 1);\n");
        };

        ifExists(srcTable, pkColumnsAsWhereClause(srcTable, "NEW."), ifPlan, elsePlan, plan);
        plan.append("END");
        return plan.toString();
    }

    /*
    Procedure addProcedureKeyExists(String procedureName, MetadataFactory mf, Table srcTable, String redirected,
            KeyRecord record) {
        StringBuilder plan = new StringBuilder();

        Procedure proc = mf.addProcedure(procedureName);
        for (int i = 0 ; i < record.getColumns().size(); i++) {
            Column c = record.getColumns().get(i);
            mf.addProcedureParameter(c.getName()+"_value", c.getRuntimeType(), ProcedureParameter.Type.In , proc);
        }
        proc.setVirtual(true);
        mf.addProcedureParameter("return", "boolean", ProcedureParameter.Type.ReturnValue , proc);
        plan.append("BEGIN\n");
        if (record.getColumns().size() == 1) {
            Column c = record.getColumns().get(0);
            plan.append(TAB);
            plan.append("DECLARE long VARIABLES.X").append(" = (SELECT count(")
                    .append(c.getName()).append(") FROM ").append("internal.").append(srcTable.getName()).append(" WHERE ")
                    .append(c.getName()).append(" = ").append(c.getName()).append("_value").append(");\n");
            plan.append(TAB);
            plan.append("DECLARE long VARIABLES.Y").append(" = (SELECT count(")
                    .append(c.getName()).append(") FROM ").append(redirectedTable(srcTable, redirected)).append(" WHERE ")
                    .append(c.getName()).append(" = ").append(c.getName()).append("_value").append(" AND ")
                    .append(ROW_STATUS_COLUMN).append(" <> 3").append(");\n");
            plan.append(TAB);
            plan.append("IF (").append("VARIABLES.X").append(" > 0 OR").append(" VARIABLES.Y").append(" > 0)\n");
        } else {
            plan.append(TAB);
            plan.append("DECLARE object[]").append(" VARIABLES.X = (SELECT (");
            for (int i = 0; i < record.getColumns().size(); i++) {
                Column c = record.getColumns().get(i);
                if (i > 0) {
                    plan.append(", ");
                }
                plan.append("count(").append(c.getName()).append(")");
            }
            plan.append(") FROM ").append("internal.").append(srcTable.getName()).append(" WHERE ");
            for (int i = 0; i < record.getColumns().size(); i++) {
                Column c = record.getColumns().get(i);
                if (i > 0) {
                    plan.append(" AND ");
                }
                plan.append(c.getName()).append(" = ").append(c.getName()).append("_value");
            }
            plan.append(");\n");

            plan.append(TAB);
            plan.append("DECLARE object[]").append(" VARIABLES.Y = (SELECT (");
            for (int i = 0; i < record.getColumns().size(); i++) {
                Column c = record.getColumns().get(i);
                if (i > 0) {
                    plan.append(", ");
                }
                plan.append("count(").append(c.getName()).append(")");
            }
            plan.append(") FROM ").append(redirectedTable(srcTable, redirected)).append(" WHERE ");
            for (int i = 0; i < record.getColumns().size(); i++) {
                Column c = record.getColumns().get(i);
                if (i > 0) {
                    plan.append(" AND ");
                }
                plan.append(c.getName()).append(" = ").append(c.getName()).append("_value AND ")
                        .append(ROW_STATUS_COLUMN).append(" <> 3").append(");\n");
            }
            plan.append(");\n");
            plan.append(TAB);
            plan.append("IF ((");
            for (int i = 0; i < record.getColumns().size(); i++) {
                if (i > 0) {
                    plan.append(" AND ");
                }
                plan.append("ARRAY_GET(VARIABLES.X, ").append(i).append(") > 0");
            }
            plan.append(") OR (");
            for (int i = 0; i < record.getColumns().size(); i++) {
                if (i > 0) {
                    plan.append(" AND ");
                }
                plan.append("ARRAY_GET(VARIABLES.Y, ").append(i).append(") > 0");
            }
            plan.append("))\n");
        }
        plan.append("\tBEGIN\n\t\tRETURN TRUE;\n\tEND\n");
        plan.append("\tRETURN FALSE;\n");
        plan.append("END");
        proc.setQueryPlan(plan.toString());
        return proc;
    }
    */

    private String buildUpdatePlan(Table srcTable, String redirected, List<Table> assosiatedTables) {
        Consumer<StringBuilder> ifPlan = (sb) -> {
            sb.append(TAB);
            sb.append(TAB);
            sb.append("RAISE SQLEXCEPTION 'duplicate key';\n");
        };

        Consumer<StringBuilder> elsePlan = (sb) -> {
            if (assosiatedTables != null && !assosiatedTables.isEmpty()) {
                addReferentialChecks(srcTable, assosiatedTables, sb);
            }
            sb.append(TAB);
            sb.append(TAB);
            sb.append("UPSERT INTO ").append(redirectedTable(srcTable, redirected)).append("(");
            KeyRecord pk = getPK(srcTable);
            for (int i = 0; i < pk.getColumns().size(); i++) {
                Column c = pk.getColumns().get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(c.getName());
            }
            sb.append(", ").append(ROW_STATUS_COLUMN).append(") VALUES (");
            for (int i = 0; i < pk.getColumns().size(); i++) {
                Column c = pk.getColumns().get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("OLD.").append(c.getName());
            }
            sb.append(", 3);\n");
            sb.append(TAB);
            sb.append(TAB);
            sb.append("UPSERT INTO ").append(redirectedTable(srcTable, redirected)).append("(");
            for (int i = 0; i < srcTable.getColumns().size(); i++) {
                Column c = srcTable.getColumns().get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(c.getName());
            }
            sb.append(", ").append(ROW_STATUS_COLUMN).append(") VALUES (");
            for (int i = 0; i < srcTable.getColumns().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Column c = srcTable.getColumns().get(i);
                sb.append("NEW.").append(c.getName());
            }
            sb.append(", 1);\n");
        };

        StringBuilder plan = new StringBuilder();
        plan.append("FOR EACH ROW\n");
        plan.append("BEGIN ATOMIC\n");
        plan.append("IF (");
        KeyRecord pk = getPK(srcTable);
        for (int i = 0; i < pk.getColumns().size(); i++) {
            Column c = pk.getColumns().get(i);
            if (i > 0) {
                plan.append(" OR ");
            }
            plan.append("CHANGING.").append(c.getName());
        }
        plan.append(")\n");
        plan.append("BEGIN\n");
        ifExists(srcTable, pkColumnsAsWhereClause(srcTable, "NEW."), ifPlan, elsePlan, plan);
        plan.append("END\n");
        plan.append("ELSE\n");
        plan.append("BEGIN\n");
        plan.append(TAB);
        plan.append("UPSERT INTO ").append(redirectedTable(srcTable, redirected)).append("(");
        for (int i = 0; i < srcTable.getColumns().size(); i++) {
            Column c = srcTable.getColumns().get(i);
            if (i > 0) {
                plan.append(", ");
            }
            plan.append(c.getName());
        }
        plan.append(", ").append(ROW_STATUS_COLUMN).append(") VALUES (");
        for (int i = 0; i < srcTable.getColumns().size(); i++) {
            if (i > 0) {
                plan.append(", ");
            }
            Column c = srcTable.getColumns().get(i);
            plan.append("NEW.").append(c.getName());
        }
        plan.append(", 2);\n");
        plan.append("END\n");

        plan.append("END");
        return plan.toString();
    }

    private void addReferentialChecks(Table srcTable, List<Table> assosiatedTables, StringBuilder sb) {
        for (Table table : assosiatedTables) {
            for (ForeignKey fk : table.getForeignKeys()) {
                if (!fk.getReferenceTableName().equals(srcTable.getName())) {
                    continue;
                }
                String check = table.getName() + "_" + fk.getName() + "_EXISTS";
                sb.append(TAB).append(TAB);
                sb.append("DECLARE boolean VARIABLES.").append(check).append(" = (SELECT COUNT(*) > 0 FROM ")
                    .append(TeiidConstants.EXPOSED_VIEW).append(".").append(table.getName()).append(" WHERE ");

                for (int i = 0; i < fk.getColumns().size(); i++) {
                    if (i > 0) {
                        sb.append(" AND ");
                    }
                    sb.append(fk.getColumns().get(i).getName()).append(" = ").append("OLD.")
                            .append(fk.getReferenceColumns().get(i));
                }
                sb.append(");\n");
                sb.append(TAB).append(TAB);
                sb.append("IF (").append("VARIABLES.").append(check).append(")\n");
                sb.append(TAB).append(TAB);
                sb.append("BEGIN\n");
                sb.append(TAB).append(TAB).append(TAB)
                        .append("RAISE SQLEXCEPTION 'referential integrity check failed on ")
                        .append(table.getName()).append(" table, cascade deletes are not supported';\n");
                sb.append(TAB).append(TAB).append("END\n");
            }
        }
    }

    private String buildDeletePlan(Table srcTable, String redirected, List<Table> assosiatedTables) {
        StringBuilder plan = new StringBuilder();
        plan.append("FOR EACH ROW\n");
        plan.append("BEGIN ATOMIC\n");

        if (assosiatedTables != null && !assosiatedTables.isEmpty()) {
            addReferentialChecks(srcTable, assosiatedTables, plan);
        }

        plan.append(TAB);
        plan.append(TAB);
        plan.append("UPSERT INTO ").append(redirectedTable(srcTable, redirected)).append("(");

        for (int i = 0; i < getPK(srcTable).getColumns().size(); i++) {
            Column c = getPK(srcTable).getColumns().get(i);
            if (i > 0) {
                plan.append(", ");
            }
            plan.append(c.getName());
        }
        plan.append(", ").append(ROW_STATUS_COLUMN).append(") VALUES (");
        for (int i = 0; i < getPK(srcTable).getColumns().size(); i++) {
            Column c = getPK(srcTable).getColumns().get(i);
            if (i > 0) {
                plan.append(", ");
            }
            plan.append("OLD.").append(c.getName());
        }
        plan.append(", 3);\n");
        plan.append("END");
        return plan.toString();
    }

    private String pkColumnsAsWhereClause(Table t, String prefix) {
        KeyRecord pk = getPK(t);
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < pk.getColumns().size(); i++) {
            Column c = pk.getColumns().get(i);
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(c.getName()).append(" = ").append(prefix).append(c.getName());
        }
        return sb.toString();
    }

    void ifExists(Table view, String args, Consumer<StringBuilder> ifPlan, Consumer<StringBuilder> elsePlan,
            StringBuilder sb) {
        sb.append(TAB);
        sb.append("DECLARE boolean VARIABLES.").append(view.getName()).append("_PK_EXISTS")
            .append(" = (SELECT true FROM ").append(view.getFullName()).append(" WHERE ").append(args).append(");\n");
        sb.append(TAB);
        sb.append("IF (").append("VARIABLES.").append(view.getName()).append("_PK_EXISTS").append(")\n");
        sb.append(TAB);
        sb.append("BEGIN\n");
        ifPlan.accept(sb);
        sb.append(TAB);
        sb.append("END\n");
        if (elsePlan != null) {
            sb.append(TAB);
            sb.append("ELSE\n");
            sb.append(TAB);
            sb.append("BEGIN\n");
            elsePlan.accept(sb);
            sb.append(TAB);
            sb.append("END\n");
        }
    }

/*    void ifExists(Procedure check, String args, Consumer<StringBuilder> ifPlan, Consumer<StringBuilder> elsePlan,
            StringBuilder sb) {
        sb.append(TAB);
        sb.append("DECLARE boolean VARIABLES.X_").append(check.getName())
            .append(" = (SELECT x.return FROM (EXECUTE ").append(check.getFullName()).append("(").append(args);
        sb.append(")) AS x);\n");
        sb.append(TAB);
        sb.append("IF (").append("VARIABLES.X_").append(check.getName()).append(")\n");
        sb.append(TAB);
        sb.append("BEGIN\n");
        ifPlan.accept(sb);
        sb.append(TAB);
        sb.append("END\n");
        if (elsePlan != null) {
            sb.append(TAB);
            sb.append("ELSE\n");
            sb.append(TAB);
            sb.append("BEGIN\n");
            elsePlan.accept(sb);
            sb.append(TAB);
            sb.append("END\n");
        }
    }  */

    private String redirectedTable(Table srcTable, String redirected) {
        return redirected+"."+srcTable.getName()+TeiidConstants.REDIRECTED_TABLE_POSTFIX;
    }

    private void appendColumnNames(Table srcTable, StringBuilder sb, String alias) {
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

    private void populateRedirectionSchema(MetadataFactory source, MetadataFactory target) {

        for (Table srcTable : source.getSchema().getTables().values()) {
            if (skipRedirection(srcTable.getName())) {
                continue;
            }
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
                        getColumnNames(srcTable.getPrimaryKey().getColumns()), table);
            }

            if (!srcTable.getForeignKeys().isEmpty()) {
                for (ForeignKey fk : srcTable.getForeignKeys()) {
                    target.addForeignKey(fk.getName(), getColumnNames(fk.getColumns()), fk.getReferenceColumns(),
                            fk.getReferenceTableName(), table);
                }
            }

            if (!srcTable.getUniqueKeys().isEmpty()) {
                for (KeyRecord kr : srcTable.getUniqueKeys()) {
                    target.addIndex(kr.getName(), false, getColumnNames(kr.getColumns()), table);
                }
            }

            if (!srcTable.getIndexes().isEmpty()) {
                for (KeyRecord kr : srcTable.getIndexes()) {
                    target.addIndex(kr.getName(), true, getColumnNames(kr.getColumns()), table);
                }
            }
            table.setSelectTransformation(buildSelectPlan(srcTable, this.redirectedDS));
            table.setInsertPlan(buildInsertPlan(srcTable, this.redirectedDS));

            for (ForeignKey fk : srcTable.getForeignKeys()) {
                List<Table> relatives = this.relations.get(fk.getReferenceTableName());
                if (relatives == null) {
                    relatives = new ArrayList<>();
                    this.relations.put(fk.getReferenceTableName(), relatives);
                }
                relatives.add(srcTable);
            }
        }

        // take a second pass to add update and delete plans with referential constrains
        for (Table table : target.getSchema().getTables().values()) {
            Table srcTable = source.getSchema().getTables().get(table.getName());
            table.setUpdatePlan(buildUpdatePlan(srcTable, this.redirectedDS, this.relations.get(table.getName())));
            table.setDeletePlan(buildDeletePlan(srcTable, this.redirectedDS, this.relations.get(table.getName())));
        }
    }

    private boolean skipRedirection(String tblName) {
        return Boolean.parseBoolean(context.getEnvironment()
                .getProperty(TeiidConstants.REDIRECTED + "." + tblName.toLowerCase() + ".skip"));
    }

    public static List<String> getColumnNames(List<Column> columns) {
        ArrayList<String> list = new ArrayList<>();
        columns.forEach((i) -> list.add(i.getName()));
        return list;
    }
}
