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
package org.teiid.spring.data.google;

import java.util.concurrent.atomic.AtomicReference;

import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.google.api.metadata.SpreadsheetInfo;

public class SpreadsheetConnectionFactory extends BaseConnectionFactory<SpreadsheetConnectionImpl4> {
    private SpreadSheetConfiguration config;

    public SpreadsheetConnectionFactory(SpreadSheetConfiguration config) {
        super("google-spreadsheet");
        this.config = config;
    }

    @Override
    public SpreadsheetConnectionImpl4 getConnection() throws Exception {
        checkConfig();
        // share the spreadsheet info among all connections
        AtomicReference<SpreadsheetInfo> spreadsheetInfo = new AtomicReference<SpreadsheetInfo>();
        return new SpreadsheetConnectionImpl4(this.config, spreadsheetInfo);
    }

    private void checkConfig() {
        // SpreadsheetName should be set
        if ((config.getSpreadSheetName() == null || config.getSpreadSheetName().trim().equals("")) //$NON-NLS-1$
                && config.getSpreadSheetId() == null) {
            throw new IllegalStateException("SpreadsheetName or SpreadsheetId are required.");
        }

        if (config.getRefreshToken() == null || config.getRefreshToken().trim().equals("")) { //$NON-NLS-1$
            throw new IllegalStateException(
                    "OAuth2 requires refreshToken, clientId, and clientSecret. Check the Google Connector "
                            + "documentation on how to retrieve the RefreshToken."); //$NON-NLS-1$
        }

        if (config.getClientId() == null || config.getClientSecret() == null) {
            throw new IllegalStateException(
                    "OAuth2 requires refreshToken, clientId, and clientSecret. Check the Google "
                            + "Connector documentation on how to retrieve the RefreshToken."); //$NON-NLS-1$
        }
        if (config.getSpreadSheetId() == null) {
            throw new IllegalStateException("v4 requires the SpreadsheetId"); //$NON-NLS-1$
        }
    }
}
