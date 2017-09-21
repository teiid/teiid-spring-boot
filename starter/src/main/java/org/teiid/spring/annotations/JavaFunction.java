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
 * code.<br/>
 * Using this annotation, you can define a static method on a class that is
 * annotated with &#64;UserDefinedFunctions, at runtime this method will be
 * available for execution in Teiid queries such as {@link SelectQuery}<br/><br/>
 * 
 * For an example see {@link UserDefinedFunctions}.
 * 
 * For more information checkout <a href=
 * "https://teiid.gitbooks.io/documents/content/dev/User_Defined_Functions.html">User
 * Defined Functions</a> in Teiid.
 * 
 * @See {@link UserDefinedFunctions}
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface JavaFunction {
	boolean nullOnNull() default false;
	Determinism determinism() default Determinism.DETERMINISTIC;
    PushDown pushdown() default PushDown.CAN_PUSHDOWN;

}
