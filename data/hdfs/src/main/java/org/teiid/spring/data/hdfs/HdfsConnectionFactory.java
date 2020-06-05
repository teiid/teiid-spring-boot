/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.data.hdfs;

import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

import java.io.IOException;

@ConnectionFactoryConfiguration(
        alias = "hdfs",
        translatorName = "hdfs"
)
public class HdfsConnectionFactory implements BaseConnectionFactory<HdfsConnection> {

    HdfsConfiguration hdfsConfiguration;

    public HdfsConnectionFactory(HdfsConfiguration hdfsConfiguration) {
        this.hdfsConfiguration = hdfsConfiguration;
    }

    @Override
    public HdfsConnection getConnection() throws Exception {
        return new HdfsConnection(hdfsConfiguration);
    }

    @Override
    public void close() throws IOException {

    }
}
