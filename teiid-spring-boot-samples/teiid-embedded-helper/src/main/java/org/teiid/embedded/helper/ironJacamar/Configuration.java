/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
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
