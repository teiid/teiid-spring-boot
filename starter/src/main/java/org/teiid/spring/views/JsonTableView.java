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

import org.hibernate.boot.Metadata;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.annotations.JsonTable;
import org.teiid.spring.annotations.RestConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonTableView extends ViewBuilder<JsonTable> {
    private StringBuilder columndef = new StringBuilder();
    private StringBuilder columns = new StringBuilder();

    public JsonTableView(Metadata metadata) {
        super(metadata);
    }

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, JsonTable annotation) {
        String source = annotation.source();
        String file = annotation.endpoint();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT \n");
        sb.append(columns.toString()).append("\n");
        sb.append("FROM (");
        if (annotation.source().equalsIgnoreCase("file")) {
            sb.append("EXEC ").append(source).append(".getFiles('").append(file).append("')");
        } else if (annotation.source().equalsIgnoreCase("rest")) {
            generateRestProcedure(entityClazz, source, file, sb);
        } else {
            throw new IllegalStateException("Source type '" + annotation.source() + " not supported on JsonTable "
                    + view.getName() + ". Only \"file\" and \"rest\" are supported");
        }
        sb.append(") AS f, ").append("\n");

        String root = annotation.root();
        root = "/response" + root;
        if (annotation.rootIsArray()) {
            root = "/response" + root;
        }
        if (root.endsWith("/")) {
            root = root.substring(0, root.lastIndexOf('/'));
        }

        if (annotation.source().equals("file")) {
            sb.append("XMLTABLE('").append(root).append("' PASSING JSONTOXML('response', f.file) ");
        } else {
            sb.append("XMLTABLE('").append(root).append("' PASSING JSONTOXML('response', f.result) ");
        }
        sb.append("COLUMNS ").append(columndef.toString());
        sb.append(") AS jt");

        view.setSelectTransformation(sb.toString());
    }

    static void generateRestProcedure(Class<?> entityClazz, String source, String file, StringBuilder sb) {
        RestConfiguration config = entityClazz.getAnnotation(RestConfiguration.class);
        String method = (config == null) ? "GET" : config.method();
        String headers = (config == null) ? null : config.headersBean();
        boolean streaming = (config == null) ? true : config.stream();
        String body = (config == null) ? null : config.bodyBean();
        sb.append("EXEC ").append(source).append(".invokeHttp(action=>'").append(method).append("', ");
        sb.append("endpoint=>'").append(file).append("', ");
        if (headers != null && !headers.isEmpty()) {
            sb.append("headers=>jsonObject('").append(headers).append("' as \"T-Spring-Bean\"), ");
        }
        if (body != null && !body.isEmpty()) {
            sb.append("body=>'").append(body).append("', ");
        }
        sb.append("stream=>'").append(Boolean.toString(streaming)).append("'");
        sb.append(")");
    }

    @Override
    void onColumnCreate(Table view, Column column, MetadataFactory mf, Field field, String parent, boolean last,
            JsonTable annotation) {

        JsonTable colAnnotation = field.getAnnotation(JsonTable.class);

        this.columns.append("jt.").append(column.getName());
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

        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
            columndef.append(" PATH '").append(jsonProperty.value()).append("'");
        } else if (parent != null) {
            columndef.append(" PATH '").append(parent).append("/").append(column.getName()).append("'");
        }

        if (!last) {
            this.columndef.append(", ");
        } else {
            this.columndef.append(" ");
        }
    }
}
