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
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.teiid.security.Credentials;
import org.teiid.security.GSSResult;
import org.teiid.security.SecurityHelper;

public class SpringSecurityHelper implements SecurityHelper {
    private static ThreadLocal<TeiidSecurityContext> securityContext = new ThreadLocal<TeiidSecurityContext>();
    private static final Log logger = LogFactory.getLog(SpringSecurityHelper.class);

    @Override
    public TeiidSecurityContext associateSecurityContext(Object newContext) {
        TeiidSecurityContext context = securityContext.get();
        if (newContext != context) {
            securityContext.set((TeiidSecurityContext)newContext);
        }
        return context;
    }

    @Override
    public void clearSecurityContext() {
        securityContext.remove();
    }

    @Override
    public TeiidSecurityContext getSecurityContext() {
        return securityContext.get();
    }

    @Override
    public Subject getSubjectInContext(String securityDomain) {
        TeiidSecurityContext tsc = securityContext.get();
        if (tsc != null && tsc.getSecurityDomain().equals(securityDomain)) {
            return getSubjectInContext(tsc);
        }
        return null;
    }

    @Override
    public Subject getSubjectInContext(Object context) {
        if (!(context instanceof TeiidSecurityContext)) {
            return null;
        }
        TeiidSecurityContext sc = (TeiidSecurityContext)context;
        return sc.getSubject();
    }

    @Override
    public Object authenticate(String securityDomain, String baseUserName,
            Credentials credentials, String applicationName) throws LoginException {
        Subject s = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            s = buildSubject(authentication);
        } else {
            s = new Subject();
            s.getPrincipals().add(new SimplePrincipal(baseUserName));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Logged in user: " + s);
        }
        return new TeiidSecurityContext(s, securityDomain);
    }

    @Override
    public GSSResult negotiateGssLogin(String securityDomain, byte[] serviceTicket) throws LoginException {
        return null;
    }

    static class TeiidSecurityContext {
        private Subject subject;
        private String securityDomain;

        public Subject getSubject() {
            return subject;
        }

        public String getSecurityDomain() {
            return securityDomain;
        }

        public TeiidSecurityContext(Subject s, String securityDomain) {
            this.subject = s;
            this.securityDomain = securityDomain;
        }
    }

    private Subject buildSubject(final Authentication authentication) {
        Subject s = new Subject();
        s.getPrincipals().add(new SimplePrincipal(authentication == null ? "anonymous":authentication.getName()));
        if (authentication != null) {
            for (GrantedAuthority ga : authentication.getAuthorities()) {
                String role = ga.getAuthority();
                if (role.startsWith("ROLE_")) {
                    role = role.substring(5);
                }
                s.getPrincipals().add(new SimpleGroup(role));
            }
        }
        return s;
    }
}
