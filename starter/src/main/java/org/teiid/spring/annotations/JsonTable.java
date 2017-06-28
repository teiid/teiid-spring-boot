/*
 * Copyright 2012-2014 the original author or authors.
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
package org.teiid.spring.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface JsonTable {
    
    /**
     * Source name; If overriding the {@link WsConnectionFactory} bean then provide the name of the bean
     */
    String source() default "rest"; // this the default web connection manager
    
    /**
     * On Class ONLY
     * The endpoint where the document is located at. If this is file resource then this field can be used to define
     * name of the file.
     */
    String endpoint();

    
    // column properties
    /**
     * On Column ONLY, this defines the column as ordinal identity column. The data type must be integer.
     * A FOR ORDINALITY column is typed as integer and will return the 1-based item number as its value.
     */
    boolean ordinal() default false;

    
    /**
     * Root of the document where the parsing needs to start from
     * @return
     */
    String root() default "/";
    
}
