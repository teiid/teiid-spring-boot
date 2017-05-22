package org.teiid.embedded.helper;

import static org.junit.Assert.*;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.junit.Test;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

public class TestNarayanaHelper {
    
    @Test
    public void testProperties() {
        
        EmbeddedHelper.Factory.transactionManager(c -> c.coreEnvironmentBean(core -> {
            try {
                core.setNodeIdentifier("100");
                core.setSocketProcessIdPort(0);
                core.setSocketProcessIdMaxPorts(10);
            } catch (CoreEnvironmentBeanException e) {
                e.printStackTrace();
            }
        }).coordinatorEnvironmentBean(coordinator -> {
            coordinator.setEnableStatistics(false);
            coordinator.setDefaultTimeout(300);
            coordinator.setTransactionStatusManagerEnable(false);
            coordinator.setTxReaperCancelFailWaitPeriod(120000);
        }).objectStoreEnvironmentBean(objectStore -> {
            objectStore.setObjectStoreDir("target/narayana/data");
        }));
        
        assertEquals("100", arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier());
        assertEquals(0, arjPropertyManager.getCoreEnvironmentBean().getSocketProcessIdPort());
        assertEquals(10, arjPropertyManager.getCoreEnvironmentBean().getSocketProcessIdMaxPorts());
        
        assertFalse(arjPropertyManager.getCoordinatorEnvironmentBean().isEnableStatistics());
        assertEquals(300, arjPropertyManager.getCoordinatorEnvironmentBean().getDefaultTimeout());
        assertFalse(arjPropertyManager.getCoordinatorEnvironmentBean().isTransactionStatusManagerEnable());
        assertEquals(120000, arjPropertyManager.getCoordinatorEnvironmentBean().getTxReaperTimeout());

        assertEquals("target/narayana/data", arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir());
    }
    
    @Test
    public void commitTransactionManager() throws Exception {
        TransactionManager tm = EmbeddedHelper.Factory.transactionManager();
        tm.begin();
        tm.commit();
    }
    
    @Test
    public void rollbackTransaction() throws Exception {
        TransactionManager tm = EmbeddedHelper.Factory.transactionManager();
        tm.begin();
        tm.rollback();
    }
    
    @Test(expected = RollbackException.class)
    public void setRollbackOnly() throws Exception {
        TransactionManager tm = EmbeddedHelper.Factory.transactionManager();
        tm.begin();
        tm.setRollbackOnly();
        tm.commit();
    }
    
    @Test
    public void transactionStatus() throws Exception {
        TransactionManager tm = EmbeddedHelper.Factory.transactionManager();
        tm.begin();
        assertEquals(Status.STATUS_ACTIVE, tm.getTransaction().getStatus());
        tm.setRollbackOnly();
        assertEquals(Status.STATUS_MARKED_ROLLBACK, tm.getTransaction().getStatus());
        tm.rollback();
    }

    @Test(expected = RollbackException.class)
    public void transactionTimeout() throws Exception{
        TransactionManager tm = EmbeddedHelper.Factory.transactionManager();
        tm.setTransactionTimeout(3);
        tm.begin();
        Thread.sleep(1000 * 5);
        tm.commit();
    }
    

}
