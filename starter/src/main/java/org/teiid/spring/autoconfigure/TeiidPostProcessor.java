/*
 * Copyright 2012-2014 the original author or authors.
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

package org.teiid.spring.autoconfigure;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.translator.ExecutionFactory;

/**
 * {@link BeanPostProcessor} used to fire {@link TeiidInitializedEvent}s. Should
 * only be registered via the inner {@link Registrar} class.
 *
 * Code as template taken from {@link DataSourceInitializerPostProcessor}
 */
class TeiidPostProcessor implements BeanPostProcessor, Ordered, ApplicationListener<ContextRefreshedEvent> {
    private static final Log logger = LogFactory.getLog(TeiidPostProcessor.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private XADataSourceWrapper xaWrapper;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            this.beanFactory.getBean("teiid", TeiidServer.class);
        }
        return bean;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TeiidServer) {
            // force initialization of this bean as soon as we see a TeiidServer
            this.beanFactory.getBean(TeiidInitializer.class);
        } else if ((bean instanceof DataSource || bean instanceof XADataSource) && !beanName.equals("dataSource")) {
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
            VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);

            DataSource ds = null;
            if (bean instanceof XADataSource) {
                try {
                    if (this.xaWrapper == null) {
                        throw new IllegalStateException("XA data source is configured, however no JTA "
                                + "transaction manager is defined in the pom.xml as dependency to this project");
                    }
                    ds = xaWrapper.wrapDataSource((XADataSource) bean);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                // only add the data source to transaction adapter when source is non-xa, in xa
                // case the sources
                // should auto enlist themselves.
                ds = (DataSource) bean;
                server.getPlatformTransactionManagerAdapter().addDataSource(ds);
            }

            // initialize databases if any
            new MultiDataSourceInitializer(ds, beanName, context).init();
            server.addDataSource(vdb, beanName, ds, context);
            logger.info("Datasource added to Teiid = " + beanName);
        } else if (bean instanceof BaseConnectionFactory) {
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
            VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
            server.addDataSource(vdb, beanName, bean, context);
            logger.info("Non JDBC Datasource added to Teiid = " + beanName);
            server.getPlatformTransactionManagerAdapter().addDataSource((BaseConnectionFactory) bean);
        } else if (bean instanceof PlatformTransactionManager) {
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
            server.getPlatformTransactionManagerAdapter()
            .setPlatformTransactionManager((PlatformTransactionManager) bean);
        } else if (bean instanceof ExecutionFactory) {
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
            server.addTranslator(beanName, (ExecutionFactory)bean);
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        boolean deploy = true;
        VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
        TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
        if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {
            PhysicalNamingStrategy namingStrategy = this.beanFactory.getBean(PhysicalNamingStrategy.class);
            deploy = server.findAndConfigureViews(vdb, event.getApplicationContext(), namingStrategy);
        }

        if (deploy) {
            // Deploy at the end when all the data sources are configured
            server.undeployVDB(vdb.getName(), vdb.getVersion());
            server.deployVDB(vdb, true, this.context);
        }
    }

    /**
     * {@link ImportBeanDefinitionRegistrar} to register the
     * {@link TeiidPostProcessor} without causing early bean instantiation issues.
     */
    static class Registrar implements ImportBeanDefinitionRegistrar {

        private static final String BEAN_NAME = "teiidInitializerPostProcessor";

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition(BEAN_NAME)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(TeiidPostProcessor.class);
                beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
                // We don't need this one to be post processed otherwise it can cause a
                // cascade of bean instantiation that we would rather avoid.
                beanDefinition.setSynthetic(true);
                registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
            }
        }
    }
}

