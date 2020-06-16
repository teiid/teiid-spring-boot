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
package org.teiid.spring.data.mssqlserver;

import org.teiid.spring.data.ConnectionFactoryConfiguration;

@ConnectionFactoryConfiguration(
        alias = "mssql-server",
        translatorName = "sqlserver",
        driverNames={"com.microsoft.sqlserver.jdbc.SQLServerDriver"},
        datasourceNames={"com.microsoft.sqlserver.jdbc.SQLServerXADataSource"},
        url="jdbc:microsoft:sqlserver://{host}:1433",
        dialect="org.hibernate.dialect.SQLServer2012Dialect",
        otherAliases= {"ms-sqlserver","sqlserver"},
        jdbc=true
        )
public class MssqlServerDataSourceConfiguration {

}
