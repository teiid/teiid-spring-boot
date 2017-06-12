package org.teiid.spring.example;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.teiid.autoconfigure.TeiidSpringDatasource;

@Configuration
public class DataSources extends TeiidSpringDatasource {

    @ConfigurationProperties(prefix = "spring.datasource.accountsDS")
    @Bean
    public DataSource accountsDS() {
        return DataSourceBuilder.create().build();
    }
    
    @ConfigurationProperties(prefix = "spring.datasource.customerDS")
    @Bean
    public DataSource customerDS() {
        return DataSourceBuilder.create().build();
    }    
}
