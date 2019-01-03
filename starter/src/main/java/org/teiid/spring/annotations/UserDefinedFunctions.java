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
 * This is marker annotation to define class of user defined java based
 * functions or data source functions.
 *
 * <pre>
 * <code>
 * &#64;UserDefinedFunctions
 * public class MyFunctions {
 *
 *     &#64;JavaFunction
 *     public static String myFunc(String msg){
 *         return "Hello " + msg;
 *     }
 *
 *     &#64;SourceFunction
 *     public static String myFunc(String msg){
 *         // No code will be called here; this can be empty block.
 *         return msg;
 *     }
 * }
 * </code>
 * </pre>
 *
 * For more information checkout <a href=
 * "http://teiid.github.io/teiid-documents/master/content/dev/User_Defined_Functions.html">User
 * Defined Functions</a> in Teiid.
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface UserDefinedFunctions {
}
