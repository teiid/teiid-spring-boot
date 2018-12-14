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

import java.util.List;

import org.hibernate.boot.Metadata;
import org.teiid.api.exception.query.QueryParserException;
import org.teiid.language.DerivedColumn;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.query.parser.QueryParser;
import org.teiid.query.sql.lang.QueryCommand;
import org.teiid.query.sql.symbol.AliasSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.spring.annotations.DeleteQuery;
import org.teiid.spring.annotations.InsertQuery;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.UpdateQuery;

public class SimpleView extends ViewBuilder<SelectQuery> {

    public SimpleView(Metadata metadata) {
        super(metadata);
    }

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, SelectQuery annotation) {
        String select = annotation.value();
        validateOrderingOfColumns(select, view, entityClazz);
        view.setSelectTransformation(annotation.value());

        InsertQuery insertAnnotation = entityClazz.getAnnotation(InsertQuery.class);
        if (insertAnnotation != null) {
            view.setInsertPlan(insertAnnotation.value());
            view.setSupportsUpdate(true);
        }

        UpdateQuery updateAnnotation = entityClazz.getAnnotation(UpdateQuery.class);
        if (updateAnnotation != null) {
            view.setUpdatePlan(updateAnnotation.value());
            view.setSupportsUpdate(true);
        }

        DeleteQuery deleteAnnotation = entityClazz.getAnnotation(DeleteQuery.class);
        if (deleteAnnotation != null) {
            view.setDeletePlan(deleteAnnotation.value());
            view.setSupportsUpdate(true);
        }
    }

    private void validateOrderingOfColumns(String select, Table view, Class<?> entityClazz) {
        try {
            QueryParser parser = QueryParser.getQueryParser();
            QueryCommand cmd = (QueryCommand) parser.parseCommand(select);
            List<Expression> expressions = cmd.getProjectedSymbols();
            List<Column> columns = view.getColumns();

            if (expressions.size() != columns.size()) {
                String msg = "On enity " + entityClazz.getName() + " in @SelectQuery annotation defined wrong number "
                        + "of projected columns than what are defined as entity attributes.";
                throw new IllegalStateException(msg);
            }

            for (int i = 0; i < columns.size(); i++) {
                Expression es = expressions.get(i);
                Column column = columns.get(i);

                if (es instanceof AliasSymbol) {
                    if (!((AliasSymbol) es).getName().equals(column.getName())) {
                        String msg = "On enity " + entityClazz.getName() + " in @SelectQuery annotation " + "column "
                                + ((DerivedColumn) es).getAlias() + "defined at wrong position. "
                                + "Please note View's columns order is " + columns;
                        throw new IllegalStateException(msg);
                    }
                } else {
                    /*
                     * if (!es.getType().equals(column.getJavaType())) { String msg = "On enity "+
                     * entityClazz.getName() + " in @SelectQuery annotation " + "column "+ es +
                     * " defined did not match in data type with Column " + column.getName()+";" +
                     * "Please note View's columns order is "+columns; throw new
                     * IllegalStateException(msg); }
                     */
                }
            }
        } catch (QueryParserException e) {
            // no-op; the validation at later state will fail
        }
    }
}
