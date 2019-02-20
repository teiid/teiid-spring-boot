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
package org.teiid.spring.identity;

import java.io.Serializable;
import java.security.Principal;

@SuppressWarnings("serial")
public class SimplePrincipal implements Principal, Serializable {

    /** SimplePrincipal.java */

    /** The unique identifier for this principal. */
    private final String name;

    /**
     * Creates a new principal with the given name.
     * @param name Principal name.
     */
    public SimplePrincipal(final String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof SimplePrincipal)) {
            return false;
        } else {
            return getName().equals(((SimplePrincipal)o).getName());
        }
    }

    @Override
    public int hashCode() {
        return 37 * getName().hashCode();
    }
}
