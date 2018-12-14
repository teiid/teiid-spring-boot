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

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.annotations.TextTable;

public class TextTableView extends ViewBuilder<TextTable> {
    private static final Log logger = LogFactory.getLog(TextTableView.class);

    private StringBuilder columndef = new StringBuilder();
    private StringBuilder columns = new StringBuilder();

    public TextTableView(Metadata metadata) {
        super(metadata);
    }

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, TextTable annotation) {
        String source = annotation.source();
        String file = annotation.file();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT \n");
        sb.append(columns.toString()).append("\n");
        sb.append("FROM (");
        if (annotation.source().equalsIgnoreCase("file")) {
            sb.append("EXEC ").append(source).append(".getTextFiles('").append(file).append("')");
        } else if (annotation.source().equalsIgnoreCase("rest")) {
            JsonTableView.generateRestProcedure(entityClazz, source, file, sb);
        } else {
            throw new IllegalStateException("Source type '" + annotation.source() + " not supported on TextTable "
                    + view.getName() + ". Only \"file\" and \"rest\" are supported");
        }
        sb.append(") AS f, ").append("\n");

        if (annotation.source().equals("file")) {
            sb.append("TEXTTABLE(f.file COLUMNS ").append(columndef.toString());
        } else {
            sb.append("TEXTTABLE(f.result COLUMNS ").append(columndef.toString());
        }

        if (!annotation.delimiter().equals(",")) {
            sb.append(" ");
            if (annotation.header() == 1) {
                sb.append("DELIMETER ").append(annotation.delimiter());
            }
        }

        if (annotation.quote() != '"') {
            sb.append(" ");
            sb.append("QUOTE ").append(annotation.quote());
        }

        if (annotation.escape() != '\\') {
            sb.append(" ");
            sb.append("ESCAPE ").append(annotation.quote());
        }

        if (annotation.header() > 0) {
            sb.append(" ");
            if (annotation.header() == 1) {
                sb.append("HEADER");
            } else {
                sb.append("HEADER ");
                sb.append(annotation.header());
            }
        }

        if (annotation.skip() > 0) {
            sb.append(" ");
            sb.append("SKIP").append(annotation.skip());
        }

        if (!annotation.notrim()) {
            sb.append(" NO TRIM");
        }
        sb.append(") AS tt;");

        logger.debug("Generated View's Transformation: " + sb.toString());
        view.setSelectTransformation(sb.toString());
    }

    @Override
    void onColumnCreate(Table view, Column column, MetadataFactory mf, Field field, String parent, boolean last,
            TextTable annotation) {

        TextTable colAnnotation = field.getAnnotation(TextTable.class);

        this.columns.append("tt.").append(column.getName());
        if (!last) {
            this.columns.append(", ");
        } else {
            this.columns.append(" ");
        }

        this.columndef.append(column.getName());

        if (colAnnotation != null && colAnnotation.ordinal()) {
            columndef.append(" FOR ORDINALITY");
        } else {
            columndef.append(" ").append(column.getRuntimeType());
        }

        if (colAnnotation != null && colAnnotation.width() > 0) {
            columndef.append(" WIDTH ").append(colAnnotation.width());
        }

        if (!last) {
            this.columndef.append(", ");
        } else {
            this.columndef.append(" ");
        }
    }
}
