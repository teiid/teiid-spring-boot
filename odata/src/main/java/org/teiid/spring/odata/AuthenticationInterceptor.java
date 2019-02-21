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
package org.teiid.spring.odata;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.teiid.spring.autoconfigure.TeiidConstants;
import org.teiid.spring.identity.SpringSecurityHelper;

public class AuthenticationInterceptor implements HandlerInterceptor {
    private SpringSecurityHelper securityHelper;

    public AuthenticationInterceptor(SpringSecurityHelper securityHelper) {
        this.securityHelper = securityHelper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        this.securityHelper.authenticate(TeiidConstants.SPRING_SECURITY, "anonymous", null, null);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        this.securityHelper.clearSecurityContext();
    }
}
