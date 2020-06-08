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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.teiid.google.SpreadsheetConnectionImpl4;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.translator.google.api.metadata.SpreadsheetInfo;

@ConnectionFactoryConfiguration(
        alias = "google-spreadsheet",
        translatorName = "google-spreadsheet"
        )
public class SpreadsheetConnectionFactory implements BaseConnectionFactory<SpreadsheetConnectionImpl4> {
    private SpreadSheetConfiguration config;
    // share the spreadsheet info among all connections
    private AtomicReference<SpreadsheetInfo> spreadsheetInfo = new AtomicReference<SpreadsheetInfo>();

    public SpreadsheetConnectionFactory(SpreadSheetConfiguration config) {
        this.config = config;
    }

    @Override
    public SpreadsheetConnectionImpl4 getConnection() throws Exception {
        checkConfig();
        return new SpreadsheetConnectionImpl4(this.config, spreadsheetInfo);
    }

    private void checkConfig() {
        // SpreadsheetName should be set
        if (config.getSpreadsheetId() == null && config.getSpreadsheets() == null) {
            throw new IllegalStateException("SpreadsheetId or Spreadsheets are required.");
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
    }

    @Override
    public void close() throws IOException {
        //close connections
    }
}
