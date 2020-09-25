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

package org.teiid.spring.example;

public class UserFunctions {

    /**
     * this is example of a UDF in Teiid. When you use this in Teiid queries the
     * function will be evaluated in the Teiid engine
     */
    public static String addSalutation(String value) {
        return "Mr. " + value;
    }

    /**
     * This is example of source pushdown function for h2 when a function is not
     * defined in the Teiid, you define a function like below and use in your user
     * queries which will be pushed to the source for evaluation
     * http://www.h2database.com/html/functions.html#repeat
     */
    public static String repeat(String p1, int p2) {
        return null;
    }
}
