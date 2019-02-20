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

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class SimpleGroup extends SimplePrincipal implements Group {
    private final Set<Principal> members = new HashSet<Principal>();

    /**
     * Creates a new group with the given name.
     *
     * @param name Group name.
     */
    public SimpleGroup(final String name) {
        super(name);
    }

    @Override
    public boolean addMember(final Principal user) {
        return this.members.add(user);
    }

    @Override
    public boolean isMember(final Principal member) {
        return this.members.contains(member);
    }

    @Override
    public Enumeration<? extends Principal> members() {
        return Collections.enumeration(this.members);
    }

    @Override
    public boolean removeMember(final Principal user) {
        return this.members.remove(user);
    }

    @Override
    public String toString() {
        return super.toString() + ": " + members.toString();
    }
}
