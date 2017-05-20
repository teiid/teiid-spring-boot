package org.jboss.teiid.springboot;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.teiid.runtime.EmbeddedServer;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TeiidEmbeddedAutoConfiguration.class})
public class TeiidEmbeddedAutoConfigurationTest {
    
    @Autowired
    EmbeddedServer embeddedServer;
    
    @Test
    public void testAutowired() {
        assertNotNull(embeddedServer);
    }

}
