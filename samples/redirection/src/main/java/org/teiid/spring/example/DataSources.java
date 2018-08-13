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

package org.teiid.spring.example;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.teiid.spring.autoconfigure.xa.XADataSourceBuilder;

@Configuration
public class DataSources{
    @Autowired
    private XADataSourceWrapper wrapper;

    @Bean(name="sampledb.xa")
    @ConfigurationProperties(prefix="spring.xa.datasource.sampledb")
    public XADataSourceBuilder sampledbXA() throws Exception {
        return new XADataSourceBuilder();
    }

    @Bean(name="sampledb")
    public DataSource sampledb(@Qualifier("sampledb.xa") XADataSourceBuilder x) throws Exception {
        return x.build(wrapper, x.buildXA());
    }
    
    @Bean(name="redirected.xa")
    @ConfigurationProperties(prefix="spring.xa.datasource.redirected")
    public XADataSourceBuilder redirectedXA() throws Exception {
        return new XADataSourceBuilder();
    }

    @Bean(name="redirected")
    public DataSource redirected(@Qualifier("redirected.xa") XADataSourceBuilder x) throws Exception {
        return x.build(wrapper, x.buildXA());
    }

      
/*    @Bean(name="sampledb")
    @ConfigurationProperties(prefix = "spring.datasource.sampledb")
    public DataSource sampledb() throws Exception {
        return DataSourceBuilder.create().build();
    }
   
    @Bean(name="redirected")
    @ConfigurationProperties(prefix = "spring.datasource.redirected")
    public DataSource redirected() throws Exception {
        return DataSourceBuilder.create().build();   
    }  */ 
}
