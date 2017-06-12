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

package org.jboss.teiid.springboot;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.teiid.autoconfigure.TeiidAutoConfiguration;
import org.teiid.runtime.EmbeddedServer;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TeiidAutoConfiguration.class, TestConfiguration.class})
public class TeiidEmbeddedAutoConfigurationTest {
    
    @Autowired
    EmbeddedServer embeddedServer;
    
    @Test
    public void testAutowired() {
        assertNotNull(embeddedServer);
    }
    /*
    @Test
    public void testConnection() throws Exception {
        Connection conn = getDriverConnection("org.teiid.jdbc.TeiidDriver", "jdbc:teiid:Portfolio@mm://localhost:33333;version=1", "teiidUser", "redhat");
        assertNotNull(conn);
        Statement stmt = conn.createStatement();
        assertNotNull(stmt);
        ResultSet rs = stmt.executeQuery("SELECT * FROM SYSADMIN.MatViews");
        assertNotNull(rs);
        assertTrue(rs.next());
        close(rs, stmt, conn);
    }
    
    @Test
    public void testTranslators() throws AdminException {
        Collection<?> translators = embeddedServer.getAdmin().getTranslators();
        assertEquals(1, translators.size());
    }
    
    @Ignore
    @Test
    public void testConnectors() throws AdminException {
        assertTrue(embeddedServer.getAdmin().getDataSourceNames().contains("datasource"));
        assertTrue(embeddedServer.getAdmin().getDataSourceNames().contains("connFactory"));
    }
    
    @Test
    public void testTransaction() throws Exception {
        Connection conn = getDriverConnection("org.teiid.jdbc.TeiidDriver", "jdbc:teiid:Portfolio@mm://localhost:33333;version=1", "teiidUser", "redhat");
        conn.setAutoCommit(false);
        conn.commit();
        conn.close();
    }
    
    static class JDBCUtils {
        
        public static Connection getDriverConnection(String driver, String url, String user, String pass) throws Exception {
            Class.forName(driver);
            return DriverManager.getConnection(url, user, pass); 
        }
        
        public static void close(ResultSet rs, Statement stmt, Connection conn) throws SQLException {
            rs.close();
            stmt.close();
            conn.close();
        }
    }
  */

}
