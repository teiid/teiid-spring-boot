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

package org.jboss.teiid.springboot.sample;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

@SpringBootApplication
public class ExampleMain {
    
    @Bean
    public Set<String> connectionFactoryNames() {
        Set<String> names = new HashSet<>();
        names.add("account-ds");
        return names;
    }

    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean(name = "account-ds")
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean(name = "narayanaConfiguration")
    public CoreEnvironmentBean narayanaConfiguration() throws CoreEnvironmentBeanException {
        arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(UUID.randomUUID().toString());
        return arjPropertyManager.getCoreEnvironmentBean();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ExampleMain.class, args);
    }
}
