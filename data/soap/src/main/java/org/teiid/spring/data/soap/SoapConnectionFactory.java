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

package org.teiid.spring.data.soap;

import java.io.IOException;

import javax.security.auth.Subject;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.ws.WSConnection;
import org.teiid.ws.cxf.BaseWSConnection;
import org.teiid.ws.cxf.WSConnectionFactory;

@ConfigurationProperties(prefix="spring.teiid.data.soap")
public class SoapConnectionFactory extends BaseConnectionFactory<WSConnection> {

    private WSConnectionFactory wsConnectionFactory;

    public SoapConnectionFactory(SoapConfiguration config) {
        super("soap", "spring.teiid.data.soap");
        try {
            this.wsConnectionFactory = new WSConnectionFactory(config);
        } catch (TranslatorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WSConnection getConnection() throws Exception {
        //none of the security methods are yet supported
        return new BaseWSConnection(wsConnectionFactory) {

            @Override
            protected String getUserName(Subject s, String defaultUserName) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Subject getSubject() {
                //returning null allows basic auth to work
                return null;
            }

            @Override
            protected <T> T getSecurityCredential(Subject s, Class<T> clazz) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected String getPassword(Subject s, String userName, String defaultPassword) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.wsConnectionFactory.close();
    }
}
