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
package org.teiid.spring.autoconfigure;

import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.type.MethodMetadata;
import org.springframework.stereotype.Component;
import org.teiid.spring.data.BaseConnectionFactory;

@Component
/**
 * This classes aim is figure out all the data source bean defined by the
 * application, and then define them as the dependency on the main "dataSource"
 * bean. Code inspired by,
 * https://stackoverflow.com/questions/44453723/is-there-a-revert-to-spring-dependson-annotation
 */
public class TeiidBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static final Log logger = LogFactory.getLog(TeiidBeanDefinitionPostProcessor.class);

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ArrayList<String> datasources = new ArrayList<String>();
        BeanDefinition datasourceBeanDefinition = registry.getBeanDefinition("dataSource");
        for (String beanName : registry.getBeanDefinitionNames()) {
            if (beanName.startsWith("org.springframework") || beanName.contentEquals("dataSource")) {
                continue;
            }
            final BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (isMatch(beanDefinition, beanName)) {
                datasources.add(beanName);
            }
        }
        logger.info("Found data sources: " + datasources);
        datasourceBeanDefinition.setDependsOn(datasources.toArray(new String[datasources.size()]));
    }

    private boolean isMatch(BeanDefinition beanDefinition, String beanName) {
        String className = beanDefinition.getBeanClassName();
        if (className == null) {
            if (beanDefinition.getFactoryMethodName() != null &&
                    beanDefinition.getFactoryMethodName().contentEquals(beanName)) {
                Object source = beanDefinition.getSource();
                if (source instanceof MethodMetadata) {
                    String returnType = ((MethodMetadata) source).getReturnTypeName();
                    if (returnType.contentEquals("javax.sql.DataSource")) {
                        return true;
                    }
                    if (returnType.startsWith("org.springframework") || returnType.startsWith("io.micrometer")
                            || returnType.startsWith("com.fasterxml.") || returnType.startsWith("org.hibernate.")) {
                        return false;
                    }
                    className = returnType;
                }
            }
        }

        if (className != null) {
            try {
                final Class<?> beanClass = Class.forName(className);
                if (DataSource.class.isAssignableFrom(beanClass)
                        || BaseConnectionFactory.class.isAssignableFrom(beanClass)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return false;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
