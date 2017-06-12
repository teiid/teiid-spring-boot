/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;

@Configuration
@PropertySource("classpath:teiid-driver.properties")
public class TeiidSpringDatasource implements Ordered {
    
   @Primary
   @Bean
   @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource teiidDataSource() {
        DataSource ds =  DataSourceBuilder.create().build();
        return ds;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE-10;
    }      
    
    /*
    @Primary
    @Bean
    @ConditionalOnBean(name = "teiid")
    public DataSource teiidDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.teiid.autoconfigure.TeiidSpringDriver");
        dataSource.setUrl("jdbc:teiid:spring");
        return dataSource;
        return DataSourceBuilder.create().driverClassName("org.teiid.autoconfigure.TeiidSpringDriver")
                .url("jdbc:teiid:spring").build();
    }    

    
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean userEntityManager() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(teiidDataSource());
 
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        //properties.put("hibernate.hbm2ddl.auto","none");
        properties.put("hibernate.dialect", "org.teiid.dialect.TeiidDialect");
        em.setJpaPropertyMap(properties);
 
        return em;
    }   
    */ 
}
