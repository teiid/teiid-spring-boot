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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.spring.data.BaseConnection;
import org.teiid.spring.data.google.dataprotocol.GoogleDataProtocolAPI;
import org.teiid.spring.data.google.v4.GoogleCredentialUtil;
import org.teiid.spring.data.google.v4.SheetsAPI;
import org.teiid.spring.data.google.v4.SpreadsheetMetadataExtractor;
import org.teiid.translator.google.api.GoogleSpreadsheetConnection;
import org.teiid.translator.google.api.UpdateSet;
import org.teiid.translator.google.api.metadata.SpreadsheetInfo;
import org.teiid.translator.google.api.result.RowsResult;
import org.teiid.translator.google.api.result.UpdateResult;

/**
 * Represents a connection to an Google spreadsheet data source.
 *
 * Uses a mixture of Sheets v4 api, visualization, and Sheets v3
 */
public class SpreadsheetConnectionImpl4 implements BaseConnection, GoogleSpreadsheetConnection {
    private static final Log logger = LogFactory.getLog(SpreadsheetConnectionImpl4.class);
    private SpreadSheetConfiguration config;
    private SheetsAPI sheetsAPI = null; // v4 specific
    private GoogleDataProtocolAPI googleDataProtocolAPI; // visualization api
    private AtomicReference<SpreadsheetInfo> spreadsheetInfo;

    public SpreadsheetConnectionImpl4(SpreadSheetConfiguration config,
            AtomicReference<SpreadsheetInfo> spreadsheetInfo) {
        this.config = config;
        this.spreadsheetInfo = spreadsheetInfo;

        String refreshToken = config.getRefreshToken().trim();
        GoogleCredentialUtil credentialUtil = new GoogleCredentialUtil(refreshToken, config.getClientId(),
                config.getClientSecret());
        sheetsAPI = new SheetsAPI(credentialUtil);
        googleDataProtocolAPI = new GoogleDataProtocolAPI();
        googleDataProtocolAPI.setCredentialUtil(credentialUtil);
        logger.debug("Initializing Google SpreadSheet Connection");
    }

    @Override
    public void close() {
        logger.debug("Closing Google SpreadSheet Connection");
    }

    @Override
    public RowsResult executeQuery(String worksheetTitle, String query, Integer offset, Integer limit, int batchSize) {
        return googleDataProtocolAPI.executeQuery(getSpreadsheetInfo(), worksheetTitle, query,
                Math.min(batchSize, config.getBatchSize()), offset, limit);
    }

    @Override
    public SpreadsheetInfo getSpreadsheetInfo() {
        SpreadsheetInfo info = spreadsheetInfo.get();
        if (info == null) {
            synchronized (spreadsheetInfo) {
                info = spreadsheetInfo.get();
                if (info == null) {
                    SpreadsheetMetadataExtractor metadataExtractor = new SpreadsheetMetadataExtractor(sheetsAPI,
                            googleDataProtocolAPI);
                    info = metadataExtractor.extractMetadata(config.getSpreadSheetId());
                    spreadsheetInfo.set(info);
                }
            }
        }
        return info;
    }

    @Override
    public UpdateResult updateRows(String worksheetTitle, String criteria, List<UpdateSet> set) {
        throw new TeiidRuntimeException("Update is not implemented");
    }

    @Override
    public UpdateResult deleteRows(String worksheetTitle, String criteria) {
        throw new TeiidRuntimeException("Update is not implemented");
    }

    @Override
    public UpdateResult executeRowInsert(String worksheetTitle, Map<String, Object> pairs) {
        return sheetsAPI.insert(getSpreadsheetInfo().getSpreadsheetKey(), pairs,
                getSpreadsheetInfo().getWorksheetByName(worksheetTitle));
    }
}
