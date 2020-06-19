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
package org.teiid.spring.common;

import org.teiid.spring.data.ConfigurationProperty;

public class JDBCConfiguration {
    @ConfigurationProperty(displayName = "Connection URL", required = true)
    private String jdbcUrl;
    @ConfigurationProperty(required = true)
    private String username;
    @ConfigurationProperty(masked = true, required = true)
    private String password;
    @ConfigurationProperty(type = "number", defaultValue = "5", advanced = true)
    private int maximumPoolSize;
    @ConfigurationProperty(type = "number", defaultValue = "0", advanced = true)
    private int minimumIdle;

    public String getJdbcUrl() {
        return jdbcUrl;
    }
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }
    public int getMinimumIdle() {
        return minimumIdle;
    }
    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }
}
