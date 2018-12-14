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
package org.teiid.spring.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    /*private final class SingleConnectionDataSource implements DataSource {
      DataSource ds;
      Connection proxy;
    public SingleConnectionDataSource(DataSource bean, Connection c) {
      this.ds = bean;
      proxy = (Connection) Proxy.newProxyInstance(ds.getClass().getClassLoader(), new Class<?>[] {Connection.class}, new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          if (method.getName().equals("close")) {
            return null;
          }
          try {
            return method.invoke(c, args);
          } catch (InvocationTargetException e) {
            throw e.getTargetException();
          }
        }
      });
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return ds.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return ds.isWrapperFor(iface);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
      ds.setLoginTimeout(seconds);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
      ds.setLogWriter(out);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return ds.getParentLogger();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
      return ds.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      return ds.getLogWriter();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      throw new AssertionError();
    }

    @Override
    public Connection getConnection() throws SQLException {
      return proxy;
    }
  }*/

  @Autowired
    ApplicationContext context;

  @Qualifier(value="dataSource")
  @Autowired
  DataSource dataSource;

    static AtomicInteger idGenerator = new AtomicInteger(1000);

    @RequestMapping("/test")
    @Transactional
    public String index() {
        /*ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
          Condition c = writeLock.newCondition();
          c.await(10, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
        } finally {
          writeLock.unlock();
        }*/

      /*try {
      Thread.sleep(1);
    } catch (InterruptedException e1) {
    }*/

      //Connection c = DataSourceUtils.getConnection(dataSource);

    //try {
      //DataSource ds = new SingleConnectionDataSource(dataSource, c);
        JdbcTemplate template = new JdbcTemplate(dataSource);
          int id = idGenerator.getAndIncrement();

          template.update("INSERT INTO customer(id, ssn, name) VALUES (?, ?,?)", new Object[] {id, "1234", "ramesh"});

          template.query("select id, name from customer where id = ?", new Object[] {id}, new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                  //System.out.println(rs.getInt(1) + ":" + rs.getString(2));
              }
          });

          template.update("UPDATE CUSTOMER SET name = ? WHERE id = ?", new Object[] {"foo", id});

          return "Greetings from Spring Boot!";
    //} finally {
    //  DataSourceUtils.releaseConnection(c, dataSource);
    //}
    }

    @RequestMapping("/count")
    public String count() {
        StringBuilder sb = new StringBuilder();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.query("select count(*) from customer", new Object[] {}, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                //System.out.println(rs.getInt(1) + ":" + rs.getString(2));
                sb.append(rs.getObject(1));
            }
        });
        return sb.toString();
    }
}
