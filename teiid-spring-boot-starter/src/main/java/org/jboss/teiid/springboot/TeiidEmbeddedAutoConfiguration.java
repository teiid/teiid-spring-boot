package org.jboss.teiid.springboot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.transport.SocketConfiguration;

@Configuration
@ComponentScan
@EnableConfigurationProperties
public class TeiidEmbeddedAutoConfiguration {
    
    private final String DEFAULT_ADDRESS = "0.0.0.0";
    private final Integer DEFAULT_PORT = 31000;
    
    @Bean
    public EmbeddedServer embeddedServer() {
        
        final EmbeddedServer server = new EmbeddedServer(); 
        SocketConfiguration socketConfiguration = new SocketConfiguration();
        socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
        socketConfiguration.setPortNumber(DEFAULT_PORT);
        EmbeddedConfiguration config = new EmbeddedConfiguration();
        config.addTransport(socketConfiguration);
        server.start(config);
        
        return server;
    }


}
