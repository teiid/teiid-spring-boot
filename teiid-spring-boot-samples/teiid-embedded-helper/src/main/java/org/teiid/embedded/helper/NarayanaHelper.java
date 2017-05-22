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

package org.teiid.embedded.helper;

import java.util.function.Consumer;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.teiid.embedded.helper.narayana.Configuration;
import org.teiid.embedded.helper.narayana.NarayanaHelperImpl;

public interface NarayanaHelper {
    
    TransactionManager transactionManager();
    
    TransactionManager transactionManager(Consumer<Configuration> consumer);
    
    /**
     * Used to get a TransactionSynchronizationRegistry, {@link #transactionManager()} or {@link #transactionManager(Consumer)}
     * should be invoke before use this method
     * @return
     */
    TransactionSynchronizationRegistry transactionSynchronizationRegistry();
    
    NarayanaHelper Factory = new NarayanaHelperImpl();

}
