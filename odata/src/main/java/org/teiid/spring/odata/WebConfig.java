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
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.spring.autoconfigure.TeiidServer;
import org.teiid.spring.identity.SpringSecurityHelper;

@EnableWebMvc
@Configuration
@PropertySource("classpath:odata.properties")
public class WebConfig implements WebMvcConfigurer {

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

    @Autowired
    ServletContext servletContext;

    @Autowired
    SpringSecurityHelper securityHelper;

    @Value("${spring.teiid.odata.alt.paths:#{null}}")
    private String[] alternatePaths;

    private Properties props = new Properties();

    @PostConstruct
    private void init() {
        addProperty(BATCH_SIZE, BATCH_SIZE_VALUE);
        addProperty(SKIPTOKEN_CACHE_TIME, SKIPTOKEN_CACHE_TIME_VALUE);
    }

    private void addProperty(String key, String defalt) {
        String value = context.getEnvironment().getProperty("spring.teiid.odata."+key, defalt);
        this.props.setProperty(key, value);
    }

    @Bean
    SpringODataFilter getOdataFilter() {
        return new SpringODataFilter(this.props, this.server, this.vdb, this.servletContext, this.securityHelper);
    }

    @Bean
    AuthenticationInterceptor getAuthInterceptor() {
        return new AuthenticationInterceptor(securityHelper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        ArrayList<String> exclude = new ArrayList<String>();
        exclude.add("/static/**");
        if (this.alternatePaths != null) {
            for(int i = 0; i < alternatePaths.length; i++) {
                exclude.add(this.alternatePaths[i]+"/**");
            }
        }

        String[] excludes = exclude.toArray(new String[exclude.size()]);

        registry.addInterceptor(getAuthInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns(excludes);

        registry.addInterceptor(getOdataFilter())
        .addPathPatterns("/**")
        .excludePathPatterns(excludes);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
        .allowedMethods("GET,POST,PUT,PATCH,DELETE")
        .allowedHeaders("Content-Type,Accept,Origin,Authorization")
        .allowCredentials(true).maxAge(1800);
    }
}
