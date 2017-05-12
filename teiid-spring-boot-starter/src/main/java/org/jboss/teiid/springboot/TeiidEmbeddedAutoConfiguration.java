package org.jboss.teiid.springboot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.teiid.runtime.EmbeddedServer;

@Configuration
@ComponentScan
@EnableConfigurationProperties
public class TeiidEmbeddedAutoConfiguration {
    
    @Bean
    public EmbeddedServer embeddedServer() {
        
        final EmbeddedServer server = new EmbeddedServer();
        
        return server;
    }

}
