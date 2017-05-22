package org.teiid.embedded.helper.utils;

import static org.teiid.embedded.helper.utils.JDBCUtils.*;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.resource.ResourceException;

import org.h2.tools.RunScript;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJDBCUtils {
    
    private static Connection conn;
    
    @BeforeClass
    public static void init() throws ResourceException, SQLException {
        conn = getDriverConnection("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "sa");
        RunScript.execute(conn, new InputStreamReader(TestJDBCUtils.class.getClassLoader().getResourceAsStream("sql/customer-schema.sql")));
    }
    
    @AfterClass
    public static void destory() throws SQLException {
        close(conn);
    }
    
    @Test
    public void testQuery() throws SQLException {
        query(conn, "SELECT * FROM CUSTOMER WHERE SSN = 'CST01036'");
    }
    
    @Test
    public void testAll() throws SQLException {
        insert(conn, "INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1001,'BAT','The BAT Company')");
        update(conn, "UPDATE PRODUCT SET SYMBOL = 'BATL' WHERE ID = 1001");
        query(conn, "SELECT * FROM PRODUCT WHERE ID = 1001");
        delete(conn, "DELETE FROM PRODUCT WHERE ID = 1001");
    }

}
