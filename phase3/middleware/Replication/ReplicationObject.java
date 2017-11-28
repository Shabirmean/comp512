package Replication;

import LockManager.LockManager;
import MiddlewareImpl.MWResourceManager;
import Transaction.Transaction;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shabirmean on 2017-11-27 with some hope.
 */
public class ReplicationObject implements Serializable {
    private Integer transactionIdCount;
    private LockManager lockMan;
    private ConcurrentHashMap<Integer, Transaction> transMap;
    private MWResourceManager customerManager;
    //TODO:: Customer Hashtable


    public Integer getTransactionIdCount() {
        return transactionIdCount;
    }

    public void setTransactionIdCount(Integer transactionIdCount) {
        this.transactionIdCount = transactionIdCount;
    }

    public LockManager getLockMan() {
        return lockMan;
    }

    public void setLockMan(LockManager lockMan) {
        this.lockMan = lockMan;
    }

    public ConcurrentHashMap<Integer, Transaction> getTransMap() {
        return transMap;
    }

    public void setTransMap(ConcurrentHashMap<Integer, Transaction> transMap) {
        this.transMap = transMap;
    }

    public MWResourceManager getCustomerManager() {
        return customerManager;
    }

    public void setCustomerManager(MWResourceManager customerManager) {
        this.customerManager = customerManager;
    }

}
