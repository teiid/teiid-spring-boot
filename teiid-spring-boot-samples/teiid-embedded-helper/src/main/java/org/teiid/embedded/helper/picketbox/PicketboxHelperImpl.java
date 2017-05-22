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

package org.teiid.embedded.helper.picketbox;

import org.jboss.security.AuthenticationManager;
import org.picketbox.config.PicketBoxConfiguration;
import org.picketbox.factories.SecurityFactory;
import org.teiid.embedded.helper.PicketboxHelper;

public class PicketboxHelperImpl implements PicketboxHelper {

    @Override
    public AuthenticationManager authenticationManager(String securityDomainName, String configFile) {
        SecurityFactory.prepare(); // switch to use picketbox xml based configuration
        PicketBoxConfiguration idtrustConfig = new PicketBoxConfiguration(); 
        idtrustConfig.load(configFile);
        AuthenticationManager authManager = SecurityFactory.getAuthenticationManager(securityDomainName);
        if(authManager == null) {
            throw new AuthenticationManagerNullException("Authentication Manager is null");
        }
        return authManager;
    }
    
    static class AuthenticationManagerNullException extends RuntimeException {

        private static final long serialVersionUID = -6500608980276363768L;
        
        public AuthenticationManagerNullException(String msg) {
            super(msg);
        }
    }

}
