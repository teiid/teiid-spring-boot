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
package org.teiid.spring.data;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface ConfigurationProperty {

    /**
     * Get the description of this property.
     * @return the description for this property
     */
    String description() default "";

    /**
     * Get the localized display name of this property.
     * @return the displayable name for this property
     */
    String displayName() default "";

    /**
     * Get the default value for values of this property, or an empty String if
     * there is no default value.
     * @return the default value for this property, or an empty String
     * if there is no default value.
     */
    String defaultValue() default "";

    /**
     * Get the type that best represents the property type.
     * @return the string, numeric or boolean that best represents the property type.
     */
    String type() default "string";

    /**
     * The "expert" flag is used to distinguish between features that are
     * intended for expert users from those that are intended for normal users.
     * @return true if this property is to be marked with the expert flag,
     * or false otherwise.
     */
    boolean advanced() default false;

    /**
     * The "masked" flag is used to tell whether the value should be masked
     * when displayed to users.
     * @return true if this property value is to be masked,
     * or false otherwise.
     */
    boolean masked() default false;

    /**
     * The "required" flag is used to identify features that require at least
     * one value (possibly a default value) by the consumer of the property.
     */
    boolean required() default false;

    /**
     * Get the allowed values for this property.
     * @return the list of allowed values for this property, or an empty
     * set if the values do not have to conform to a fixed set.
     */
    String[] allowedValues() default {};
}
