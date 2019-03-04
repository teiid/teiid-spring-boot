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

import javax.transaction.TransactionManager;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.teiid.resource.api.Connection;
import org.teiid.resource.api.ConnectionFactory;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.ExecutionFactory;

@Configuration
public class TestConfiguration {
    @Bean
    public TransactionManager transactionManager() {
        return Mockito.mock(TransactionManager.class);
    }

    @SuppressWarnings("rawtypes")
    @Bean("fakeSource")
    public ConnectionFactory fakeSource() {
        return new BaseConnectionFactory() {
            @Override
            public Connection getConnection() throws Exception {
                return new Connection() {
                    @Override
                    public void close() throws Exception {
                    }
                };
            }
        };
    }

    @SuppressWarnings("rawtypes")
    @Bean("fakeSource3")
    public ConnectionFactory fakeSource3() {
        return new BaseConnectionFactory() {
            @Override
            public Connection getConnection() throws Exception {
                return new Connection() {
                    @Override
                    public void close() throws Exception {
                    }
                };
            }
        };
    }

    @SuppressWarnings("rawtypes")
    @Bean("fakeSource2")
    @DependsOn({"fake2"})
    public ConnectionFactory fakeSource2() {
        return new BaseConnectionFactory() {
            @Override
            public Connection getConnection() throws Exception {
                return new Connection() {
                    @Override
                    public void close() throws Exception {
                    }
                };
            }
        };
    }

    @Bean("fake2")
    public ExecutionFactory<?, ?> fake2(){
        return new FakeTranslator();
    }
}
