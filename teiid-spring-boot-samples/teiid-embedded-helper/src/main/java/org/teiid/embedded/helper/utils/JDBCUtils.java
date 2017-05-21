/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
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
