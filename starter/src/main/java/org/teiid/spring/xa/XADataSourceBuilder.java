/*
 * Copyright 2012-2016 the original author or authors.
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

package org.teiid.spring.xa;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience class for building a {@link XADataSource} with common
 * implementations and properties.
 */
@ConfigurationProperties(prefix = "spring.xa.datasource")
public class XADataSourceBuilder implements BeanClassLoaderAware, InitializingBean, XADataSource {

    private ClassLoader classLoader;

    private Map<String, String> properties = new LinkedHashMap<String, String>();

    private XADataSource delegate;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.delegate = createXaDataSource();
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public XADataSourceBuilder() {
        this(null);
    }

    public XADataSourceBuilder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public XADataSource build() throws Exception {
        return this;
    }

    public static XADataSourceBuilder create() throws Exception {
        return new XADataSourceBuilder();
    }

    protected XADataSource createXaDataSource() {
        String className = dataSourceClassName();
        Assert.state(StringUtils.hasLength(className), "No XA DataSource class name specified");
        XADataSource dataSource = createXaDataSourceInstance(className);
        Bindable<XADataSource> bindable = Bindable.ofInstance(dataSource);
        bindXaProperties(bindable);
        return dataSource;
    }

    private XADataSource createXaDataSourceInstance(String className) {
        try {
            Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
            Object instance = BeanUtils.instantiateClass(dataSourceClass);
            Assert.isInstanceOf(XADataSource.class, instance);
            return (XADataSource) instance;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create XADataSource instance from '" + className + "'");
        }
    }

    private void bindXaProperties(Bindable<XADataSource> target) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
                this.properties);
        ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
        aliases.addAliases("url", "jdbc-url");
        aliases.addAliases("username", "user");
        aliases.addAliases("portNumber", "port");
        aliases.addAliases("serverName", "server");
        aliases.addAliases("databaseName", "database");

        Binder binder = new Binder(source.withAliases(aliases));
        binder.bind(ConfigurationPropertyName.EMPTY, target);
    }

    public XADataSourceBuilder driverClassName(String driverClassName) {
        this.properties.put("driverClassName", driverClassName);
        return this;
    }

    public XADataSourceBuilder username(String username) {
        this.properties.put("username", username);
        return this;
    }

    public XADataSourceBuilder password(String password) {
        this.properties.put("password", password);
        return this;
    }

    public XADataSourceBuilder port(String port) {
        this.properties.put("port", port);
        return this;
    }

    public XADataSourceBuilder server(String server) {
        this.properties.put("server", server);
        return this;
    }

    public XADataSourceBuilder database(String database) {
        this.properties.put("database", database);
        return this;
    }

    public XADataSourceBuilder dataSourceClassName(String dataSourceClassName) {
        this.properties.put("dataSourceClassName", dataSourceClassName);
        return this;
    }

    public String dataSourceClassName() {
        String className = this.properties.get("dataSourceClassName");
        if (!StringUtils.hasLength(className)) {
            String url = this.properties.get("url");
            className = DatabaseDriver.fromJdbcUrl(url).getXaDataSourceClassName();
        }
        return className;
    }

    public XADataSourceBuilder url(String url) {
        this.properties.put("url", url);
        return this;
    }

    public XADataSourceBuilder initialize(boolean initialize) {
        this.properties.put("initialize", Boolean.toString(initialize));
        return this;
    }

    public XADataSourceBuilder platform(String platform) {
        this.properties.put("platform", platform);
        return this;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.delegate.setLoginTimeout(seconds);
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return this.delegate.getXAConnection();
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return this.delegate.getXAConnection(user, password);
    }
}
