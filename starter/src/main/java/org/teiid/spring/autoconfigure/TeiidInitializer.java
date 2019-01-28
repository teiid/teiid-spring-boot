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
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.config.SortedResourcesFactoryBean;

/**
 * Bean to handle {@link TeiidServer} initialization by running
 * {@literal teiid.ddl} on {@link PostConstruct} on a
 * {@link TeiidInitializedEvent}.
 *
 * Code borrowed from {@link DataSourceInitializedEvent}
 */
class TeiidInitializer implements ApplicationListener<TeiidInitializedEvent> {

    private static final Log logger = LogFactory.getLog(TeiidInitializer.class);

    private final ApplicationContext applicationContext;

    private TeiidServer teiidServer;

    private boolean initialized = false;

    TeiidInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        if (this.applicationContext.getBeanNamesForType(TeiidServer.class, false, false).length > 0) {
            this.teiidServer = this.applicationContext.getBean(TeiidServer.class);
        }
        if (this.teiidServer == null) {
            logger.debug("No DataSource found so not initializing");
            return;
        }
        runSchemaScripts();
    }

    private void runSchemaScripts() {

    }

    @Override
    public void onApplicationEvent(TeiidInitializedEvent event) {
        // NOTE the event can happen more than once and
        // the event datasource is not used here
        if (!this.initialized) {
            runDataScripts();
            this.initialized = true;
        }
    }

    private void runDataScripts() {
        // none right now.
    }

    static List<Resource> getScripts(String propertyName, String vdb, String fallback, ApplicationContext context) {
        if (vdb == null) {
            vdb = fallback;
        }
        List<String> fallbackResources = new ArrayList<String>();
        fallbackResources.add("classpath*:" + vdb);
        return getResources(propertyName, fallbackResources, context);
    }

    private static List<Resource> getResources(String propertyName, List<String> locations,
            ApplicationContext context) {
        List<Resource> resources = new ArrayList<Resource>();
        for (String location : locations) {
            for (Resource resource : doGetResources(location, context)) {
                if (resource.exists()) {
                    resources.add(resource);
                }
            }
        }
        return resources;
    }

    private static Resource[] doGetResources(String location, ApplicationContext context) {
        try {
            SortedResourcesFactoryBean factory = new SortedResourcesFactoryBean(context,
                    Collections.singletonList(location));
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load resources from " + location, ex);
        }
    }
}

