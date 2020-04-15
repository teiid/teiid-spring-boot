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
package org.teiid.spring.data.ftp;

import java.io.IOException;

import org.teiid.file.ftp.FtpFileConnection;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

@ConnectionFactoryConfiguration(
        alias = "ftp",
        translatorName = "ftp"
        )
public class FtpConnectionFactory implements BaseConnectionFactory<FtpConnection> {
    private FtpConfiguration config;

    public FtpConnectionFactory(FtpConfiguration config) {
        this.config = config;
    }

    @Override
    public FtpConnection getConnection() throws Exception {
        return new FtpConnection(new FtpFileConnection(this.config));
    }

    @Override
    public void close() throws IOException {
        // close connections
    }
}
