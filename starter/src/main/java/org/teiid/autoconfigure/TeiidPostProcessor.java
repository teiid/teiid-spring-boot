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

package org.teiid.autoconfigure;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.teiid.adminapi.impl.VDBMetaData;

/**
 * {@link BeanPostProcessor} used to fire {@link TeiidInitializedEvent}s. Should only
 * be registered via the inner {@link Registrar} class.
 *
 * Code as template taken from {@link DataSourceInitializerPostProcessor}
 */
class TeiidPostProcessor implements BeanPostProcessor, Ordered {
    private static final Log logger = LogFactory.getLog(TeiidPostProcessor.class);
    
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	@Autowired
	private BeanFactory beanFactory;
	
	@Autowired
	private ApplicationContext context;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
	    if (bean instanceof DataSource) {
	        this.beanFactory.getBean("teiid", TeiidServer.class);
	    }
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof TeiidServer) {
			// force initialization of this bean as soon as we see a TeiidServer
			this.beanFactory.getBean(TeiidInitializer.class);
		} else if (bean instanceof DataSource && !beanName.equals("teiidDataSource")) {
		    TeiidServer server = this.beanFactory.getBean(TeiidServer.class);		    
		    VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
		    server.addDataSource(vdb, beanName, (DataSource)bean, context);		        		
		    logger.info("Datasource added to Teiid = " + beanName);
		}
		return bean;
	}
	    
    /**
	 * {@link ImportBeanDefinitionRegistrar} to register the
	 * {@link TeiidPostProcessor} without causing early bean instantiation
	 * issues.
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
