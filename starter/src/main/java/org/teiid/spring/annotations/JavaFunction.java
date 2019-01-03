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

import org.teiid.metadata.FunctionMethod.Determinism;
import org.teiid.metadata.FunctionMethod.PushDown;

/**
 * Using this annotation define User Defined Function based on the Java
 * code.<br>
 * Using this annotation, you can define a static method on a class that is
 * annotated with &#64;UserDefinedFunctions, at runtime this method will be
 * available for execution in Teiid queries such as {@link SelectQuery}<br>
 *
 * <pre>
 * <code>
 * &#64;UserDefinedFunctions
 * public class UserFunctions {
 *
 *   &#64;JavaFunction
 *   public static String addHello(String value) {
 *     return "Hello "+value;
 *   }
 * }
 *
 * </code>
 * </pre>
 *
 * Then above function can be used in annotation queries such as
 * &#64;SelectQuery
 *
 * <pre>
 * <code>
 * &#64;Entity
 * &#64;SelectQuery(select ssn as id, addHello(concat(firstname, concat(lastname,','))) as full_name, dob as dob FROM myTable)
 * public class Person {
 *    &#64;Id
 *    private Long id;
 *    private String fullName;
 *    private date dob;
 * }
 * </code>
 * </pre>
 *
 * Note: functions can be used any where you are writing SQL as part of Entity
 * definition. For an example see
 * {@link org.teiid.spring.annotations.UserDefinedFunctions }
 *
 * For more information checkout <a href=
 * "http://teiid.github.io/teiid-documents/master/content/dev/User_Defined_Functions.html">UDF</a>
 * in Teiid.
 *
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface JavaFunction {
    boolean nullOnNull() default false;

    Determinism determinism() default Determinism.DETERMINISTIC;

    PushDown pushdown() default PushDown.CAN_PUSHDOWN;
}

