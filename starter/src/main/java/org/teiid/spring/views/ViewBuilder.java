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

import javax.persistence.Embedded;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.dialect.TeiidDialect;
import org.teiid.hibernate.types.BigDecimalArrayType;
import org.teiid.hibernate.types.BigIntegerArrayType;
import org.teiid.hibernate.types.BooleanArrayType;
import org.teiid.hibernate.types.DateArrayType;
import org.teiid.hibernate.types.DoubleArrayType;
import org.teiid.hibernate.types.FloatArrayType;
import org.teiid.hibernate.types.IntArrayType;
import org.teiid.hibernate.types.LongArrayType;
import org.teiid.hibernate.types.ShortArrayType;
import org.teiid.hibernate.types.StringArrayType;
import org.teiid.hibernate.types.TimeArrayType;
import org.teiid.hibernate.types.TimestampArrayType;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;

public class ViewBuilder<T> {
	public static final TeiidDialect dialect = new TeiidDialect();
	protected Metadata metadata;
	
	public ViewBuilder(Metadata metadata) {
		this.metadata = metadata;	
	}
    
	@SuppressWarnings("unchecked")
    public void buildView(Class<?> entityClazz, MetadataFactory mf, T annotation) {
    	
		PersistentClass hibernateClass = this.metadata.getEntityBinding(entityClazz.getName());
    	org.hibernate.mapping.Table ormTable = hibernateClass.getTable();
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
        view.setSupportsUpdate(true);

        onTableCreate(view, mf, entityClazz, annotation);

		Iterator<org.hibernate.mapping.Column> it = ormTable.getColumnIterator();
        while(it.hasNext()) {
        	org.hibernate.mapping.Column ormColumn = it.next();
        	FieldInfo attribute = getAttributeField(entityClazz, hibernateClass, ormColumn.getName(), new FieldInfo());
        	// .. parent is used in the graph like structures, for now in json table.
			addColumn(ormTable, ormColumn, attribute.path, attribute.field, view, mf, !it.hasNext(),
					annotation);
        }
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
    
	
	private void addColumn(org.hibernate.mapping.Table ormTable, org.hibernate.mapping.Column ormColumn, String parent,
			Field attributeField, Table view, MetadataFactory mf, boolean last, T annotation) {
        
        String columnName = ormColumn.getName();
        boolean pk = isPK(ormTable, ormColumn);
        
        String type = JDBCSQLTypeInfo.getTypeName(ormColumn.getSqlTypeCode(metadata));
        if (type.equals("ARRAY")) {
        	type = getArrayType(ormColumn);
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
        if (pk && ormTable.getPrimaryKey().isGenerated(dialect)) {
        	column.setAutoIncremented(true);
        }
        onColumnCreate(view, column,  mf, attributeField, parent, last, annotation);        
    }	
	
	private String getArrayType(org.hibernate.mapping.Column ormColumn) {
		if (ormColumn.getValue().getType() instanceof StringArrayType) {
			return "string[]";
		} else if (ormColumn.getValue().getType() instanceof ShortArrayType) {
			return "short[]";
		} else if (ormColumn.getValue().getType() instanceof LongArrayType) {
			return "long[]";
		} else if (ormColumn.getValue().getType() instanceof IntArrayType) {
			return "integer[]";
		} else if (ormColumn.getValue().getType() instanceof FloatArrayType) {
			return "float[]";
		} else if (ormColumn.getValue().getType() instanceof DoubleArrayType) {
			return "double[]";
		} else if (ormColumn.getValue().getType() instanceof BigDecimalArrayType) {
			return "bigdecimal[]";
		} else if (ormColumn.getValue().getType() instanceof BooleanArrayType) {
			return "boolean[]";
		} else if (ormColumn.getValue().getType() instanceof BigIntegerArrayType) {
			return "biginteger[]";
		} else if (ormColumn.getValue().getType() instanceof DateArrayType) {
			return "date[]";
		} else if (ormColumn.getValue().getType() instanceof TimeArrayType) {
			return "time[]";
		} else if (ormColumn.getValue().getType() instanceof TimestampArrayType) {
			return "timestamp[]";
		}
		return ormColumn.getSqlType();
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
	
	@SuppressWarnings("unchecked")
	String propertyName(Iterator<org.hibernate.mapping.Property> it, org.hibernate.mapping.Property identifierProperty,
			String colName) {
		if (identifierProperty!= null && propertyMatches(identifierProperty, colName)) {
			return identifierProperty.getName();
		}
		while(it.hasNext()) {
			org.hibernate.mapping.Property property = it.next();
			if (propertyMatches(property, colName)) {
				if (property.isComposite()) {
					Component comp = (Component) property.getValue();
					Iterator<org.hibernate.mapping.Property> compIt = comp.getPropertyIterator();
					return propertyName(compIt, null, colName);
				} else {
					return property.getName();
				}
			}
		}
		return null;
	}	
	
	@SuppressWarnings("unchecked")
	boolean propertyMatches(org.hibernate.mapping.Property property, String colName) {
		if (property.isComposite()) {
			Component comp = (Component) property.getValue();
			Iterator<org.hibernate.mapping.Property> compIt = comp.getPropertyIterator();
			while (compIt.hasNext()) {
				property = compIt.next();
				if (propertyMatches(property, colName)) {
					return true;
				}
			}
			return false;
		}		
	    Iterator<?> columnIterator = property.getColumnIterator();
	    if (columnIterator.hasNext()) {
		    org.hibernate.mapping.Column col = (org.hibernate.mapping.Column) columnIterator.next();
		    assert ! columnIterator.hasNext();
			if (col.getName().equals(colName)) {
				return true;
			}
	    }
		return false;
	}
	
	static class FieldInfo {
		Field field;
		String path;
	}

	@SuppressWarnings("unchecked")
	private FieldInfo getAttributeField(Class<?> entityClazz, PersistentClass hibernateClass, String columnName,
			FieldInfo fieldInfo) {
		String propertyName = propertyName(hibernateClass.getPropertyIterator(), hibernateClass.getIdentifierProperty(),
				columnName);
		FieldInfo attribute = new FieldInfo();
		if (propertyName != null) {
			try {
				attribute.field = entityClazz.getDeclaredField(propertyName);
			} catch (NoSuchFieldException | SecurityException e) {
				for (Field field:entityClazz.getDeclaredFields()) {
					Embedded embedded = field.getAnnotation(Embedded.class);					
					if (embedded != null) {					
						attribute = getAttributeField(field.getType(), hibernateClass, columnName, fieldInfo);
						if (attribute.field != null) {
							fieldInfo.field = attribute.field;
							fieldInfo.path = fieldInfo.path == null?field.getName():field.getName()+"/"+fieldInfo.path;
							attribute = fieldInfo;
							break;
						}
					}
				}
			}
		}
		return attribute;
	}	
}
