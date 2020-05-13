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


import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

public class EnvironmentPropertiesPostProcessor implements EnvironmentPostProcessor {
    private static final String LOG_PREFIX = "LOGGING_LEVEL_ORG_TEIID_";
    private static List<String> PROPERTIES = Arrays.asList(
            "LOGGING_LEVEL_ORG_TEIID_COMMAND_LOG",
            "LOGGING_LEVEL_ORG_TEIID_AUDIT_LOG",
            "LOGGING_LEVEL_ORG_TEIID_COMMAND_LOG_SOURCE",
            "LOGGING_LEVEL_ORG_TEIID_RUNTIME",
            "LOGGING_LEVEL_ORG_TEIID_TXN_LOG",
            "LOGGING_LEVEL_ORG_TEIID_CONNECTOR",
            "LOGGING_LEVEL_ORG_TEIID_PLANNER",
            "LOGGING_LEVEL_ORG_TEIID_PROCESSOR",
            "LOGGING_LEVEL_ORG_TEIID_BUFFER_MGR",
            "LOGGING_LEVEL_ORG_TEIID");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        PropertySource<?> system = environment.getPropertySources().get(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        Map<String, Object> prefixed = new LinkedHashMap<>();
        for (String property: PROPERTIES) {
            if (system.containsProperty(property)) {
                if (property.startsWith(LOG_PREFIX)) {
                    prefixed.put("logging.level.org.teiid." + property.substring(LOG_PREFIX.length()),
                            system.getProperty(property));
                } else if (property.equals("LOGGING_LEVEL_ORG_TEIID")) {
                    prefixed.put("logging.level.org.teiid", system.getProperty(property));
                }
            }
        }
        environment.getPropertySources().addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource("prefixer", prefixed));
    }
}
