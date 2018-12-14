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

package org.teiid.spring.odata;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.olingo.web.CorsFilter;
import org.teiid.olingo.web.ODataServlet;
import org.teiid.olingo.web.StaticContentServlet;
import org.teiid.olingo.web.gzip.GzipFilter;
import org.teiid.spring.autoconfigure.TeiidServer;

@Configuration
@ConditionalOnClass({TeiidServer.class})
@ConditionalOnBean(name= {"teiid"})
public class ODataConfiguration {
    private static String URL_MAPPING = "/*";
    private static String URL_MAPPING_STATIC = "/static/*";

    private static String BATCH_SIZE = "batch-size";
    private static String BATCH_SIZE_VALUE = "256";
    private static String SKIPTOKEN_CACHE_TIME = "skiptoken-cache-time";
    private static String SKIPTOKEN_CACHE_TIME_VALUE = "300000";

    @Autowired
    ApplicationContext context;

    @Autowired
    TeiidServer server;

    @Autowired
    VDBMetaData vdb;

    @Bean
    public FilterRegistrationBean odataFilters() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        addProperty(registrationBean, BATCH_SIZE, BATCH_SIZE_VALUE);
        addProperty(registrationBean, SKIPTOKEN_CACHE_TIME, SKIPTOKEN_CACHE_TIME_VALUE);

        CorsFilter corsFilter = new CorsFilter();
        GzipFilter gzipFilter = new GzipFilter();
        SpringODataFilter odataFilter = new SpringODataFilter(server, vdb);

        registrationBean.setFilter(corsFilter);
        registrationBean.setFilter(gzipFilter);
        registrationBean.setFilter(odataFilter);

        ArrayList<String> match = new ArrayList<>();
        match.add(URL_MAPPING);
        registrationBean.setUrlPatterns(match);
        return registrationBean;
    }

    private void addProperty(FilterRegistrationBean bean, String key, String defalt) {
      String value = context.getEnvironment().getProperty("spring.teiid.odata."+key, defalt);
      bean.addInitParameter(key, value);
    }

    @Bean
    public ServletRegistrationBean odataServlet() {
        return new ServletRegistrationBean(new ODataServlet(), URL_MAPPING);
    }

    @Bean
    public ServletRegistrationBean staticContentServlet() {
        return new ServletRegistrationBean(new StaticContentServlet(), URL_MAPPING_STATIC);
    }
}
