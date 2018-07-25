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

package org.teiid.spring.autoconfigure.xa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience class for building a {@link XADataSource} with common implementations and
 * properties. 
 */
@ConfigurationProperties(prefix = "spring.xa.datasource")
public class XADataSourceBuilder implements BeanClassLoaderAware, EnvironmentAware, InitializingBean {

	private ClassLoader classLoader;
	
	private Environment environment;
	
    /**
     * XA datasource fully qualified name.
     */
    private String dataSourceClassName;

    /**
     * Properties to pass to the XA data source.
     */
    private Map<String, String> properties = new LinkedHashMap<String, String>();
    
    /**
     * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
     */
    private String driverClassName;

    /**
     * JDBC url of the database.
     */
    private String url;

    /**
     * Login user of the database.
     */
    private String username;

    /**
     * Login password of the database.
     */
    private String password;
    
    /**
     * Populate the database using 'data.sql'.
     */
    private boolean initialize = true;

    /**
     * Platform to use in the DDL or DML scripts (e.g. schema-${platform}.sql or
     * data-${platform}.sql).
     */
    private String platform = "all";

    /**
     * Schema (DDL) script resource references.
     */
    private List<String> schema;

    /**
     * User of the database to execute DDL scripts (if different).
     */
    private String schemaUsername;

    /**
     * Password of the database to execute DDL scripts (if different).
     */
    private String schemaPassword;

    /**
     * Data (DML) script resource references.
     */
    private List<String> data;

    /**
     * User of the database to execute DML scripts.
     */
    private String dataUsername;

    /**
     * Password of the database to execute DML scripts.
     */
    private String dataPassword;

    /**
     * Do not stop if an error occurs while initializing the database.
     */
    private boolean continueOnError = false;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void setEnvironment(Environment env) {
        this.environment = env;
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

    public DataSource build(XADataSourceWrapper wrapper, XADataSource xaDataSource) throws Exception {
        return wrapper.wrapDataSource(xaDataSource);
    }
    
    public XADataSource buildXA() throws Exception {
        return createXaDataSource();
    }    

    private XADataSource createXaDataSource() {
        String className = getDataSourceClassName();
        if (!StringUtils.hasLength(className)) {
            className = DatabaseDriver.fromJdbcUrl(getUrl())
                    .getXaDataSourceClassName();
        }
        Assert.state(StringUtils.hasLength(className),
                "No XA DataSource class name specified");
        XADataSource dataSource = createXaDataSourceInstance(className);
        bindXaProperties(dataSource);
        return dataSource;
    }

    private XADataSource createXaDataSourceInstance(String className) {
        try {
            Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
            Object instance = BeanUtils.instantiate(dataSourceClass);
            Assert.isInstanceOf(XADataSource.class, instance);
            return (XADataSource) instance;
        }
        catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create XADataSource instance from '" + className + "'");
        }
    }

    private void bindXaProperties(XADataSource target) {
        MutablePropertyValues values = new MutablePropertyValues();
        values.add("user",getUsername());
        values.add("password", getPassword());
        values.add("url", getUrl());
        values.addPropertyValues(getProperties());
        new RelaxedDataBinder(target)
            .withAlias("user", "username")
            .withAlias("port", "portNumber")
            .withAlias("server", "serverName")
            .withAlias("database", "databaseName")
            .bind(values);
    }	
    
    /**
     * Return the configured driver or {@code null} if none was configured.
     * @return the configured driver
     * @see #determineDriverClassName()
     */
    public String getDriverClassName() {
        return this.driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }    
    
    public String getDataSourceClassName() {
        return this.dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
        this.dataSourceClassName = dataSourceClassName;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }    
    
    /**
     * Return the configured username or {@code null} if none was configured.
     * @return the configured username
     * @see #determineUsername()
     */
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Return the configured password or {@code null} if none was configured.
     * @return the configured password
     * @see #determinePassword()
     */
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Return the configured url or {@code null} if none was configured.
     * @return the configured url
     * @see #determineUrl()
     */
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public boolean isInitialize() {
        return this.initialize;
    }

    public void setInitialize(boolean initialize) {
        this.initialize = initialize;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<String> getSchema() {
        return this.schema;
    }

    public void setSchema(List<String> schema) {
        this.schema = schema;
    }

    public String getSchemaUsername() {
        return this.schemaUsername;
    }

    public void setSchemaUsername(String schemaUsername) {
        this.schemaUsername = schemaUsername;
    }

    public String getSchemaPassword() {
        return this.schemaPassword;
    }

    public void setSchemaPassword(String schemaPassword) {
        this.schemaPassword = schemaPassword;
    }

    public List<String> getData() {
        return this.data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public String getDataUsername() {
        return this.dataUsername;
    }

    public void setDataUsername(String dataUsername) {
        this.dataUsername = dataUsername;
    }

    public String getDataPassword() {
        return this.dataPassword;
    }

    public void setDataPassword(String dataPassword) {
        this.dataPassword = dataPassword;
    }

    public boolean isContinueOnError() {
        return this.continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
    
}
