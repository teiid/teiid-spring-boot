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
 * Defines RestConfiguration, like verbs and headers etc for REST based
 * connection. If the values of the annotation parameters in configuration are
 * expressions like ${custom.myheader} they will be evaluated at the runtime.
 *
 * For example:
 *
 * <pre>
 * <code>
 * &#64;Entity
 * &#64;JsonTable(source=rest, endpoint="http://my.serviceprovider.com/service")
 * &#64;RestConfiguration(method="GET", headersBean="myHeaders")
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
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RestConfiguration {
    /**
     * HTTP Verb GET, PUT, PATCH, DELETE
     *
     * @return String
     */
    String method() default "GET";

    /**
     * Bean name which defines the HTTP Headers to be sent to the REST invocation.
     * For example when you want to handle HTTP Basic Authentication, you can do
     * like.
     *
     * <pre>
     * <code>
     * &#64;Configuration
     * public class MyConfigClass {<br>
     *   &#64;Bean(name="myHeaders")
     *   private HttpHeaders createHttpHeaders()
     *   {
     *     String notEncoded = "user:password";
     *     String encodedAuth = Base64.getEncoder().encodeToString(notEncoded.getBytes());
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setContentType(MediaType.APPLICATION_JSON);
     *     headers.add("Authorization", "Basic " + encodedAuth);
     *     return headers;
     *   }<br>
     *}
     * </code>
     * </pre>
     *
     * @return beanName
     */
    String headersBean() default "";

    /**
     * use streaming, i.e. the the read data will not copied, can only read once
     *
     * @return default true
     */
    boolean stream() default true;

    /**
     * Define the bean name that supplies for the payload for REST based calls. This
     * bean MUST be of type {@link org.teiid.core.types.InputStreamFactory}.
     *
     * @return body
     */
    String bodyBean() default "";
}

