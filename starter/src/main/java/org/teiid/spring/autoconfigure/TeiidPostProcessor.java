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

import static org.teiid.spring.autoconfigure.TeiidConstants.VDBNAME;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBVERSION;

import java.util.Set;

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
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.metadata.MetadataFactory;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.spring.annotations.JsonTable;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.TextTable;
import org.teiid.spring.connections.BaseConnectionFactory;

/**
 * {@link BeanPostProcessor} used to fire {@link TeiidInitializedEvent}s. Should only
 * be registered via the inner {@link Registrar} class.
 *
 * Code as template taken from {@link DataSourceInitializerPostProcessor}
 */
class TeiidPostProcessor implements BeanPostProcessor, Ordered, ApplicationListener<ContextRefreshedEvent>{
    private static final Log logger = LogFactory.getLog(TeiidPostProcessor.class);
    
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	@Autowired
	private BeanFactory beanFactory;
	
	@Autowired
	private ApplicationContext context;
	
    @Autowired
    private TeiidProperties properties;	

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
		    // initialize databases if any
		    new MultiDataSourceInitializer((DataSource)bean, beanName, context).init();
		    
		    TeiidServer server = this.beanFactory.getBean(TeiidServer.class);		    
		    VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
		    server.addDataSource(vdb, beanName, (DataSource)bean, context);		        		
		    logger.info("Datasource added to Teiid = " + beanName);
		} else if (bean instanceof BaseConnectionFactory) {
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);           
            VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
            server.addDataSource(vdb, beanName, (BaseConnectionFactory)bean, context);                     
            logger.info("Non JDBC Datasource added to Teiid = " + beanName);
		}
		return bean;
	}

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        boolean deploy = true;
        VDBMetaData vdb = this.beanFactory.getBean(VDBMetaData.class);
        if (vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true")) {
            deploy = findAndConfigureViews(vdb, event.getApplicationContext());
        }

        if (deploy) {
            // Deploy at the end when all the datasources are configured
            TeiidServer server = this.beanFactory.getBean(TeiidServer.class);
            server.undeployVDB(VDBNAME, VDBVERSION);
            server.deployVDB(vdb);
        }
    }	

    private boolean findAndConfigureViews(VDBMetaData vdb, ApplicationContext context) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
                false);
        provider.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Entity.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(SelectQuery.class));
        
        String basePackage = context.getEnvironment().getProperty("spring.teiid.model.package");
        if(basePackage == null) {
            return false;
        }
        Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
        if (components.isEmpty()) {
            return false;
        }
        
        ModelMetaData model = new ModelMetaData();
        model.setName("teiid");
        model.setModelType(Model.Type.VIRTUAL);
        MetadataFactory mf = new MetadataFactory(VDBNAME, VDBVERSION, SystemMetadata.getInstance().getRuntimeTypeMap(),
                model);
        for (BeanDefinition c : components) {
            try {
                Class<?> clazz = Class.forName(c.getBeanClassName());                
                SelectQuery selectAnnotation = clazz.getAnnotation(SelectQuery.class);
                TextTable textTableAnnotation = clazz.getAnnotation(TextTable.class);
                JsonTable jsonTableAnnotation = clazz.getAnnotation(JsonTable.class);

                if (textTableAnnotation != null) {
                    new TextTableView().buildView(clazz, mf, textTableAnnotation);
                } else if (jsonTableAnnotation != null) {
                    new JsonTableView().buildView(clazz, mf, jsonTableAnnotation);
                } else if (selectAnnotation != null) {
                    new SimpleView().buildView(clazz, mf, selectAnnotation);
                }
                
            } catch (ClassNotFoundException e) {
                logger.warn("Error loading entity classes");
            }
        }

        if (mf.getSchema().getTables().isEmpty()) {
            return false;
        }
        
        String ddl = DDLStringVisitor.getDDLString(mf.getSchema(), null, null);
        model.addSourceMetadata("DDL", ddl);        
        vdb.addModel(model);
        logger.debug("Generated Teiid DDL:"+ddl);
        
        return true;
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
