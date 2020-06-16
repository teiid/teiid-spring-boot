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
package org.teiid.spring.data.oracle;

import org.teiid.spring.data.ConnectionFactoryConfiguration;

@ConnectionFactoryConfiguration(
        alias = "oracle",
        translatorName = "oracle",
        driverNames={"oracle.jdbc.OracleDriver"},
        datasourceNames={"oracle.jdbc.xa.client.OracleXADataSource"},
        url="jdbc:oracle:thin:{host}:1521:orcl",
        dialect="org.hibernate.dialect.Oracle12cDialect",
        jdbc=true
        )
public class OracleDataSourceConfiguration {

}
