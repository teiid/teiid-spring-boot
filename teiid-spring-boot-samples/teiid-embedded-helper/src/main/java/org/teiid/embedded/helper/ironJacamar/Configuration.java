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

package org.teiid.embedded.helper.ironJacamar;

import java.util.Objects;
import java.util.function.Consumer;

import org.jboss.jca.adapters.jdbc.local.LocalManagedConnectionFactory;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;

/**
 * A wrapper class for ironJacamar {@link org.jboss.jca.adapters.jdbc.local.LocalManagedConnectionFactory LocalManagedConnectionFactory} and {@link org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration PoolConfiguration}
 * 
 * @author Kylin Soong
 */
public class Configuration {
    
    private LocalManagedConnectionFactory localManagedConnectionFactory;
    
    private PoolConfiguration poolConfiguration;

    public Configuration poolConfiguration(Consumer<PoolConfiguration> consumer) {
        Objects.requireNonNull(consumer);
        this.poolConfiguration = new PoolConfiguration();
        consumer.accept(poolConfiguration);
        return this;
    }

    public PoolConfiguration poolConfiguration() {
        return poolConfiguration;
    }
    
    public Configuration localManagedConnectionFactory(Consumer<LocalManagedConnectionFactory> consumer) {
        Objects.requireNonNull(consumer);
        this.localManagedConnectionFactory = new LocalManagedConnectionFactory();
        consumer.accept(localManagedConnectionFactory);
        return this;
    }
    
    public LocalManagedConnectionFactory localManagedConnectionFactory() {
        return localManagedConnectionFactory;
    }

}
