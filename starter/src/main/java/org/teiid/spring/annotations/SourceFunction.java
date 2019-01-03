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

/**
 * Using this annotation define Data Source specific Function.<br>
 * Using this annotation, you can define a static method on a class that is
 * annotated with &#64;UserDefinedFunctions, at runtime this method will be
 * available for execution in Teiid queries such as {@link SelectQuery}. During
 * runtime the function will be evaluated in the source database as it gets
 * pushed down to source for execution<br>
 *
 * <pre>
 * <code>
 * &#64;UserDefinedFunctions
 * public class UserFunctions {
 *  &#64;SourceFunction(source="mydb", nativequery="repeat")
 *  public static String repeat(String p1, int p2) {
 *    return null;
 *  }
 * }
 * </code>
 * </pre>
 *
 * For an example see {@link UserDefinedFunctions}.
 *
 * For more information checkout <a href=
 * "http://teiid.github.io/teiid-documents/master/content/dev/Source_Supported_Functions.html">Source
 * Supported Functions</a> in Teiid.
 *
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface SourceFunction {
    /**
     * Defines the datasource name where the function needs to be defined.
     *
     * @return source database name
     */
    String source();

    /**
     * Adds teiid_rel:native-query to the function; when omitted the function name
     * executed as is
     *
     * @return native query or command
     */
    String nativequery() default "";
}

