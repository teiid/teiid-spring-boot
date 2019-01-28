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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.teiid.spring.autoconfigure.MultiDataSourceTransactionManagement;

@Configuration
public class DataSources extends MultiDataSourceTransactionManagement {
    /*
    @Bean(name="sampledb")
    @Primary
    @ConfigurationProperties(prefix="spring.xa.datasource.sampledb")
    public XADataSource sampledbXA() throws Exception {
        return XADataSourceBuilder.create().build();
    }

    @Bean(name="redirected")
    @ConfigurationProperties(prefix="spring.xa.datasource.redirected")
    public XADataSource redirectedXA() throws Exception {
        return XADataSourceBuilder.create().build();
    }
     */

    @Bean(name="sampledb")
    @ConfigurationProperties(prefix = "spring.datasource.sampledb")
    public DataSource sampledb() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name="redirected")
    @ConfigurationProperties(prefix = "spring.datasource.redirected")
    public DataSource redirected() {
        return DataSourceBuilder.create().build();
    }
}
