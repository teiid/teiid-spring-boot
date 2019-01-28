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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceSchemaCreatedEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.config.SortedResourcesFactoryBean;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;
import org.teiid.core.TeiidRuntimeException;

/**
 * Bean to handle {@link DataSource} initialization by running
 * {@literal schema-*.sql} on {@link PostConstruct} and {@literal data-*.sql}
 * SQL scripts on a {@link DataSourceInitializedEvent}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 1.1.0
 * @see DataSourceAutoConfiguration
 */
class MultiDataSourceInitializer implements ApplicationListener<DataSourceSchemaCreatedEvent> {

    private static final Log logger = LogFactory.getLog(MultiDataSourceInitializer.class);

    private final ApplicationContext applicationContext;

    private DataSource dataSource;

    private boolean initialized = false;

    String sourceName;

    MultiDataSourceInitializer(DataSource dataSource, String sourceName, ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.sourceName = sourceName;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        if (this.dataSource == null) {
            logger.debug("No DataSource found so not initializing");
            return;
        }
        runSchemaScripts();
    }

    private void runSchemaScripts() {
        List<String> resources = null;
        if (getProperty("schema") == null) {
            resources = Collections.emptyList();
        } else {
            resources = Arrays.asList(getProperty("schema"));
        }
        List<Resource> scripts = getScripts("spring.datasource." + this.sourceName + ".schema", resources, "schema");
        if (!scripts.isEmpty()) {
            String username = getProperty("schema-username");
            String password = getProperty("schema-password");
            runScripts(scripts, username, password);
            try {
                this.applicationContext.publishEvent(new DataSourceSchemaCreatedEvent(this.dataSource));
                // The listener might not be registered yet, so don't rely on
                // it.
                if (!this.initialized) {
                    runDataScripts();
                    this.initialized = true;
                }
            } catch (IllegalStateException ex) {
                logger.warn("Could not send event to complete DataSource initialization (" + ex.getMessage() + ")");
            }
        }
    }

    @Override
    public void onApplicationEvent(DataSourceSchemaCreatedEvent event) {
        String initialize = getProperty("initialize");

        if (initialize != null && !Boolean.parseBoolean(initialize)) {
            logger.debug("Initialization disabled (not running data scripts)");
            return;
        }
        // NOTE the event can happen more than once and
        // the event datasource is not used here
        if (!this.initialized) {
            runDataScripts();
            this.initialized = true;
        }
    }

    private void runDataScripts() {
        List<String> resources = null;
        if (getProperty("data") == null) {
            resources = Collections.emptyList();
        } else {
            resources = Arrays.asList(getProperty("schema"));
        }
        List<Resource> scripts = getScripts("spring.datasource." + this.sourceName + ".data", resources, "data");
        String username = getProperty("data-username");
        String password = getProperty("data-password");
        runScripts(scripts, username, password);
    }

    List<Resource> getScripts(String propertyName, List<String> resources, String fallback) {
        if (resources != null && !resources.isEmpty()) {
            return getResources(propertyName, resources, true);
        }
        String platform = getProperty("platform");
        List<String> fallbackResources = new ArrayList<String>();
        fallbackResources.add("classpath*:" + fallback + "-" + platform + ".sql");
        fallbackResources.add("classpath*:" + fallback + ".sql");
        return getResources(propertyName, fallbackResources, false);
    }

    List<Resource> getResources(String propertyName, List<String> locations, boolean validate) {
        List<Resource> resources = new ArrayList<Resource>();
        for (String location : locations) {
            for (Resource resource : doGetResources(location)) {
                if (resource.exists()) {
                    resources.add(resource);
                } else if (validate) {
                    throw new TeiidRuntimeException(resource.getFilename() + " does not exist!");
                }
            }
        }
        return resources;
    }

    private Resource[] doGetResources(String location) {
        try {
            SortedResourcesFactoryBean factory = new SortedResourcesFactoryBean(this.applicationContext,
                    Collections.singletonList(location));
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load resources from " + location, ex);
        }
    }

    private String getProperty(String key) {
        String k = "spring.datasource." + this.sourceName + "." + key;
        String value = this.applicationContext.getEnvironment().getProperty(k);
        if (value == null) {
            k = "spring.xa.datasource." + this.sourceName + "." + key;
            value = this.applicationContext.getEnvironment().getProperty(k);
        }
        return value;
    }

    private void runScripts(List<Resource> resources, String username, String password) {
        if (resources.isEmpty()) {
            return;
        }
        boolean continueOnError = Boolean.parseBoolean(getProperty("continue-on-error"));
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(continueOnError);
        populator.setSeparator(getProperty("seperator"));

        if (getProperty("sql-script-encoding") != null) {
            populator.setSqlScriptEncoding(getProperty("sql-script-encoding"));
        }
        for (Resource resource : resources) {
            populator.addScript(resource);
        }
        DataSource dataSource = this.dataSource;
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            String driver = getProperty("driver-class-name");
            String url = getProperty("url");
            dataSource = DataSourceBuilder.create(this.getClass().getClassLoader()).driverClassName(driver).url(url)
                    .username(username).password(password).build();
        }
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
