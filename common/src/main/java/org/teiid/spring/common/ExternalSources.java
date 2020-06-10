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
package org.teiid.spring.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

public class ExternalSources implements Serializable{
    private static final long serialVersionUID = 4872582926073134433L;
    private Map<String, ExternalSource> items = Collections.synchronizedMap(new TreeMap<String, ExternalSource>());

    public ExternalSources() {
        loadConnctionFactories(this.getClass().getClassLoader(), "org.teiid.spring.data");
    }

    public void loadConnctionFactories(ClassLoader classloader, String packageName) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.setResourceLoader(new ResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                return null;
            }
            @Override
            public ClassLoader getClassLoader() {
                return classloader;
            }
        });
        provider.addIncludeFilter(new AnnotationTypeFilter(ConnectionFactoryConfiguration.class));
        Set<BeanDefinition> components = provider.findCandidateComponents(packageName);
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName(), false, classloader);
                ConnectionFactoryConfiguration cfc = clazz.getAnnotation(ConnectionFactoryConfiguration.class);
                if(cfc != null) {
                    ExternalSource source = build(cfc, c.getBeanClassName());
                    for (String name : cfc.otherAliases()) {
                        items.put(name, source);
                    }
                    items.put(source.getName(), source);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("failed to load " + c.getBeanClassName());
            }
        }
    }

    private static ExternalSource build(ConnectionFactoryConfiguration annotation, String className) {
        String dialect = annotation.dialect();
        boolean jdbc = annotation.jdbc();
        String prefix = annotation.prefix().isEmpty() ? "spring.teiid.data." + annotation.alias():annotation.prefix();
        String[] drivers = annotation.driverNames().length == 0 ? new String[] { className }: annotation.driverNames();
        String[] dataSources = annotation.datasourceNames().length == 0 ?new String[] {}: annotation.datasourceNames();
        ExternalSource source = new ExternalSource(annotation.alias(), drivers, dataSources,
                annotation.translatorName(), dialect.isEmpty() ? null : dialect, prefix, jdbc);
        return source;
    }

    public ExternalSource findByDriverName(String driverName) {
        for (ExternalSource source : this.items.values()) {
            for (String driver : source.getDriverNames()) {
                if (driver.equals(driverName)) {
                    return source;
                }
            }
            for (String driver : source.getDatasourceNames()) {
                if (driver.equals(driverName)) {
                    return source;
                }
            }
        }
        return null;
    }

    public ExternalSource find(String sourceName) {
        return this.items.get(sourceName);
    }

    public void addSource(ExternalSource source) {
        if (find(source.getName()) == null) {
            this.items.put(source.getName(), source);
        }
    }

    public void addSource(ConnectionFactoryConfiguration annotation, String className) {
        ExternalSource source = build(annotation, className);
        if (find(source.getName()) == null) {
            this.items.put(source.getName(), source);
        }
    }

    public Map<String, ExternalSource> getItems() {
        return items;
    }

    public static void main(String[] args) {
        ExternalSources sources = new ExternalSources();
        for (ExternalSource source : sources.items.values()) {
            if (!source.getTranslatorName().equals(source.getName())) {
                System.out.println(source.getTranslatorName() + " " + source.getName());
            }
        }
    }
}
