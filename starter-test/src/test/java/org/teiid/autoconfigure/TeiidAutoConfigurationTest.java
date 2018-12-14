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

package org.teiid.autoconfigure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.teiid.autoconfigure.JDBCUtils.close;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.teiid.adminapi.VDB.Status;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.spring.autoconfigure.TeiidAutoConfiguration;
import org.teiid.spring.autoconfigure.TeiidConstants;
import org.teiid.spring.autoconfigure.TeiidServer;
import org.teiid.spring.configuration.TestConfiguration;
import org.teiid.translator.TranslatorException;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TeiidAutoConfiguration.class, TestConfiguration.class})
public class TeiidAutoConfigurationTest {

    @Autowired
    TeiidServer teiidServer;

    @Autowired
    VDBMetaData vdbMetaData;

    @Autowired
    DataSource datasource;

    @Test
    public void testAutowired() {
        assertNotNull(teiidServer);
        assertNotNull(vdbMetaData);
        assertNotNull(datasource);
    }

    @Test
    public void testSpringVDB() throws SQLException {

        assertEquals(Status.ACTIVE, vdbMetaData.getStatus());
        assertEquals(TeiidConstants.VDBNAME, vdbMetaData.getName());
        assertEquals(TeiidConstants.VDBVERSION, vdbMetaData.getVersion());

        Connection conn = teiidServer.getDriver().connect("jdbc:teiid:spring", null);
        testConnection(conn);
    }

    @Test
    public void testXmlVDBDeployment() throws VirtualDatabaseException, ConnectorManagerException, TranslatorException, IOException, SQLException {

        InputStream is = TeiidAutoConfigurationTest.class.getClassLoader().getResourceAsStream("empty-vdb.xml");
        teiidServer.deployVDB(is);
        Connection conn = teiidServer.getDriver().connect("jdbc:teiid:Portfolio", null);
        testConnection(conn);
    }

    private void testConnection(Connection conn) throws SQLException {
        assertNotNull(conn);
        Statement stmt = conn.createStatement();
        assertNotNull(stmt);
        ResultSet rs = stmt.executeQuery("SELECT * FROM SYSADMIN.MatViews");
        assertNotNull(rs);
        assertTrue(rs.next());
        close(rs, stmt, conn);
    }

    @Test
    public void testTeiidSpringDatasource() throws SQLException {
        Connection conn = datasource.getConnection();
        testConnection(conn);
    }

}
