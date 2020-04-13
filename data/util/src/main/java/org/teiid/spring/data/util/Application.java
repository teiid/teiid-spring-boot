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

package org.teiid.spring.data.util;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.common.ExternalSource;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class Application implements CommandLineRunner {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args).close();
    }

    @Override
    public void run(String... args) throws Exception {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(ConnectionFactoryConfiguration.class));
        Set<BeanDefinition> components = provider.findCandidateComponents("org.teiid.spring.data");
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());
                ConnectionFactoryConfiguration cfc = clazz.getAnnotation(ConnectionFactoryConfiguration.class);
                if(cfc != null) {
                    ExternalSource.addSource(ExternalSource.build(cfc, c.getBeanClassName()));
                }
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(ExternalSource.SOURCES));
    }
}
