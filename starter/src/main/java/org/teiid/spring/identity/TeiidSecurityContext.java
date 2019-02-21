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

import javax.security.auth.Subject;

public class TeiidSecurityContext {
    private Subject subject;
    private String securityDomain;
    private String userName;

    public String getUserName() {
        return userName;
    }

    public TeiidSecurityContext(Subject s, String user, String securityDomain) {
        this.subject = s;
        this.userName = user;
        this.securityDomain = securityDomain;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getSecurityDomain() {
        return securityDomain;
    }
}
