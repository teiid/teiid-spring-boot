package org.teiid.embedded.helper;

import static org.junit.Assert.*;
import static org.teiid.embedded.helper.utils.JDBCUtils.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;

public class TestEmbeddedHelper {
    
    @Test
    public void testNoTxDataSource() throws Exception {
        
        DataSource ds = EmbeddedHelper.Factory.newNoTxDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }));
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        Statement stmt = conn.createStatement();
        assertNotNull(stmt);
        ResultSet rs = stmt.executeQuery("SELECT DATABASE ()");
        assertTrue(rs.next());
        assertEquals("TEST", rs.getString(1));
        close(stmt, conn);
    }
    
    @Test
    public void testNoTxDataSource_1() throws Exception {
        
        DataSource ds = EmbeddedHelper.Factory.newNoTxDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }).poolConfiguration(p -> {
            p.setMaxSize(30);
            p.setMinSize(5);
            p.setBlockingTimeout(30000);
            p.setIdleTimeoutMinutes(10);
        }));
        
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        close(conn);
    }

    @Test
    public void testNoTxDataSource_2() throws Exception {
        
        DataSource ds = EmbeddedHelper.Factory.newNoTxDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }));
        Connection conn = ds.getConnection();
        conn.setAutoCommit(false);
        conn.commit();
    }
    
    @Test
    public void testDataSource() throws Exception {
        DataSource ds = EmbeddedHelper.Factory.newDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }).poolConfiguration(p -> {
            p.setMaxSize(30);
            p.setMinSize(5);
            p.setBlockingTimeout(30000);
            p.setIdleTimeoutMinutes(10);
        }));
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        Statement stmt = conn.createStatement();
        assertNotNull(stmt);
        ResultSet rs = stmt.executeQuery("SELECT DATABASE ()");
        assertTrue(rs.next());
        assertEquals("TEST", rs.getString(1));
        close(stmt, conn);
    }
    
    @Test
    public void testDataSource_1() throws Exception {
        DataSource ds = EmbeddedHelper.Factory.newDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }));
        Connection conn = ds.getConnection();
        conn.setAutoCommit(false);
        conn.commit();
        close(conn);
    }
    
    @Test
    public void testDataSource_2() throws Exception {
        DataSource ds = EmbeddedHelper.Factory.newDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }));
        Connection c1 = ds.getConnection();
        Connection c2 = ds.getConnection();
        
        update(c1, "CREATE TABLE IF NOT EXISTS FOO(ID integer)");
        boolean autoCommit = c1.getAutoCommit();
        assertTrue(autoCommit);
        c1.setAutoCommit(false);
        insert(c1, "INSERT INTO FOO VALUES (100)");
        query(c2, "SELECT COUNT(ID) FROM FOO"); // should be 0
        c1.commit();
        query(c2, "SELECT COUNT(ID) FROM FOO"); // should be 1
        c1.setAutoCommit(autoCommit);
        
        close(c1);
        close(c2);
    }

}
