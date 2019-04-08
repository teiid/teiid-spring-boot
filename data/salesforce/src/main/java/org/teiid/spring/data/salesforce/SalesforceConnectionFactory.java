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

import java.lang.reflect.Field;

import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.salesforce.SalesForcePlugin;

import com.sforce.soap.partner.Connector;


public class SalesforceConnectionFactory extends BaseConnectionFactory<SalesforceConnectionImpl> {
    private SalesforceConfiguration config;
    private SalesforceConnectionImpl connection;

    public SalesforceConnectionFactory(SalesforceConfiguration config) {
        setTranslatorName("salesforce");
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
            if(f.isAccessible()){
                String endPoint = (String) f.get(null);
                String javaApiVersion = endPoint.substring(endPoint.lastIndexOf('/') + 1, endPoint.length());
                if (!javaApiVersion.equals(apiVersion)) {
                    LogManager.logWarning(LogConstants.CTX_CONNECTOR, SalesForcePlugin.Util.gs(SalesForcePlugin.Event.TEIID13009, apiVersion, javaApiVersion));
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public SalesforceConnectionImpl getConnection() throws Exception {
        if (connection == null || !connection.isValid()) {
            this.connection =  new SalesforceConnectionImpl(config);
        }
        return this.connection;
    }
}
