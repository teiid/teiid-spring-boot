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

package org.teiid.embedded.helper.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

/**
 * A Utils class which build on top of Commons DbUtils to make working with JDBC easier.
 * 
 * @author Kylin Soong
 */
public class JDBCUtils {
	
	public static Connection getDriverConnection(String driver, String url, String user, String pass) throws SQLException {
	    DbUtils.loadDriver(driver);
		return DriverManager.getConnection(url, user, pass); 
	}

	public static void close(Connection conn) throws SQLException {
	    DbUtils.close(conn);
	}
	
	public static void close(Statement stmt) throws SQLException {
	    DbUtils.close(stmt);
	}
	
	public static void close(ResultSet rs) throws SQLException {
	    DbUtils.close(rs);
    }
	
	public static void close(Statement stmt, Connection conn) throws SQLException {
	    DbUtils.close(stmt);
	    DbUtils.close(conn);
    }
	
	public static void close(ResultSet rs, Statement stmt) throws SQLException {
	    DbUtils.close(rs);
	    DbUtils.close(stmt);
	}
	
	public static void close(ResultSet rs, Statement stmt, Connection conn) throws SQLException {
	    DbUtils.close(rs);
        DbUtils.close(stmt);
        DbUtils.close(conn);
	}

	public static void query(Connection conn, String sql) throws SQLException {
		
		System.out.println("Query SQL: " + sql);  
		QueryRunner runner = new QueryRunner();
		ArrayListHandler handler = new ArrayListHandler();
		List<Object[]> results = runner.query(conn, sql, handler);
		dumpResults(results);
	}
	
	public static void insert(Connection conn, String sql) throws SQLException {
        
        System.out.println("Insert SQL: " + sql);
        QueryRunner runner = new QueryRunner();
        ArrayListHandler handler = new ArrayListHandler();
        List<Object[]> results = runner.insert(conn, sql, handler);
        dumpResults(results);
    }
	
	public static void update(Connection conn, String sql) throws SQLException {
        
        System.out.println("Update SQL: " + sql);
        QueryRunner runner = new QueryRunner();
        int rows = runner.update(conn, sql);
        System.out.println(rows + " of rows updated");
    }
	
	public static void delete(Connection conn, String sql) throws SQLException {
        
        System.out.println("Delete SQL: " + sql);
        QueryRunner runner = new QueryRunner();
        int rows = runner.update(conn, sql);
        System.out.println(rows + " of rows updated");
    }

	private static void dumpResults(List<Object[]> results) {

	    results.forEach(a -> {
            StringBuilder sb = new StringBuilder();
            Arrays.asList(a).forEach(i -> {
                sb.append(i + ", ");
            });
            System.out.println(sb.substring(0, sb.length() - 2));
        });
    }

}
