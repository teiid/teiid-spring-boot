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
package org.teiid.spring;

import java.sql.SQLException;
import java.util.Properties;

import org.teiid.jdbc.ConnectionImpl;
import org.teiid.jdbc.ConnectionProfile;
import org.teiid.jdbc.TeiidDriver;
import org.teiid.jdbc.TeiidSQLException;
import org.teiid.spring.autoconfigure.TeiidAutoConfiguration;
import org.teiid.spring.autoconfigure.TeiidServer;

public class TeiidSpringDriver extends TeiidDriver {
    @Override
    public ConnectionImpl connect(String url, Properties info) throws SQLException {
        super.setLocalProfile(new ConnectionProfile() {
            @Override
            public ConnectionImpl connect(String url, Properties info) throws TeiidSQLException {
                try {
                    TeiidServer server = TeiidAutoConfiguration.serverContext.get();
                    return server.getDriver().connect(url, info);
                } catch (SQLException e) {
                    throw TeiidSQLException.create(e);
                }
            }
        });
        return super.connect(url, info);
    }    
}
