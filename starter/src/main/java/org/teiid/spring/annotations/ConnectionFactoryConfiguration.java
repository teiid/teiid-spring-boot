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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.teiid.spring.data.BaseConnection;
import org.teiid.spring.data.BaseConnectionFactory;

/**
 * The Bean that has this annotation represents a Custom Source in Teiid Spring
 * Boot framework. <br>
 *
 * To access custom data source with Teiid framework once can implement this
 * annotation and follow the rules to define a source. Build the bean and have
 * the jar as dependency to your Teiid engine, whether you are working with FAT
 * JAR or simple YAML based deployment<br>
 *
 * The bean which has this annotation expected to implement
 * {@link BaseConnectionFactory} and return a connection that extends
 * {@link BaseConnection}
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface ConnectionFactoryConfiguration {

    /**
     * Name of the ConnectionFactory alias name. If in the classpath a file found with name.mustache, that will be used
     * with auto generation of the Datasource classes
     * @return
     */
    String alias();

    /**
     * Name of the translator that works with this source
     * @return
     */
    String translatorName();

    /**
     * Thirdparty dependencies in GAV format that are needed with this connection
     * @return
     */
    String[] dependencies() default {};

    /**
     * Hibernate dialect if available
     * @return
     */
    String dialect() default "";
}
