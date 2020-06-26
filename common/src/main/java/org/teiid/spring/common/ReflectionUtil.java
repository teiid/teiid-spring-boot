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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtil {
    public static boolean isGetter(Method method) {
        if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0) {
            if (method.getName().matches("^get[A-Z].*") && !method.getReturnType().equals(void.class)) {
                return true;
            }
            if (method.getName().matches("^is[A-Z].*") && method.getReturnType().equals(boolean.class)) {
                return true;
            }
        }
        return false;
    }

    public static String getterName(Method method) {
        if (method.getName().matches("^get[A-Z].*")) {
            return method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
        }
        if (method.getName().matches("^is[A-Z].*")) {
            return method.getName().substring(2, 3).toLowerCase() + method.getName().substring(3);
        }
        return null;
    }

    public static Class<?> getterType(Method method) {
        Class<?> type = null;
        if (method.getName().matches("^get[A-Z].*")) {
            type = method.getReturnType();
        }
        if (method.getName().matches("^is[A-Z].*")) {
            type = method.getReturnType();
        }
        return type;
    }

    public static boolean isSetter(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getReturnType().equals(void.class)
                && method.getParameterTypes().length == 1 && method.getName().matches("^set[A-Z].*");
    }

    public static boolean hasSetter(String name, Class<?> clazz) {
        String methodName = "set" + name.substring(0,1).toUpperCase() + name.substring(1);
        for (Method m : clazz.getMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && m.getReturnType().equals(void.class)
                    && m.getParameterTypes().length == 1 && m.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
