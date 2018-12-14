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
package org.teiid.hibernate.types;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractArrayTypeDescriptor<T> extends AbstractTypeDescriptor<T>
        implements DynamicParameterizedType {
    private static final long serialVersionUID = 7698345570957205617L;
    private Class<T> arrayObjectClass;

    @SuppressWarnings("unchecked")
    @Override
    public void setParameterValues(Properties parameters) {
        arrayObjectClass = ((ParameterType) parameters.get(PARAMETER_TYPE)).getReturnedClass();

    }

    @SuppressWarnings({ "unchecked", "serial" })
    public AbstractArrayTypeDescriptor(Class<T> arrayObjectClass) {
        super(arrayObjectClass, (MutabilityPlan<T>) new MutableMutabilityPlan<Object>() {
            @Override
            protected T deepCopyNotNull(Object value) {
                return ArrayUtil.deepCopy(value);
            }
        });
        this.arrayObjectClass = arrayObjectClass;
    }

    @Override
    public boolean areEqual(Object one, Object another) {
        if (one == another) {
            return true;
        }
        if (one == null || another == null) {
            return false;
        }
        return ArrayUtil.isEquals(one, another);
    }

    @Override
    public String toString(Object value) {
        return Arrays.deepToString((Object[]) value);
    }

    @Override
    public T fromString(String string) {
        return ArrayUtil.fromString(string, arrayObjectClass);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        return (X) ArrayUtil.wrapArray(value);
    }

    @Override
    public <X> T wrap(X value, WrapperOptions options) {
        if (value instanceof Array) {
            Array array = (Array) value;
            try {
                return ArrayUtil.unwrapArray((Object[]) array.getArray(), arrayObjectClass);
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return (T) value;
    }

    protected abstract String getSqlArrayType();
}

