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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.teiid.salesforce.BaseSalesforceConnection;
import org.teiid.translator.salesforce.SalesforceConnection;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

public class SalesforceConnectionImpl extends BaseSalesforceConnection<SalesforceConfiguration> implements SalesforceConnection {
    private static final Log logger = LogFactory.getLog(SalesforceConnectionImpl.class);
    private PartnerConnection partnerConnection;
    private SalesforceConnectorConfig config;

    public SalesforceConnectionImpl(SalesforceConfiguration sfc) throws Exception {
        super(sfc);
    }

    @Override
    protected void login(SalesforceConfiguration sfc) throws ConnectionException {
        config = new SalesforceConnectorConfig();
        config.setCompression(true);
        config.setTraceMessage(false);
        config.setUsername(sfc.getUsername());
        config.setPassword(sfc.getPassword());
        config.setAuthEndpoint(sfc.getUrl());
        config.setRestTemplate(sfc.getRestTemplate());
        config.setOAuth2Template(sfc.getOAuth2Template());
        config.setRefreshToken(sfc.getRefreshToken());

        if (sfc.getConnectTimeout() != null) {
            config.setConnectionTimeout((int)Math.min(Integer.MAX_VALUE, sfc.getConnectTimeout()));
        }

        if (sfc.getRequestTimeout() != null) {
            config.setReadTimeout((int)Math.min(Integer.MAX_VALUE, sfc.getRequestTimeout()));
        }

        partnerConnection = new TeiidPartnerConnection(config);

        logger.trace("Login was successful"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        //could make a TeiidPartner connection interface method for this
        if (partnerConnection instanceof TeiidPartnerConnection
                && !((TeiidPartnerConnection)partnerConnection).isAccessTokenValid()) {
            return false;
        }
        return super.isValid();
    }

}
