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
 * Defines the Teiid View's Transformation query. This is must have annotation
 * to define Entity as View.<br>
 *
 * If you defined a @Entity annotation on a JAVA class and would like to use
 * this class as View definition in Teiid, then use this annotation to define,
 * how Teiid can build this View based on other Tables and Views.<br>
 * <br>
 *
 * For example for entity class:<br>
 *
 * <pre>
 * <code>
 * &#64;Entity
 * &#64;SelectQuery(select ssn as id, concat(firstname, concat(lastname,',')) as full_name, dob as dob FROM myTable)
 * public class Person {
 *    &#64;Id
 *    private Long id;
 *    private String fullName;
 *    private date dob;
 * }
 * </code>
 * </pre>
 *
 * Will generate view in Teiid as follows <br>
 *
 * <pre>
 * <code>
 * CREATE VIEW person {
 *    id long;
 *    dob date,
 *    full_name string,
 *    PRIMARY KEY(id)
 * AS select ssn as id, dob as dob, concat(firstname, concat(lastname,',')) as full_name FROM myTable;
 * </code>
 * </pre>
 *
 * <b>IMPOTANT</b>: No matter how the ordering of your attributes in the
 * &#64;Entity class, JPA(Hibernate) framework generates View columns in
 * ALPHABETICAL order. So, in &#64;SelectQuery the ordering of the columns in
 * select clause MUST match to that of JPA generation. If you do not follow this
 * you will either end with validation errors, or with wrong data. In the above
 * example see how the 'dob' and 'fullName' attributes have been changed in
 * order.<br>
 * <br>
 *
 * <b>NOTE</b>: If you used CamelCase for your attributes in the Entity class,
 * the generated Teiid's View class columns will be based on the Naming Strategy
 * that is configured. By default SpringBoot uses
 * {@link org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy}
 * class. Which converts column names with under scores ("_"). You can define
 * your own naming strategy by defining the property as below example. The
 * example below will keep the names intact as defined.
 *
 * <pre>
 * {@code
 * spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
 * }
 * </pre>
 *
 * For more information checkout <a href=
 * "http://teiid.github.io/teiid-documents/master/content/reference/DDL_Metadata.html">DDL
 * support in Teiid</a>
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SelectQuery {
    String value() default "";
}

