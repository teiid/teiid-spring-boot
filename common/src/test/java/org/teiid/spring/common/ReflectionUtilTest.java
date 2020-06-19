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
package org.teiid.spring.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReflectionUtilTest {

    class Foo {
        public String getOne() {
            return "one";
        }
        public void setOne(String one) {
        }
        public boolean isTwo() {
            return true;
        }
        public void setTwo(boolean two) {
        }
        public String getThree() {
            return "three";
        }
    }

    @Test
    public void testUtils() throws Exception {
        assertTrue(ReflectionUtil.isGetter(Foo.class.getMethod("getOne")));
        assertTrue(ReflectionUtil.isGetter(Foo.class.getMethod("isTwo")));
        assertFalse(ReflectionUtil.isSetter(Foo.class.getMethod("isTwo")));
        assertTrue(ReflectionUtil.isSetter(Foo.class.getMethod("setOne", String.class)));
        assertTrue(ReflectionUtil.isSetter(Foo.class.getMethod("setTwo", boolean.class)));
        assertEquals("one", ReflectionUtil.getterName(Foo.class.getMethod("getOne")));
        assertEquals("two", ReflectionUtil.getterName(Foo.class.getMethod("isTwo")));

        assertEquals(boolean.class, ReflectionUtil.getterType(Foo.class.getMethod("isTwo")));
        assertEquals(String.class, ReflectionUtil.getterType(Foo.class.getMethod("getOne")));
    }
}
