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
