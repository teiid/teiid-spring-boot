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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.teiid.core.TeiidProcessingException;
import org.teiid.jdbc.ConnectionImpl;
import org.teiid.odbc.ODBCServerRemoteImpl;
import org.teiid.olingo.service.LocalClient;
import org.teiid.spring.autoconfigure.TeiidServer;

public class SpringClient extends LocalClient {
    private String vdbName;
    private String vdbVersion;
    private ConnectionImpl connection;
    private Properties properties;
    private TeiidServer server;

    public SpringClient(String vdbName, String vdbVersion, Properties properties, TeiidServer server) {
        super(vdbName, vdbVersion, properties);
        this.properties = properties;
        this.vdbName = vdbName;
        this.vdbVersion = vdbVersion;
        this.server = server;
    }

    @Override
    public Connection open() throws SQLException, TeiidProcessingException {
        this.connection = buildConnection(server.getDriver(), this.vdbName, this.vdbVersion, this.properties);
        ODBCServerRemoteImpl.setConnectionProperties(connection);
        ODBCServerRemoteImpl.setConnectionProperties(connection, this.properties);
        getVDB();
        return this.connection;
    }

    @Override
    public void close() throws SQLException {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    @Override
    public ConnectionImpl getConnection() {
        return this.connection;
    }
}
