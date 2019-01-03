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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCUtils {

  public static Connection getDriverConnection(String driver, String url, String user, String pass) throws Exception {
    Class.forName(driver);
    return DriverManager.getConnection(url, user, pass);
  }

  public static void close(Connection conn) throws SQLException {
      close(null, null, conn);
  }

  public static void close(Statement stmt) throws SQLException {
      close(null, stmt, null);
  }

  public static void close(ResultSet rs) throws SQLException {
      close(rs, null, null);
    }

  public static void close(Statement stmt, Connection conn) throws SQLException {
      close(null, stmt, conn);
    }

  public static void close(ResultSet rs, Statement stmt) throws SQLException {
      close(rs, stmt, null);
  }

  public static void close(ResultSet rs, Statement stmt, Connection conn) throws SQLException {

      if (null != rs) {
            rs.close();
            rs = null;
        }

        if(null != stmt) {
            stmt.close();
            stmt = null;
        }

        if(null != conn) {
            conn.close();
            conn = null;
        }
  }

  public static void execute(Connection connection, String sql, boolean closeConn) throws Exception {

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            boolean hasResults = stmt.execute(sql);
            if (hasResults) {
                rs = stmt.getResultSet();
                ResultSetMetaData metadata = rs.getMetaData();
                int columns = metadata.getColumnCount();
                for (int row = 1; rs.next(); row++) {
                    System.out.print(row + ": ");
                    for (int i = 0; i < columns; i++) {
                        if (i > 0) {
                            System.out.print(", ");
                        }
                        System.out.print(rs.getObject(i+1));
                    }
                    System.out.println();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt);
            if(closeConn)
                close(connection);
        }
        System.out.println();
    }

  public static void executeQuery(Connection conn, String sql) throws SQLException {

    Statement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      ResultSetMetaData metadata = rs.getMetaData();
            int columns = metadata.getColumnCount();
            for (int row = 1; rs.next(); row++) {
                System.out.print(row + ": ");
                for (int i = 0; i < columns; i++) {
                    if (i > 0) {
                        System.out.print(",");
                    }
                    System.out.print(rs.getObject(i+1));
                }
                System.out.println();
            }
    } finally {
      close(rs, stmt);
    }

    System.out.println();

  }


}
