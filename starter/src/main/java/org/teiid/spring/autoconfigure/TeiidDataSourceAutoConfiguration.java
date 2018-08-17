package org.teiid.spring.autoconfigure;

import java.sql.Driver;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.ExecutionFactory;

@Configuration
@ConditionalOnClass({EmbeddedServer.class, ExecutionFactory.class})
@AutoConfigureAfter(TeiidAutoConfiguration.class)
public class TeiidDataSourceAutoConfiguration {

    @Autowired
    ApplicationContext context;

    @Bean(name="dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource getDataSource(TeiidServer server) {
        EmbeddedDatabaseFactory edf = new EmbeddedDatabaseFactory();
        edf.setDatabaseConfigurer(new TeiidDatabaseConfigurer(server));
        edf.setDataSourceFactory(new DataSourceFactory() {
            @Override
            public DataSource getDataSource() {
                String url = context.getEnvironment().getProperty("spring.datasource.teiid.url");
                return new SimpleDriverDataSource(server.getDriver(), url);
            }
            
            @Override
            public ConnectionProperties getConnectionProperties() {
                return new ConnectionProperties() {
                    @Override
                    public void setDriverClass(Class<? extends Driver> driverClass) {
                    }
                    @Override
                    public void setUrl(String url) {
                    }
                    @Override
                    public void setUsername(String username) {
                    }
                    @Override
                    public void setPassword(String password) {
                    }
                };
            }
        });
        return edf.getDatabase();
    }      
}
