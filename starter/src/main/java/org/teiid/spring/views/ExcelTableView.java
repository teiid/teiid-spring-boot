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
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.Metadata;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.annotations.ExcelTable;

public class ExcelTableView extends ViewBuilder<ExcelTable> {

    private AtomicInteger columnIdx = new AtomicInteger();
    private StringBuilder columns = new StringBuilder();

    public ExcelTableView(Metadata metadata) {
        super(metadata);
    }

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, ExcelTable annotation) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(columns.toString()).append("FROM ");
        sb.append(entityClazz.getSimpleName().toLowerCase()).append(".").append(annotation.sheetName());
        sb.append(" AS st");
        view.setSelectTransformation(sb.toString());
    }

    @Override
    void onColumnCreate(Table view, Column column, MetadataFactory mf, Field field, String parent, boolean last,
            ExcelTable annotation) {

        boolean headerExists = annotation.headerRow() != -1;
        String columnName = column.getName();
        if (!headerExists) {
            columnName = "column" + columnIdx.incrementAndGet();
        }
        this.columns.append("convert(st.").append(columnName).append(",").append(column.getRuntimeType()).append(")");
        this.columns.append(" AS ").append(column.getName());

        if (!last) {
            this.columns.append(", ");
        } else {
            this.columns.append(" ");
        }
    }
}
