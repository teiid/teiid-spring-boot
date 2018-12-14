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

package org.teiid.spring.data.rest;

import javax.resource.ResourceException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.client.RestTemplate;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.WSConnection;

@ConfigurationProperties(prefix="spring.teiid.ws")
public class RestConnectionFactory extends BaseConnectionFactory {
    private static final long serialVersionUID = 7082082147591076185L;

    @Autowired
    RestTemplate template;

    @Autowired
  private BeanFactory beanFactory;

    public RestConnectionFactory() {
        super.setTranslatorName("rest");
    }

    @Override
    public WSConnection getConnection() throws ResourceException {
        return new RestConnection(template, beanFactory);
    }
}
