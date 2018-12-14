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

/**
 * Use this annotation on any Entity class, that defines the data from a JSON
 * file or web resource.<br>
 * For Example if you have a JSON payload like
 *
 * <pre>
 * <code>
[
   {
      "id":1,
      "FirstName":"John",
      "LastName":"Doe",
      "Age":20
   },
   {
      "id":2,
      "FirstName":"Susan",
      "LastName":"Webber",
      "Age":55
   },
   {
      "id":1,
      "FirstName":"Mike",
      "LastName":"Smith",
      "Age":34
   }
]
 * </code>
 * </pre>
 *
 * You can parse and read the contents of this file into an entity by defining
 * this annotation on a Entity class like
 *
 * <pre>
 * <code>
 * &#64;Entity
 * &#64;JsonTable(source=file, endpoint="/path/to/file.txt")
 * public class Person {
 *    &#64;Id
 *    private int id;
 *
 *    &#64;Column(name="FirstName")
 *    private String firstName;
 *
 *    &#64;Column(name="LastName")
 *    private String lastName;
 *
 *    &#64;Column(name="Age")
 *    private int age;
 *    ...
 * }
 * </code>
 * </pre>
 *
 * Note: the getters and setter are omitted for brevity.<br>
 *
 * For more information checkout <a href=
 * "http://teiid.github.io/teiid-documents/master/content/reference/XMLTABLE.html">XMLTABLE</a>
 * in Teiid.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface JsonTable {

    /**
     * Source name; If overriding the org.teiid.translator.ws.WsExecutionFactory
     * bean then provide the name of the bean
     *
     * @return string
     */
    String source() default "rest"; // this the default web connection manager

    /**
     * On Class ONLY The endpoint where the document is located at. If this is file
     * resource then this field can be used to define name of the file.
     *
     * @return string
     */
    String endpoint();

    // column properties
    /**
     * On Column ONLY, this defines the column as ordinal identity column. The data
     * type must be integer. A FOR ORDINALITY column is typed as integer and will
     * return the 1-based item number as its value.
     *
     * @return boolean
     */
    boolean ordinal() default false;

    /**
     * Root of the document where the parsing needs to start from.
     *
     * @return string
     */
    String root() default "/";

    /**
     * JSON root content is array. ex: [{...}, {...}]
     *
     * @return boolean
     */
    boolean rootIsArray() default false;
}

