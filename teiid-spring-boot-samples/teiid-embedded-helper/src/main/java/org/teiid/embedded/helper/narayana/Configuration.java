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

import java.util.Objects;
import java.util.function.Consumer;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

public class Configuration {
    
    private CoreEnvironmentBean coreEnvironmentBean;
    
    private CoordinatorEnvironmentBean coordinatorEnvironmentBean;
    
    private ObjectStoreEnvironmentBean objectStoreEnvironmentBean;
    
    public Configuration coreEnvironmentBean(Consumer<CoreEnvironmentBean> consumer) {
        Objects.requireNonNull(consumer);
        this.coreEnvironmentBean = arjPropertyManager.getCoreEnvironmentBean();
        consumer.accept(coreEnvironmentBean);
        return this;
    }
    
    public CoreEnvironmentBean coreEnvironmentBean() {
        return this.coreEnvironmentBean;
    }
    
    public Configuration coordinatorEnvironmentBean(Consumer<CoordinatorEnvironmentBean> consumer) {
        Objects.requireNonNull(consumer);
        this.coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
        consumer.accept(coordinatorEnvironmentBean);
        return this;
    }
    
    public CoordinatorEnvironmentBean coordinatorEnvironmentBean() {
        return this.coordinatorEnvironmentBean;
    }
    
    public Configuration objectStoreEnvironmentBean(Consumer<ObjectStoreEnvironmentBean> consumer) {
        Objects.requireNonNull(consumer);
        this.objectStoreEnvironmentBean = arjPropertyManager.getObjectStoreEnvironmentBean();
        consumer.accept(objectStoreEnvironmentBean);
        return this;
    }
    
    public ObjectStoreEnvironmentBean objectStoreEnvironmentBean() {
        return this.objectStoreEnvironmentBean;
    }

}
