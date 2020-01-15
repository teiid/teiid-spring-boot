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
package org.teiid.spring.data.infinispan;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.transaction.TransactionManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.AuthenticationConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.teiid.infinispan.api.ProtobufResource;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.TranslatorException;

public class InfinispanConnectionFactory extends BaseConnectionFactory<InfinispanConnectionImpl> {

    private RemoteCacheManager cacheManager;
    private RemoteCacheManager scriptCacheManager;
    private SerializationContext ctx;
    private InfinispanConfiguration config;

    @Autowired(required = false)
    private TransactionManager transactionManager;

    public InfinispanConnectionFactory(InfinispanConfiguration config) {
        super("infinispan-hotrod", "spring.teiid.data.infinispan");
        this.config = config;
        buildCacheManager();
        buildScriptCacheManager();
    }

    public InfinispanConfiguration getConfig() {
        return config;
    }

    private void buildCacheManager() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServers(config.getUrl());
        builder.marshaller(new ProtoStreamMarshaller());
        if (config.getTransactionMode() != null) {
            builder.transaction()
                .transactionMode(config.getTransactionMode())
                .transactionManagerLookup(() -> {return transactionManager;});
        }

        handleSecurity(builder);

        // note this object is expensive, so there needs to only one
        // instance for the JVM, in this case one per RA instance.
        this.cacheManager = new RemoteCacheManager(builder.build());

        // register default marshellers
        /*
         * SerializationContext ctx =
         * ProtoStreamMarshaller.getSerializationContext(this.cacheManager);
         * FileDescriptorSource fds = new FileDescriptorSource();
         * ctx.registerProtoFiles(fds);
         */
        this.cacheManager.start();
        this.ctx = MarshallerUtil.getSerializationContext(this.cacheManager);
    }

    private void buildScriptCacheManager() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServers(config.getUrl());
        // builder.marshaller(new GenericJBossMarshaller());
        handleSecurity(builder);

        // note this object is expensive, so there needs to only one
        // instance for the JVM, in this case one per RA instance.
        this.scriptCacheManager = new RemoteCacheManager(builder.build());
        this.scriptCacheManager.start();
    }

    public void handleSecurity(ConfigurationBuilder builder) {
        if (config.getSaslMechanism() != null && config.getSaslMechanism().equals("EXTERNAL")) {

            if (config.getKeyStoreFileName() == null || config.getKeyStoreFileName().isEmpty()) {
                throw new RuntimeException(
                        "\"EXTERNAL\" SASL Mechanism enabled, however no Keystore information provided for SSL");
            }

            if (config.getKeyStorePassword() == null) {
                throw new RuntimeException("No Keystore password defined");
            }

            if (config.getTrustStoreFileName() == null && config.getTrustStoreFileName().isEmpty()) {
                throw new RuntimeException(
                        "\"EXTERNAL\" SASL Mechanism enabled, however no Truststore information provided for SSL");
            }

            if (config.getTrustStorePassword() == null) {
                throw new RuntimeException("No Truststore password defined");
            }

            CallbackHandler callback = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                }
            };
            builder.security().authentication().enable().saslMechanism("EXTERNAL").callbackHandler(callback).ssl()
            .enable().keyStoreFileName(config.getKeyStoreFileName())
            .keyStorePassword(config.getKeyStorePassword().toCharArray())
            .trustStoreFileName(config.getTrustStoreFileName())
            .trustStorePassword(config.getTrustStorePassword().toCharArray());
        } else if (config.getSaslMechanism() != null || config.getUserName() != null){
            if (config.getUserName() == null) {
                throw new RuntimeException("No User name supplied");
            }
            if (config.getPassword() == null) {
                throw new RuntimeException("No password supplied");
            }
            if (config.getAuthenticationRealm() == null) {
                throw new RuntimeException("No Authentication Realm supplied");
            }
            if (config.getAuthenticationServerName() == null) {
                throw new RuntimeException("No Authentication server information provided.");
            }
            AuthenticationConfigurationBuilder authBuilder = builder.security().authentication().enable()
            .username(config.getUserName()).realm(config.getAuthenticationRealm())
            .password(config.getPassword()).serverName(config.getAuthenticationServerName());
            if (config.getSaslMechanism() != null) {
                authBuilder.saslMechanism(config.getSaslMechanism());
            }
        }
    }

    public void registerProtobufFile(ProtobufResource protobuf) throws TranslatorException {
        try {
            if (protobuf != null) {
                // client side
                this.ctx.registerProtoFiles(
                        FileDescriptorSource.fromString(protobuf.getIdentifier(), protobuf.getContents()));

                // server side
                RemoteCache<String, String> metadataCache = this.cacheManager
                        .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                if (metadataCache != null) {
                    metadataCache.put(protobuf.getIdentifier(), protobuf.getContents());
                    String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
                    // ispn removes leading '/' in a string in the results
                    String protoSchemaIdent = (protobuf.getIdentifier().startsWith("/"))
                            ? protobuf.getIdentifier().substring(1) : protobuf.getIdentifier();
                            if (errors != null && isProtoSchemaInErrors(protoSchemaIdent, errors)) {
                                throw new TranslatorException("Error occurred during the registration of the protobuf resource: {0}");

                            }
                }
            } else {
                throw new TranslatorException("No protobuf supplied to register");
            }
        } catch (Throwable t) {
            throw new TranslatorException(t);
        }
    }

    private boolean isProtoSchemaInErrors(String ident, String errors) {
        for (String s : errors.split("\n")) {
            if (s.trim().startsWith(ident)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InfinispanConnectionImpl getConnection() throws Exception {
        return new InfinispanConnectionImpl(this.cacheManager, this.scriptCacheManager, config.getCacheName(), this.ctx,
                this, config.getCacheTemplate());
    }
}
