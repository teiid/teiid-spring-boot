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

import java.lang.reflect.Field;
import java.util.Arrays;

import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.annotations.TextTable;

public class ViewBuilder<T> {
    
    public void buildView(Class<?> entityClazz, MetadataFactory mf, T annotation) {
        String tableName = entityClazz.getSimpleName();
        javax.persistence.Entity entityAnnotation = entityClazz.getAnnotation(javax.persistence.Entity.class);
        if (entityAnnotation != null && !entityAnnotation.name().isEmpty()) {
            tableName = entityAnnotation.name();
        }
        
        javax.persistence.Table tableAnnotation = entityClazz.getAnnotation(javax.persistence.Table.class);                
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            tableName = tableAnnotation.name();
        }
        Table view = mf.addTable(tableName);
        view.setVirtual(true);

        onTableCreate(view, mf, entityClazz, annotation);
        
        for (int i = 0; i < entityClazz.getDeclaredFields().length; i++) {
            
            Field field = entityClazz.getDeclaredFields()[i];
            if (field.getAnnotation(javax.persistence.Transient.class) != null) {
                continue;
            }
            String columnName = field.getName();
            javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
            if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
                columnName = columnAnnotation.name();
            }
            
            boolean pk = false;
            javax.persistence.Id idAnnotation = field.getAnnotation(javax.persistence.Id.class);
            if (idAnnotation != null) {
                pk = true;
            }
            
            String type = DataTypeManager.getDataTypeName(normalizeType(field.getType()));
            Column column = mf.addColumn(columnName, type, view);
            if (pk) {
                mf.addPrimaryKey("PK", Arrays.asList(column.getName()), view);
            }
            
            onColumnCreate(view, column,  mf, field, i == (entityClazz.getDeclaredFields().length-1), annotation);
        }
        onFinish(view, mf, entityClazz, annotation);
    }
    
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, T annotation) {
    }

    void onColumnCreate(Table view, Column column, MetadataFactory mf, Field field, boolean last, T annotation) {
    }

    void onTableCreate(Table view, MetadataFactory mf, Class<?> entityClazz, T annotation) {
    }

    protected Class<?> normalizeType(Class<?> clazz){
        if (clazz.isAssignableFrom(int.class)) {
            return Integer.class;
        } else if (clazz.isAssignableFrom(byte.class)) {
            return Byte.class;
        } else if (clazz.isAssignableFrom(short.class)) {
            return Short.class;
        } else if (clazz.isAssignableFrom(float.class)) {
            return Float.class;
        } else if (clazz.isAssignableFrom(double.class)) {
            return Double.class;
        } else if (clazz.isAssignableFrom(long.class)) {
            return Long.class;
        }
        return clazz;
    }    
}
