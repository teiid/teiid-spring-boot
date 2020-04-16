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

package org.teiid.spring.configuration;

import java.io.IOException;

import org.teiid.spring.data.BaseConnection;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;

@ConnectionFactoryConfiguration(alias="fake", translatorName="fake")
public class FakeConnectionFactory implements BaseConnectionFactory<BaseConnection>{

    @Override
    public BaseConnection getConnection() throws Exception {
        return new BaseConnection() {
            @Override
            public void close() throws Exception {
            }
        };
    }

    @Override
    public void close() throws IOException {
    }
}
