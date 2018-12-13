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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.impl.VDBMetaData;

public class TeiidSpringDriver implements Driver {
    private Driver delegate;
    private TeiidServer server;
    private VDBMetaData vdb;

    public TeiidSpringDriver(Driver d, TeiidServer server, VDBMetaData vdb) {
        this.delegate = d;
        this.server = server;
        this.vdb = vdb;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return delegate.acceptsURL(url);
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        boolean exists = false;
        try {
            exists = server.getAdmin().getVDB(vdb.getName(), vdb.getVersion()) != null;
        } catch (AdminException e) {
            // ignore.
        }
        if (!vdb.getName().equals(TeiidConstants.VDBNAME) && exists) {
            url = url.replace(TeiidConstants.VDBNAME, vdb.getName());
        }
        return delegate.connect(url, info);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return delegate.getPropertyInfo(url, info);
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }
}
