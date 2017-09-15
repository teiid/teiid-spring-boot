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
import java.util.Arrays;
import java.util.Iterator;

import javax.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PrimaryKey;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.dialect.TeiidDialect;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;

public class ViewBuilder<T> {
	protected Metadata metadata;
	
	public ViewBuilder(Metadata metadata) {
		this.metadata = metadata;	
	}
    
    public void buildView(Class<?> entityClazz, MetadataFactory mf, T annotation) {
    	
    	org.hibernate.mapping.Table ormTable = this.metadata.getEntityBinding(entityClazz.getName()).getTable();
        String tableName = ormTable.getQuotedName();
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
        
        Iterator<org.hibernate.mapping.Column> it = ormTable.getColumnIterator();
        while(it.hasNext()) {
        	org.hibernate.mapping.Column ormColumn = it.next();
        	addColumn(ormTable, ormColumn, null, view, mf, !it.hasNext(), annotation);
        }
        
        
//        for (int i = 0; i < entityClazz.getDeclaredFields().length; i++) {
//            Field field = entityClazz.getDeclaredFields()[i];
//            boolean last = (i == (entityClazz.getDeclaredFields().length-1));
//            addColumn(field, null, view, mf, last, annotation);
//        }
        onFinish(view, mf, entityClazz, annotation);
    }
    
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, T annotation) {
    }

    void onColumnCreate(Table view, Column column, MetadataFactory mf, Field field, String parent, boolean last,
            T annotation) {
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
        }else if (clazz.isAssignableFrom(int[].class)) {
            return Integer[].class;
        } else if (clazz.isAssignableFrom(byte[].class)) {
            return Byte[].class;
        } else if (clazz.isAssignableFrom(short[].class)) {
            return Short[].class;
        } else if (clazz.isAssignableFrom(float[].class)) {
            return Float[].class;
        } else if (clazz.isAssignableFrom(double[].class)) {
            return Double[].class;
        } else if (clazz.isAssignableFrom(long[].class)) {
            return Long[].class;
        }
        return clazz;
    }
    
    protected boolean isArray(Class<?> clazz) {
        return clazz.isArray();
    }
    
    
	private void addColumn(Field field, String parent, Table view, MetadataFactory mf, boolean last, T annotation) {
        
		if (field.getAnnotation(javax.persistence.Transient.class) != null) {
            return;
        }
        
        String fieldName = field.getName();
        String columnName = (parent == null) ? fieldName : parent + "_" + fieldName;
        javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            columnName = columnAnnotation.name();
        }
        
        boolean pk = false;
        javax.persistence.Id idAnnotation = field.getAnnotation(javax.persistence.Id.class);
        if (idAnnotation != null && parent == null) {
            pk = true;
        }
        
        String type = DataTypeManager.getDataTypeName(normalizeType(field.getType()));
        if (type.contains("object")  ) {

            if (field.getAnnotation(javax.persistence.Lob.class) != null) {
                type = DataTypeManager.DefaultDataTypes.BLOB;
            }

            // let's exclude arrays as we can handle that as a one to many
            if(!field.getType().isArray()) {

                if (field.getAnnotation(javax.persistence.Embedded.class) != null
                        && field.getType().getAnnotation(javax.persistence.Embeddable.class) != null) {
                    // really need recursive logic here.
                    for (int x = 0; x < field.getType().getDeclaredFields().length; x++) {
                        Field innerField = field.getType().getDeclaredFields()[x];
                        boolean innerLast = (x == (field.getType().getDeclaredFields().length - 1));
                        addColumn(innerField, fieldName, view, mf, innerLast && last, annotation);
                    }
                    return;
                }
                if ((field.getAnnotation(javax.persistence.ManyToOne.class) != null
                        || field.getAnnotation(javax.persistence.OneToOne.class) != null)
                        && field.getType().getAnnotation(javax.persistence.Entity.class) != null) {
                    for (int x = 0; x < field.getType().getDeclaredFields().length; x++) {
                        Field innerField = field.getType().getDeclaredFields()[x];
                        if (innerField.getAnnotation(Id.class) == null) {
                            continue;
                        }
                        addColumn(innerField, fieldName, view, mf, last, annotation);
                    }
                    return;
                }
                else if (field.getAnnotation(javax.persistence.OneToMany.class) != null
                        || field.getAnnotation(javax.persistence.ManyToOne.class) != null) {
                    return;
                } else if (field.getType().isEnum()){
                    type = DataTypeManager.DefaultDataTypes.SHORT;
                } else {
					throw new IllegalStateException(
							fieldName + " failed to inference type information without additional metadata");
                }
            }

        }
        
        Column column = mf.addColumn(columnName, type, view);
        if (pk) {
            mf.addPrimaryKey("PK", Arrays.asList(column.getName()), view);
        }
        column.setUpdatable(true);
        onColumnCreate(view, column,  mf, field, parent, last, annotation);        
    }
    
	
	private void addColumn(org.hibernate.mapping.Table ormTable, org.hibernate.mapping.Column ormColumn, String parent,
			Table view, MetadataFactory mf, boolean last, T annotation) {
        
        String fieldName = ormColumn.getName();
        String columnName = (parent == null) ? fieldName : parent + "_" + fieldName;

        boolean pk = false;
        if (parent == null) {
            pk = isPK(ormTable, ormColumn);
        }
        
        String type = JDBCSQLTypeInfo.getTypeName(ormColumn.getSqlTypeCode(metadata));
        if (type.contains("object")  ) {
/*
            if (field.getAnnotation(javax.persistence.Lob.class) != null) {
                type = DataTypeManager.DefaultDataTypes.BLOB;
            }

            // let's exclude arrays as we can handle that as a one to many
            if(!field.getType().isArray()) {

                if (field.getAnnotation(javax.persistence.Embedded.class) != null
                        && field.getType().getAnnotation(javax.persistence.Embeddable.class) != null) {
                    // really need recursive logic here.
                    for (int x = 0; x < field.getType().getDeclaredFields().length; x++) {
                        Field innerField = field.getType().getDeclaredFields()[x];
                        boolean innerLast = (x == (field.getType().getDeclaredFields().length - 1));
                        addColumn(innerField, fieldName, view, mf, innerLast && last, annotation);
                    }
                    return;
                }
                if ((field.getAnnotation(javax.persistence.ManyToOne.class) != null
                        || field.getAnnotation(javax.persistence.OneToOne.class) != null)
                        && field.getType().getAnnotation(javax.persistence.Entity.class) != null) {
                    for (int x = 0; x < field.getType().getDeclaredFields().length; x++) {
                        Field innerField = field.getType().getDeclaredFields()[x];
                        if (innerField.getAnnotation(Id.class) == null) {
                            continue;
                        }
                        addColumn(innerField, fieldName, view, mf, last, annotation);
                    }
                    return;
                }
                else if (field.getAnnotation(javax.persistence.OneToMany.class) != null
                        || field.getAnnotation(javax.persistence.ManyToOne.class) != null) {
                    return;
                } else if (field.getType().isEnum()){
                    type = DataTypeManager.DefaultDataTypes.SHORT;
                } else {
					throw new IllegalStateException(
							fieldName + " failed to inference type information without additional metadata");
                }
            }
*/
        }
        
        Column column = mf.addColumn(columnName, type, view);
        if (pk) {
            mf.addPrimaryKey(ormTable.getPrimaryKey().getName(), Arrays.asList(column.getName()), view);
        }
        column.setUpdatable(true);
        column.setLength(ormColumn.getLength());
        column.setScale(ormColumn.getScale());
        column.setPrecision(ormColumn.getPrecision());
        column.setNullType(ormColumn.isNullable()?NullType.Nullable:NullType.No_Nulls);
        column.setDefaultValue(ormColumn.getDefaultValue());
        onColumnCreate(view, column,  mf, null, parent, last, annotation);        
    }	
	
	// TODO: Support composite primary key
	private boolean isPK(org.hibernate.mapping.Table ormTable, org.hibernate.mapping.Column ormColumn) {
		PrimaryKey pk = ormTable.getPrimaryKey();
		if (pk != null) {
			Iterator<org.hibernate.mapping.Column> it = pk.getColumnIterator();
			while(it.hasNext()) {
				org.hibernate.mapping.Column c = it.next();
				if (ormColumn.equals(c)) {
					return true;
				}
			}
		}
		return false;
	}
	

    
}
