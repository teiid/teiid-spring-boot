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

package org.teiid.embedded.helper;

import org.jboss.security.AuthenticationManager;
import org.teiid.embedded.helper.picketbox.PicketboxHelperImpl;

public interface PicketboxHelper {
    
    /**
     * Initializes a AuthenticationManager.
     * @param securityDomainName - the security domain name which mapped with name in configFile
     * @param authConfig - Authentication configuration file
     * @return
     */
    AuthenticationManager authenticationManager(String securityDomainName, String configFile);
    
    PicketboxHelper Factory = new PicketboxHelperImpl();

}
