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

package org.teiid.spring.data;



import java.io.Closeable;
import java.io.IOException;

import org.teiid.resource.api.Connection;
import org.teiid.resource.api.ConnectionFactory;

public abstract class BaseConnectionFactory<T extends Connection> implements ConnectionFactory<T>, Closeable {

    public String getTranslatorName() {
        ConnectionFactoryConfiguration cfc = this.getClass().getAnnotation(ConnectionFactoryConfiguration.class);
        if (cfc != null) {
            return cfc.translatorName();
        }
        throw new IllegalStateException("@ConnectionFactoryAnnotation is not defined on class "
                + this.getClass().getName());
    }

    public String getConfigurationPrefix() {
        ConnectionFactoryConfiguration cfc = this.getClass().getAnnotation(ConnectionFactoryConfiguration.class);
        if (cfc != null) {
            return cfc.propertyPrefix();
        }
        throw new IllegalStateException("@ConnectionFactoryAnnotation is not defined on class "
                + this.getClass().getName());
    }

    public String getAlias() {
        ConnectionFactoryConfiguration cfc = this.getClass().getAnnotation(ConnectionFactoryConfiguration.class);
        if (cfc != null) {
            return cfc.alias();
        }
        throw new IllegalStateException("@ConnectionFactoryAnnotation is not defined on class "
                + this.getClass().getName());
    }

    @Override
    public void close() throws IOException {
    }
}
