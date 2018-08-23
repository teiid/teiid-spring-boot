package org.teiid.spring.autoconfigure;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Provides a light JTA TransactionManager based upon the {@link PlatformTransactionManager}
 */
public final class PlatformTransactionManagerAdapter implements TransactionManager {
	
	private static DefaultTransactionDefinition DEFAULT_TRANSACTION_DEFINITION = new DefaultTransactionDefinition();
	static {
		DEFAULT_TRANSACTION_DEFINITION.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
	}
	
	private static final class PlatformTransactionAdapter implements Transaction {
		
		private WeakReference<TransactionStatus> transactionStatus;
		
		public PlatformTransactionAdapter(TransactionStatus status) {
			this.transactionStatus = new WeakReference<TransactionStatus>(status);
		}
		
		@Override
		public void registerSynchronization(final Synchronization synch)
				throws IllegalStateException, RollbackException, SystemException {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void beforeCompletion() {
					synch.beforeCompletion();
				}
				@Override
				public void afterCompletion(int status) {
					switch (status) {
					case TransactionSynchronization.STATUS_COMMITTED:
						status = Status.STATUS_COMMITTED;
						break;
					case TransactionSynchronization.STATUS_ROLLED_BACK:
						status = Status.STATUS_ROLLEDBACK;
						break;
					case TransactionSynchronization.STATUS_UNKNOWN:
						status = Status.STATUS_UNKNOWN;
						break;
					}
					synch.afterCompletion(status);
				}
			});
		}

		@Override
		public void setRollbackOnly() throws IllegalStateException, SystemException {
			TransactionStatus status = transactionStatus.get();
			if (status == null) {
				throw new IllegalStateException();
			}
			status.setRollbackOnly();
		}

		@Override
		public void rollback() throws IllegalStateException, SystemException {
			throw new SystemException();				
		}

		@Override
		public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException,
				SecurityException, SystemException {
			throw new SystemException();				
		}

		@Override
		public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
			throw new SystemException();
		}

		@Override
		public boolean enlistResource(XAResource xaRes)
				throws IllegalStateException, RollbackException, SystemException {
			throw new SystemException();
		}

		@Override
		public int getStatus() throws SystemException {
			throw new SystemException();
		}
	}
	
	private PlatformTransactionManager platformTransactionManager;
	private WeakHashMap<TransactionStatus, PlatformTransactionAdapter> transactions = new WeakHashMap<>();
	
	public PlatformTransactionManagerAdapter() {
		
	}
	
	public PlatformTransactionManagerAdapter(PlatformTransactionManager platformTransactionManager) {
		this.platformTransactionManager = platformTransactionManager;
	}
	
	public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
		this.platformTransactionManager = platformTransactionManager;
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		try {
			if (platformTransactionManager == null) {
				return null;
			}
			TransactionStatus status = null;
			try {
				status = TransactionAspectSupport.currentTransactionStatus();
			} catch (NoTransactionException e) {
				return null;
			}
			//status = platformTransactionManager.getTransaction(DEFAULT_TRANSACTION_DEFINITION);
			synchronized (transactions) {
				PlatformTransactionAdapter adapter = transactions.get(status);
				if (adapter == null) {
					adapter = new PlatformTransactionAdapter(status);
					transactions.put(status, adapter);
				}
				return adapter;
			}
		} catch (IllegalTransactionStateException e) {
			return null;
		}
	}
	
	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		throw new SystemException();		
	}
	
	@Override
	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
			RollbackException, SecurityException, SystemException {
		throw new SystemException();		
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		throw new SystemException();
	}
	
	@Override
	public Transaction suspend() throws SystemException {
		throw new SystemException();
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		throw new SystemException();		
	}
	
	@Override
	public void resume(Transaction tobj) throws IllegalStateException, InvalidTransactionException, SystemException {
		throw new SystemException();		
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		throw new SystemException();
	}
	
	@Override
	public int getStatus() throws SystemException {
		throw new SystemException();
	}
	
}