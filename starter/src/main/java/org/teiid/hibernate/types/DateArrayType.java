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

import java.sql.Date;
import java.util.Properties;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Vlad MIhalcea
 */
public class DateArrayType extends AbstractSingleColumnStandardBasicType<Date[]> implements DynamicParameterizedType {
    private static final long serialVersionUID = -4935534679191612249L;

    public DateArrayType() {
        super(ArraySqlTypeDescriptor.INSTANCE, DateArrayTypeDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return "date-array";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        ((DateArrayTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
    }
}
