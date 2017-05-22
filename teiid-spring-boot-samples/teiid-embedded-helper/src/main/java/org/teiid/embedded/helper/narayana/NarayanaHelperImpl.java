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

package org.teiid.embedded.helper.narayana;

import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.teiid.embedded.helper.NarayanaHelper;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

public class NarayanaHelperImpl implements NarayanaHelper {
    
    @Override
    public TransactionManager transactionManager() {
        return transactionManager(c -> c.coreEnvironmentBean(core -> {
            try {
                core.setNodeIdentifier(UUID.randomUUID().toString());
            } catch (CoreEnvironmentBeanException e) {
                throw new IllegalBeanPropertiesException(e);
            }
            core.setSocketProcessIdPort(0);
            core.setSocketProcessIdMaxPorts(10);
        }).coordinatorEnvironmentBean(coordinator -> {
            coordinator.setEnableStatistics(false);
            coordinator.setDefaultTimeout(300);
            coordinator.setTransactionStatusManagerEnable(false);
            coordinator.setTxReaperCancelFailWaitPeriod(120000);
        }).objectStoreEnvironmentBean(objectStore -> {
            objectStore.setObjectStoreDir(System.getProperty("java.io.tmpdir")  + File.separator + "narayana");
        }));
    }

    @Override
    public TransactionManager transactionManager(Consumer<Configuration> consumer) {
        
        Objects.requireNonNull(consumer);
        Configuration config = new Configuration();
        consumer.accept(config);
        
        if(arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier() == null) {
            try {
                arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(UUID.randomUUID().toString());
            } catch (CoreEnvironmentBeanException e) {
                throw new IllegalBeanPropertiesException(e);
            }
        }
        
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }
    
    static class IllegalBeanPropertiesException extends RuntimeException {

        private static final long serialVersionUID = -6500608980276363768L;
        
        public IllegalBeanPropertiesException(Exception e) {
            super(e);
        }
    }

    @Override
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
        return new TransactionSynchronizationRegistryImple();
    }

    

}
