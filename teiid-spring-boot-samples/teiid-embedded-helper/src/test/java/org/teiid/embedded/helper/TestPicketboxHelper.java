package org.teiid.embedded.helper;

import static org.junit.Assert.*;

import java.security.Principal;

import javax.security.auth.Subject;

import org.jboss.security.AuthenticationManager;
import org.jboss.security.SimplePrincipal;
import org.junit.Test;

public class TestPicketboxHelper {

    @Test
    public void testAuthentication() {
        AuthenticationManager authManager = EmbeddedHelper.Factory.authenticationManager("sample-file", "config/sample-file.xml");
        Subject subject = new Subject();
        Principal principal = new SimplePrincipal("kylin");
        String credential = new String("password");
        boolean isValid = authManager.isValid(principal, credential, subject);
        assertTrue(isValid);
        
        subject = new Subject();
        principal = new SimplePrincipal("testUser");
        credential = new String("password");
        isValid = authManager.isValid(principal, credential, subject);
        assertTrue(isValid);
        
        subject = new Subject();
        principal = new SimplePrincipal("testUser-");
        credential = new String("password");
        isValid = authManager.isValid(principal, credential, subject);
        assertFalse(isValid);
    }
}
