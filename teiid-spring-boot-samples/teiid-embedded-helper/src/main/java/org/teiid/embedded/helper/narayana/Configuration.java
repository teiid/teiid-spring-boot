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
