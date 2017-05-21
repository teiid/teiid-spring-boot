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
