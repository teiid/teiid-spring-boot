package org.jboss.teiid.springboot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.teiid.resource.spi.BasicConnection;
import org.teiid.resource.spi.BasicConnectionFactory;
import org.teiid.resource.spi.BasicManagedConnectionFactory;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.Translator;

@Configuration
public class TestConfiguration {
    
    @Bean
    public Integer jdbcPort() {
        return 33333;
    }
    
    @Bean
    public Map<String, ExecutionFactory<?, ?>> translators() {
        Map<String, ExecutionFactory<?, ?>> efs = new HashMap<>();
        efs.put("test", new TestTranslator());
        return efs;
    }
    
    @Bean
    public Set<String> connectionFactoryNames() {
        Set<String> names = new HashSet<>();
        names.add("datasource");
        return names;
    }
    
    @Bean(name = "datasource")
    public DataSource initTestFactory() {
        return Mockito.mock(DataSource.class);
    }
    
    @Bean
    public Map<String, ConnectionFactory> connectionFactories() throws ResourceException {
        Map<String, ConnectionFactory> factories = new HashMap<>();
        factories.put("connFactory", new TestManagedConnectionFactory().createConnectionFactory());
        return factories;
    }
    
    @Bean
    public TransactionManager transactionManager() {
        return Mockito.mock(TransactionManager.class);
    }
    
    public static interface TestConnection extends Connection {
    }
    
    public static class TestConnectionImpl extends BasicConnection implements TestConnection {

        @Override
        public void close() throws ResourceException {            
        }
    }
    
    @SuppressWarnings("serial")
    public static class TestManagedConnectionFactory extends BasicManagedConnectionFactory {

        @Override
        public BasicConnectionFactory<TestConnectionImpl> createConnectionFactory() throws ResourceException {
            return new BasicConnectionFactory<TestConnectionImpl>() {

                @Override
                public TestConnectionImpl getConnection() throws ResourceException {
                    return new TestConnectionImpl();
                }};
        }
        
    }
    
    @Translator(name="test", description="Test Translator")
    public static class TestTranslator extends ExecutionFactory<ConnectionFactory, Connection>  {
    }

}
