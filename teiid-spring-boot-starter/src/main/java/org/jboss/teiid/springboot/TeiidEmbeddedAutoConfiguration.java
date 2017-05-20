package org.jboss.teiid.springboot;

import java.util.Collections;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.security.SecurityHelper;
import org.teiid.translator.ExecutionFactory;
import org.teiid.transport.SocketConfiguration;

@Configuration
@ComponentScan
@EnableConfigurationProperties
public class TeiidEmbeddedAutoConfiguration {
    
    private final String DEFAULT_ADDRESS = "0.0.0.0";
    private final Integer DEFAULT_PORT = 31000;
    
    @Autowired(required = false)
    private Integer jdbcPort;
    
    @Autowired(required = false)
    private SocketConfiguration socketConfiguration;
    
    @Autowired(required = false)
    private EmbeddedConfiguration embeddedConfiguration;
    
    @Autowired(required = false)
    private TransactionManager transactionManager;
    
    @Autowired(required = false)
    private SecurityHelper securityHelper;
    
    @Autowired(required = false)
    private String securityDomain;
    
    @Autowired(required = false)
    private Map<String, ExecutionFactory<?, ?>> translators = Collections.emptyMap();
    
    @Autowired(required = false)
    private Map<String, Object> connectionFactories = Collections.emptyMap();
    
    @Bean
    public EmbeddedServer embeddedServer() {
        
        final EmbeddedServer server = new EmbeddedServer(); 
        
        translators.forEach((name, ef) -> server.addTranslator(name, ef));
        connectionFactories.forEach((name, factory) -> server.addConnectionFactory(name, factory));
        
        if(embeddedConfiguration == null) {
            embeddedConfiguration = new EmbeddedConfiguration();
        }
        
        if(socketConfiguration == null) {
            socketConfiguration = new SocketConfiguration();
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.jdbcPort == null ? DEFAULT_PORT : jdbcPort);
            embeddedConfiguration.addTransport(socketConfiguration);
        }
        
        if(embeddedConfiguration.getTransports() == null || embeddedConfiguration.getTransports().size() == 0) {
            socketConfiguration.setBindAddress(DEFAULT_ADDRESS);
            socketConfiguration.setPortNumber(this.jdbcPort == null ? DEFAULT_PORT : jdbcPort);
            embeddedConfiguration.addTransport(socketConfiguration);
        }
        
        if(transactionManager != null && embeddedConfiguration.getTransactionManager() == null) {
            embeddedConfiguration.setTransactionManager(transactionManager);
        }
        
        if(securityDomain != null) {
            embeddedConfiguration.setSecurityDomain(securityDomain);
        }
        
        if(securityHelper != null) {
            embeddedConfiguration.setSecurityHelper(securityHelper);
        }
                
        server.start(embeddedConfiguration);
        
        return server;
    }

}
