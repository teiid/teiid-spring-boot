/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.teiid.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.metadata.Database;
import org.teiid.metadata.Datatype;
import org.teiid.query.metadata.DatabaseStore;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.query.parser.QueryParser;

public class PluginDatabaseStore extends DatabaseStore {
    static class VdbImport {
        String dbName;
        String version;
        boolean importPolicies;
    }
    private List<VdbImport> vdbImports = new ArrayList<>();

    public List<VdbImport> getVdbImports() {
        return vdbImports;
    }
    @Override
    public Map<String, Datatype> getRuntimeTypes() {
        return SystemMetadata.getInstance().getRuntimeTypeMap();
    }
    @Override
    public void importSchema(String schemaName, String serverType, String serverName, String foreignSchemaName,
            List<String> includeTables, List<String> excludeTables, Map<String, String> properties) {
        // ignore
    }
    @Override
    public Database getCurrentDatabase() {
        return super.getCurrentDatabase();
    }
    @Override
    public void importDatabase(String dbName, String version, boolean importPolicies) {
        VdbImport vdb = new VdbImport();
        vdb.dbName = dbName;
        vdb.version = version;
        vdb.importPolicies = importPolicies;
        vdbImports.add(vdb);
    }

    public Database parse(File vdbfile) throws IOException {
        String vdbStr = ObjectConverterUtil.convertToString(new FileInputStream(vdbfile));
        StringReader reader = new StringReader(vdbStr);
        try {
            startEditing(false);
            setMode(Mode.ANY);
            QueryParser.getQueryParser().parseDDL(this, reader);
            return getDatabases().get(0);
        } finally {
            reader.close();
            stopEditing();
        }
    }

    public Database db() {
        return getDatabases().get(0);
    }
}
