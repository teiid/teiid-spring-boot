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
package org.teiid.spring.common;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PropertyDefinition  implements Comparable<PropertyDefinition>{
    private String name;
    private String defaultValue;
    private String description;
    private String displayName;
    private String type;
    private Boolean advanced;
    private Boolean masked;
    private Boolean required;
    private Collection<String> allowedValues;

    public PropertyDefinition(String name, String displayName, String description, String type,
            Boolean required, Boolean masked, Boolean advanced, String defaultValue, String[] allowedValues) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.required = required;
        this.masked = masked;
        this.advanced = advanced;
        if (allowedValues != null && allowedValues.length > 0) {
            this.allowedValues = Arrays.asList(allowedValues);
        }
    }

    /**
     * Name of the property
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the allowed values for this property.
     * @return the list of allowed values for this property, or an empty
     * set if the values do not have to conform to a fixed set.
     */
    public Collection<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * Get the default value for values of this property, or an empty String if
     * there is no default value.
     * @return the default value for this property, or an empty String
     * if there is no default value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get the description of this property.
     * @return the description for this property
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the localized display name of this property.
     * @return the displayable name for this property
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the type that best represents the property type.
     * @return the string, numeric or boolean that best represents the property type.
     */
    public String getType() {
        return type;
    }

    /**
     * The "expert" flag is used to distinguish between features that are
     * intended for expert users from those that are intended for normal users.
     * @return true if this property is to be marked with the expert flag,
     * or false otherwise.
     */
    public Boolean isAdvanced() {
        return advanced;
    }

    /**
     * The "masked" flag is used to tell whether the value should be masked
     * when displayed to users.
     * @return true if this property value is to be masked,
     * or false otherwise.
     */
    public Boolean isMasked() {
        return masked;
    }

    /**
     * The "required" flag is used to identify features that require at least
     * one value (possibly a default value) by the consumer of the property.
     * <p>
     * Whether a property is required by the consumer is unrelated to whether
     * there is a default value, which only simplifies the task of the property
     * provider.  A property may be required, meaning it must have at least one
     * value, but that same property definition may or may not have a default.
     * The combination of required and whether it has a default will determine
     * whether the user must supply a value.
     * @return true if this property requires at least one value.
     */
    public Boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Display Name:").append(getDisplayName()); //$NON-NLS-1$
        result.append(" Name:").append(getName()); //$NON-NLS-1$
        result.append(" Description:").append(getDescription()); //$NON-NLS-1$
        result.append(" Property Type Classname:").append(getType()); //$NON-NLS-1$
        result.append(" Default Value:").append(getDefaultValue()); //$NON-NLS-1$
        result.append(" Allowed Values:").append(getAllowedValues()); //$NON-NLS-1$
        result.append(" Required:").append(isRequired()); //$NON-NLS-1$
        result.append(" Expert:").append(isAdvanced()); //$NON-NLS-1$
        result.append(" Masked:").append(isMasked()); //$NON-NLS-1$
        return result.toString();
    }

    @Override
    public int compareTo(PropertyDefinition arg0) {
        if (arg0 == null) {
            return -1;
        }
        return this.name.compareToIgnoreCase(arg0.name);
    }
}

