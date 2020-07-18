/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.data.salesforce;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

import com.sforce.soap.partner.Connector;

@ConnectionFactoryConfiguration(
        alias = "salesforce",
        translatorName = "salesforce-41",
        otherAliases = {"salesforce-34", "salesforce-41"},
        configuration = SalesforceConfiguration.class
        )
public class SalesforceConnectionFactory implements BaseConnectionFactory<SalesforceConnectionImpl> {
    private static final Log logger = LogFactory.getLog(SalesforceConnectionFactory.class);

    private SalesforceConfiguration config;
    private volatile SalesforceConnectionImpl connection;

    public SalesforceConnectionFactory(SalesforceConfiguration config) {
        this.config = config;
        checkVersion(config);
    }

    @SuppressWarnings("deprecation")
    public void checkVersion(SalesforceConfiguration config) {
        try {
            String url = config.getUrl();
            String apiVersion = url.substring(url.lastIndexOf('/') + 1, url.length());
            Field f = Connector.class.getDeclaredField("END_POINT"); //$NON-NLS-1$
            f.setAccessible(true);
            if (f.isAccessible()) {
                String endPoint = (String) f.get(null);
                String javaApiVersion = endPoint.substring(endPoint.lastIndexOf('/') + 1, endPoint.length());
                if (!javaApiVersion.equals(apiVersion)) {
                    logger.warn("Accessing remote API version" + apiVersion + "with Java API version " + javaApiVersion
                            + "may not be compatible");
                }
            }
        } catch (Exception e) {

        }
    }
    
    @Override
    public SalesforceConnectionImpl getConnection() throws Exception {
        SalesforceConnectionImpl localInstance = connection;
        if (localInstance == null || !localInstance.isValid()) {
            synchronized (SalesforceConnectionImpl.class) {
                localInstance = connection;
                if (localInstance == null || !localInstance.isValid()) {
                    connection = localInstance = new SalesforceConnectionImpl(config);
                }
            }
        }
        return localInstance;
    }

    @Override
    public void close() throws IOException {
        // close connections
    }
}
