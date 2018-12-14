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

import java.util.Properties;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

public class FloatArrayType extends AbstractSingleColumnStandardBasicType<float[]> implements DynamicParameterizedType {
    private static final long serialVersionUID = 233850294937607019L;

    public FloatArrayType() {
        super(ArraySqlTypeDescriptor.INSTANCE, FloatArrayTypeDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return "float-array";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        ((FloatArrayTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
    }
}
