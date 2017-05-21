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
