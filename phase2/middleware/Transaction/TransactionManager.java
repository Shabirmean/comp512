package Transaction;

import LockManager.DeadlockException;
import LockManager.LockManager;
import ResImpl.*;
import ResInterface.InvalidTransactionException;
import ResInterface.ResourceManager;
import ResInterface.TransactionAbortedException;
import exception.TransactionManagerException;
import util.RequestType;
import util.ResourceManagerType;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    public static final int VALUE_NOT_SET = -1;
    private static final String FLIGHT_ITEM_KEY = "flight";
    private static final String CAR_ITEM_KEY = "car";
    private static final String HOTEL_ITEM_KEY = "room";
    private static final String CUSTOMER_ITEM_KEY = "customer";
    private static final Object countLock = new Object();
    private static final Timer transactionTimer = new Timer();
    private static final Timer shutdownTimer = new Timer();

    private static Integer TRANSACTION_ID_COUNT = 1001;
    private static LockManager lockMan = new LockManager();
    private static HashMap<ResourceManagerType, ResourceManager> resourceManagers = new HashMap<>();
    private static ConcurrentHashMap<Integer, Transaction> transMap = new ConcurrentHashMap<Integer, Transaction>();


    public TransactionManager(ResourceManager carManager, ResourceManager flightManager,
                              ResourceManager hotelManager, ResourceManager customerManager) {
        resourceManagers.put(ResourceManagerType.CAR, carManager);
        resourceManagers.put(ResourceManagerType.FLIGHT, flightManager);
        resourceManagers.put(ResourceManagerType.HOTEL, hotelManager);
        resourceManagers.put(ResourceManagerType.CUSTOMER, customerManager);
    }

    public void initTransactionManager() {
        transactionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int tiD = -1;
                try {
                    for (Transaction transaction : transMap.values()) {
                        int ttl = transaction.updateTimeToLive();
                        if (ttl <= 0) {
                            tiD = transaction.getTransactionId();
                            System.out.println("TM:: Transaction-" + tiD + " has timed-out. Aborting it....");
                            abort(tiD);
                        }
                    }
                } catch (TransactionManagerException e) {
                    String errMsg = "Attempted to abort an invalid transaction [" + tiD + "] " +
                            "that seemed to have timed-out." + e;
                    printMsg(errMsg);
                }
            }
        }, 0, 5 * 1000);
    }

    public int start() throws RemoteException {
        int newTId;
        synchronized (countLock) {
            newTId = TRANSACTION_ID_COUNT++;
        }
        Transaction newTrans = new Transaction(newTId);
        transMap.put(newTId, newTrans);
        return newTId;
    }

    public boolean commit(int transId) throws TransactionManagerException {
        String errMsg;
        boolean status = false;
        Transaction thisTransaction = transMap.get(transId);
        //noinspection Duplicates
        if (thisTransaction == null) {
            errMsg = "TId [" + transId + "] No valid ResInterface.transactions found with the given " +
                    "transactionId";
            printMsg(errMsg);
            throw new TransactionManagerException(errMsg, ReqStatus.INVALID_TRANSACTION_ID);
        } else {
            thisTransaction.resetTTL();
        }

        ConcurrentHashMap<String, RMItem> tAccessedItemSet = thisTransaction.getAccessedItemSet();
        int rmTId = -1;
        String rmNow = "";

        try {
            for (ResourceManagerType rmType : ResourceManagerType.values()) {
                rmNow = rmType.getCodeString();
                ResourceManager rm = resourceManagers.get(rmType);
                List<String> writeList = thisTransaction.getCorrespondingWriteList(rmType);
                List<String> deleteList = thisTransaction.getCorrespondingDeleteList(rmType);

                if (writeList.isEmpty() && deleteList.isEmpty()) {
                    status = true;
                    continue;
                }

                rmTId = rm.start();
                for (String itemKey : writeList) {
                    RMItem thisItem = tAccessedItemSet.get(itemKey);
                    rm.writeData(rmTId, itemKey, thisItem);

                    if (deleteList.contains(itemKey)) {
                        rm.deleteItem(rmTId, itemKey);
                        deleteList.remove(itemKey);
                    }
                }

                for (String itemKey : deleteList) {
                    rm.deleteItem(rmTId, itemKey);
                }

                status = rm.commit(rmTId);
                if (!status) {
                    errMsg = "[" + transId + "] Commit attempt to resource manager " +
                            "[" + rmType.getCodeString() + "] with rmTId [" + rmTId + "] failed.";
                    printMsg(errMsg);
                    rm.abort(rmTId);
                    throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);
                }
            }
        } catch (RemoteException e) {
            errMsg = "An error occurred with transaction [" + rmTId + "] on RM [" + rmNow + "] at middleware.";
            printMsg(errMsg);
            abort(transId);
            throw new TransactionManagerException(errMsg, ReqStatus.MW_RM_COMMUNICATION_FAILED);
        } catch (InvalidTransactionException | TransactionAbortedException e) {
            errMsg = "TId [" + transId + "] Call to abort transaction [" + rmTId + "] at RM [" + rmNow + "] failed.";
            printMsg(errMsg);
            abort(transId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_OR_ABORT_TO_RM_THROWED_ERROR);
        }

        lockMan.UnlockAll(transId);
        thisTransaction.clearAll();
        thisTransaction.setStatus(Transaction.TStatus.ENDED);
        transMap.remove(transId);
        return status;
    }

    public void abort(int transactionId) throws TransactionManagerException {
        String errMsg;
        Transaction thisTransaction = transMap.get(transactionId);
        //noinspection Duplicates
        if (thisTransaction == null) {
            errMsg = "TId [" + transactionId + "] No valid transactions found with the given transactionId";
            printMsg(errMsg);
            throw new TransactionManagerException(errMsg, ReqStatus.INVALID_TRANSACTION_ID);
        }

        System.out.println("TM:: Aborting transaction-" + transactionId + "....");
        lockMan.UnlockAll(transactionId);
        thisTransaction.clearAll();
        thisTransaction.setStatus(Transaction.TStatus.ENDED);
        transMap.remove(transactionId);
    }


    public boolean shutdown() throws TransactionManagerException {
        boolean rmShutdownStatus = false;
        String errMsg;
        String rmNow = null;
        String shutRMs = "";
        try {
            if (transMap.isEmpty()) {
                printMsg("Shutdown invoked and no active transactions. Hence shutting down RMs!!");
                for (ResourceManagerType rmType : ResourceManagerType.values()) {
                    rmNow = rmType.getCodeString();
                    ResourceManager rm = resourceManagers.get(rmType);
                    rmShutdownStatus = rm.shutdown();
                    if (!rmShutdownStatus) {
                        errMsg = "Shutdown call to RM [" + rmNow + "] said NO. Already shutdown RMs - " + shutRMs;
                        printMsg(errMsg);
                        throw new TransactionManagerException(errMsg, ReqStatus.SHUTDOWN_AT_TM_MIGHT_BE_PARTIAL);
                    }
                    shutRMs = rmNow + ", ";
                }

                shutdownTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 5000);
            } else {
                errMsg = "Transaction manager still has active transactions. Hence, cannot shutdown.";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.ACTIVE_TRANSACTIONS_EXIST);
            }
        } catch (RemoteException e) {
            errMsg = "Shutdown call to RM [" + rmNow + "] throwed an error.";
            printMsg(errMsg);
            throw new TransactionManagerException(errMsg, ReqStatus.SHUTDOWN_TO_RM_THROWED_ERROR);
        }
        return rmShutdownStatus;
    }

    @SuppressWarnings("Duplicates")
    public int submitOperation(int tId, RequestType requestType, ReservableItem resourceItem)
            throws TransactionManagerException {
        String errMsg;
        String rmType = "";
        String itemKey = "";
        int rmTID = -1;
        int responseNum = ReqStatus.FAILURE.getStatusCode();

        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                errMsg = "No transaction exists with transaction id [" + tId + "]";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.INVALID_TRANSACTION_ID);
            } else {
                transaction.resetTTL();
            }

            itemKey = resourceItem.getKey();
            ResourceManagerType resManType = requestForWhichRM(requestType);
            if (resManType == null) {
                resManType = itemToRMTypeMatcher(itemKey);
                if (resManType == null) {
                    errMsg = "Reserve/UnReserve request for item [" + itemKey + "] cannot be completed since no " +
                            "matching RM found.";
                    printMsg(errMsg);
                    throw new TransactionManagerException(errMsg, ReqStatus.NO_MATCHING_RM);
                }
            } else {
                rmType = resManType.getCodeString();
            }

            int opType = isReadOWrite(requestType);
            boolean tryLock = lockMan.Lock(tId, itemKey, opType);

            if (!tryLock) {
                if (tId < 0) {
                    errMsg = "Operation invalid given a negative Transaction Id [" + tId + "]";
                } else if (itemKey == null) {
                    errMsg = "Operation on transaction with Id [" + tId + "] is invalid given a NULL item.";
                } else {
                    errMsg = "Operation on transaction with Id [" + tId + "] for item with key [" + itemKey + "] " +
                            "is invalid due unknown Operation-TYPE";
                }
                printMsg("Lock request on item [" + itemKey + "] for transaction [" + tId + "] failed.\n    " + errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.LOCK_REQUEST_FAILED);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey) && !isAddOperation(requestType)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_DELETED_ITEM);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
            ReservableItem alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                rmTID = resMan.start();
//                alreadyAccessedItem = (ReservableItem) resMan.getItem(rmTID, itemKey);
                RMItem itemFromRM = resMan.getItem(rmTID, itemKey);
                alreadyAccessedItem = (ReservableItem) getCopyOfItem(itemFromRM);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    resMan.abort(rmTID);
                    throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);
                }

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);
                } else if (!isAddOperation(requestType)) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL.";
                    printMsg(errMsg);
                    throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_NULL_ITEM);
                }
            }

            alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);
            if (isAddOperation(requestType)) {
                String itemUniqueAttrib = resourceItem.getLocation();
                int itemCount = resourceItem.getCount();
                int itemPrice = resourceItem.getPrice();
                if (alreadyAccessedItem == null) {
                    itemSet.put(itemKey, resourceItem);
                    Trace.info("TManager::addItem(" + tId + ") created new " + resManType.getCodeString() + " " +
                            itemUniqueAttrib + ", seats=" + itemCount + ", price=$" + itemPrice);
                } else {
                    int currentSeatCount = alreadyAccessedItem.getCount();
                    int newSeatCount = resourceItem.getCount();
                    int newPrice = resourceItem.getPrice();

                    alreadyAccessedItem.setCount(currentSeatCount + newSeatCount);
                    if (newPrice > 0) {
                        alreadyAccessedItem.setPrice(newPrice);
                    }
                    Trace.info("TManager::addFlight(" + tId + ") modified existing " + resManType.getCodeString()
                            + " " + itemUniqueAttrib + ", " + "seats=" + itemCount + ", price=$" + itemPrice);
                }
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = ReqStatus.SUCCESS.getStatusCode();

            } else if (isDeleteOperation(requestType)) {
                if (alreadyAccessedItem.getReserved() == 0) {
                    itemSet.remove(itemKey);
                    transaction.getCorrespondingDeleteList(resManType).add(itemKey);
                    responseNum = ReqStatus.SUCCESS.getStatusCode();
                    Trace.info("TManager::deleteItem(" + tId + ", " + itemKey + ") item deleted");
                } else {
                    Trace.info("TManager::deleteItem(" + tId + ", " + itemKey + ") item can't be deleted " +
                            "because some customers reserved it");
                }

            } else if (isCountQuery(requestType)) {
                Trace.info("TM::queryNum(" + tId + ", " + itemKey + ") called");
                responseNum = alreadyAccessedItem.getCount();
                Trace.info("TM::queryNum(" + tId + ", " + itemKey + ") returns count=" + responseNum);

            } else if (isPriceQuery(requestType)) {
                Trace.info("TM::queryPrice(" + tId + ", " + itemKey + ") called");
                responseNum = alreadyAccessedItem.getPrice();
                Trace.info("TM::queryPrice(" + tId + ", " + itemKey + ") returns cost=$" + responseNum);

            } else if (requestType == RequestType.UNRESERVE_RESOURCE) {
                int reservedCountOfItem = alreadyAccessedItem.getReserved();
                int reservedByCustomer = resourceItem.getCount();
                alreadyAccessedItem.setReserved(reservedCountOfItem - reservedByCustomer);
                alreadyAccessedItem.setCount(resourceItem.getCount() + reservedByCustomer);
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = alreadyAccessedItem.getReserved();

            } else if (requestType == RequestType.RESERVE_RESOURCE) {
                String location = alreadyAccessedItem.getLocation();
                Trace.info("TM::reserveItem( " + tId + ", " + itemKey + ", " + location + " ) called");
                int currentCount = alreadyAccessedItem.getCount();
                int currentReservd = alreadyAccessedItem.getReserved();
                alreadyAccessedItem.setCount(currentCount - 1);
                alreadyAccessedItem.setReserved(currentReservd + 1);
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = alreadyAccessedItem.getPrice();
            }
        } catch (DeadlockException e) {
            errMsg = "TId [" + tId + "] failed its LOCK request on item [" + resourceItem.getKey() + "] on DEADLOCK.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.DEADLOCK_MET);

        } catch (RemoteException e) {
            errMsg = "An error occurred with transaction [" + rmTID + "] on RM [" + rmType + "] at middleware.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.MW_RM_COMMUNICATION_FAILED);

        } catch (TransactionAbortedException e) {
            errMsg = "TId [" + tId + "] Call to commit at RM [" + rmType + "] data for item with " +
                    "key [" + itemKey + "] could not be committed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);

        } catch (InvalidTransactionException e) {
            errMsg = "TId [" + tId + "] Call to abort transaction [" + rmTID + "] at RM [" + rmType + "] failed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_OR_ABORT_TO_RM_THROWED_ERROR);

        }
        return responseNum;
    }


    @SuppressWarnings("Duplicates")
    public String submitOperation(int tId, RequestType requestType, Customer customerItem)
            throws TransactionManagerException {
        String errMsg;
        String rmType = "";
        String itemKey = "";
        int rmTID = -1;
        String response = ReqStatus.FAILURE.getStatus();

        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                errMsg = "No transaction exists with transaction id [" + tId + "]";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.INVALID_TRANSACTION_ID);
            } else {
                transaction.resetTTL();
            }

            ResourceManagerType resManType = requestForWhichRM(requestType);
            rmType = resManType.getCodeString();
            int opType = isReadOWrite(requestType);
            itemKey = customerItem.getKey();
            boolean tryLock = lockMan.Lock(tId, itemKey, opType);

            if (!tryLock) {
                if (tId < 0) {
                    errMsg = "Operation invalid given a negative Transaction Id [" + tId + "]";
                } else if (itemKey == null) {
                    errMsg = "Operation on transaction with Id [" + tId + "] is invalid given a NULL item.";
                } else {
                    errMsg = "Operation on transaction with Id [" + tId + "] for item with key [" + itemKey + "] " +
                            "is invalid due unknown Operation-TYPE";
                }
                printMsg("Lock request on item [" + itemKey + "] for transaction [" + tId + "] failed.\n    " + errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.LOCK_REQUEST_FAILED);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey) && !isAddOperation(requestType)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_DELETED_ITEM);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
            Customer alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            int customerId = customerItem.getID();

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                rmTID = resMan.start();
//                alreadyAccessedItem = (Customer) resMan.getItem(rmTID, itemKey);
                RMItem itemFromRM = resMan.getItem(rmTID, itemKey);
                alreadyAccessedItem = (Customer) getCopyOfItem(itemFromRM);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    resMan.abort(rmTID);
                    throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);
                }

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);

                } else if (!isAddOperation(requestType)) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL.";
                    printMsg(errMsg);
                    throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_NULL_ITEM);
                }
            }

            alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            switch (requestType) {
                case ADD_NEW_CUSTOMER_WITHOUT_ID: {
                    if (alreadyAccessedItem == null) {
                        itemSet.put(itemKey, customerItem);
                        Trace.info("TM::newCustomer(" + tId + ") returns ID=" + customerId);
                        transaction.getCorrespondingWriteList(resManType).add(itemKey);
                        response = ReqStatus.SUCCESS.getStatus();

                    } else {
                        Trace.info("TM::newCustomer(" + tId + ") returns error. " +
                                "Generated Id : " + customerId + " conflicts with an existing customer.");
                    }
                    break;
                }
                case ADD_NEW_CUSTOMER_WITH_ID: {
                    if (alreadyAccessedItem == null) {
                        itemSet.put(itemKey, customerItem);
                        Trace.info("INFO: TM::newCustomer(" + tId + ", " + customerId + ") created a new customer");
//                        transaction.getWriteSet().put(itemKey, resManType);
                        transaction.getCorrespondingWriteList(resManType).add(itemKey);
                        response = ReqStatus.SUCCESS.getStatus();

                    } else {
                        Trace.info("INFO: TM::newCustomer(" + tId + ", " + customerId + ") failed--" +
                                "customer already exists");
                    }
                    break;
                }
                case DELETE_CUSTOMER: {
                    RMHashtable reservationHT = alreadyAccessedItem.getReservations();

                    for (Enumeration e = reservationHT.keys(); e.hasMoreElements(); ) {
                        String reservedkey = (String) (e.nextElement());
                        ReservedItem reserveditem = alreadyAccessedItem.getReservedItem(reservedkey);
                        Trace.info("TM::deleteCustomer(" + tId + ", " + customerId + ") has reserved " +
                                reserveditem.getKey() + " " + reserveditem.getCount() + " times");

                        ReservableItem reservableItem = getCopyOfReservedItem(reserveditem);
                        int newReserveCount = submitOperation(tId, RequestType.UNRESERVE_RESOURCE, reservableItem);
                        int newCount = reservableItem.getCount() + (reservableItem.getReserved() - newReserveCount);
                        Trace.info("TM::deleteCustomer(" + tId + ", " + customerId + ") has reserved " +
                                reserveditem.getKey() + "which is now updated to be reserved" + newReserveCount +
                                " times and is now available " + newCount + " times");
                    }

                    itemSet.remove(itemKey);
                    transaction.getCorrespondingDeleteList(resManType).add(itemKey);
                    response = ReqStatus.SUCCESS.getStatus();
                    Trace.info("TM::deleteCustomer(" + tId + ", " + customerId + ") succeeded");
                    break;
                }
                case QUERY_CUSTOMER_INFO: {
                    Trace.info("TM::queryCustomerInfo(" + tId + ", " + customerId + ") called");
                    String customerBill = alreadyAccessedItem.printBill();
                    Trace.info("TM::queryCustomerInfo(" + tId + ", " + customerId + "), bill follows...");
                    System.out.println(customerBill);
                    response = customerBill;
                    break;
                }
            }

        } catch (DeadlockException e) {
            errMsg = "TId [" + tId + "] failed its LOCK request on item [" + customerItem.getKey() + "] on DEADLOCK.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.DEADLOCK_MET);

        } catch (RemoteException e) {
            errMsg = "An error occurred with transaction [" + rmTID + "] on RM [" + rmType + "] at middleware.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.MW_RM_COMMUNICATION_FAILED);

        } catch (TransactionAbortedException e) {
            errMsg = "TId [" + tId + "] Call to commit at RM [" + rmType + "] data for item with " +
                    "key [" + itemKey + "] could not be committed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);

        } catch (InvalidTransactionException e) {
            errMsg = "TId [" + tId + "] Call to abort transaction [" + rmTID + "] at RM [" + rmType + "] failed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_OR_ABORT_TO_RM_THROWED_ERROR);
        }

        return response;
    }


    //TODO:: Handle DEALock scenario only evict one transaction and not both
    //TODO:: Handle scenario for itinerary call with no items
    //TODO:: Handle scenario where changes reflected on resource but not customer.
    @SuppressWarnings("Duplicates")
    public int submitReserveOperation(int tId, Customer customerItem, RequestType requestType,
                                      ReservableItem resourceItem) throws TransactionManagerException {
        String errMsg;
        String rmType = "";
        String itemKey = "";
        int rmTID = -1;
        int responseNum;

        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                errMsg = "No transaction exists with transaction id [" + tId + "]";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.INVALID_TRANSACTION_ID);
            } else {
                transaction.resetTTL();
            }

            itemKey = customerItem.getKey();
            ResourceManagerType resManType = itemToRMTypeMatcher(itemKey);
            rmType = resManType.getCodeString();
            int opType = isReadOWrite(requestType);
            boolean tryLock = lockMan.Lock(tId, itemKey, opType);

            if (!tryLock) {
                if (tId < 0) {
                    errMsg = "Operation invalid given a negative Transaction Id [" + tId + "]";
                } else if (itemKey == null) {
                    errMsg = "Operation on transaction with Id [" + tId + "] is invalid given a NULL item.";
                } else {
                    errMsg = "Operation on transaction with Id [" + tId + "] for item with key [" + itemKey + "] " +
                            "is invalid due unknown Operation-TYPE";
                }
                printMsg("Lock request on item [" + itemKey + "] for transaction [" + tId + "] failed.\n    " + errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.LOCK_REQUEST_FAILED);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_DELETED_ITEM);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
            Customer alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            int customerId = customerItem.getID();

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                rmTID = resMan.start();
//                alreadyAccessedItem = (Customer) resMan.getItem(rmTID, itemKey);
                RMItem itemFromRM = resMan.getItem(rmTID, itemKey);
                alreadyAccessedItem = (Customer) getCopyOfItem(itemFromRM);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);
                }

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);

                } else {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL. Cannot make reservations for this customer.";
                    printMsg(errMsg);
                    throw new TransactionManagerException(errMsg, ReqStatus.REQUEST_ON_NULL_ITEM);
                }
            }

            int reservedItemPrice = submitOperation(tId, RequestType.RESERVE_RESOURCE, resourceItem);
            String resourceKey = resourceItem.getKey();
            String resourceLoc = resourceItem.getLocation();
            alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            alreadyAccessedItem.reserve(resourceKey, resourceLoc, reservedItemPrice);
            Trace.info("TM::reserveItem( " + tId + ", " + customerId + ", " + resourceKey + ", " +
                    resourceLoc + ") succeeded");
            transaction.getCorrespondingWriteList(resManType).add(itemKey);
            responseNum = ReqStatus.SUCCESS.getStatusCode();

        } catch (DeadlockException e) {
            errMsg = "TId [" + tId + "] failed its LOCK request on item [" + customerItem.getKey() + "] on DEADLOCK.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.DEADLOCK_MET);

        } catch (RemoteException e) {
            errMsg = "An error occurred with transaction [" + rmTID + "] on RM [" + rmType + "] at middleware.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.MW_RM_COMMUNICATION_FAILED);

        } catch (TransactionAbortedException e) {
            errMsg = "TId [" + tId + "] Call to commit at RM [" + rmType + "] data for item with " +
                    "key [" + itemKey + "] could not be committed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_TO_RM_FAILED);

        } catch (InvalidTransactionException e) {
            errMsg = "TId [" + tId + "] Call to abort transaction [" + rmTID + "] at RM [" + rmType + "] failed.";
            printMsg(errMsg);
            abort(tId);
            throw new TransactionManagerException(errMsg, ReqStatus.COMMIT_OR_ABORT_TO_RM_THROWED_ERROR);
        }
        return responseNum;
    }


    public boolean submitReserveItineraryOperation(int tId, int cId, Vector flightNumbers,
                                                   String location, boolean bookCar, boolean bookHotel)
            throws TransactionManagerException {
        String errMsg;
        int responseVal = ReqStatus.FAILURE.getStatusCode();
        Customer customer = new Customer(cId);
        try {
            for (Object flightNumber : flightNumbers) {
                int flNumber = Integer.parseInt((String) flightNumber);
                Flight flightToBook = new Flight(flNumber, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, flightToBook);
                if (responseVal == ReqStatus.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + flightToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }

            if (bookCar) {
                Car carToBook = new Car(location, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, carToBook);
                if (responseVal == ReqStatus.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + carToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }

            if (bookHotel) {
                Hotel hotelToBook = new Hotel(location, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, hotelToBook);
                if (responseVal == ReqStatus.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + hotelToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }
        } catch (TransactionManagerException e) {
            printMsg("(Itinerary-Req) Reserve request for some items of transaction [" + tId + "] failed.");
            abort(tId);
            throw e;
        }
        return responseVal == ReqStatus.SUCCESS.getStatusCode();
    }


    private ResourceManagerType itemToRMTypeMatcher(String itemKey) {
        ResourceManagerType matchingResourceMan = null;
        if (itemKey.contains(FLIGHT_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.FLIGHT;

        } else if (itemKey.contains(CAR_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.CAR;

        } else if (itemKey.contains(HOTEL_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.HOTEL;

        } else if (itemKey.contains(CUSTOMER_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.CUSTOMER;

        }
        return matchingResourceMan;
    }

    private ResourceManagerType requestForWhichRM(RequestType requestType) {
        ResourceManagerType matchingResourceMan = null;

        if (requestType == RequestType.ADD_FLIGHT || requestType == RequestType.DELETE_FLIGHT ||
                requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE) {
            matchingResourceMan = ResourceManagerType.FLIGHT;

        } else if (requestType == RequestType.ADD_CARS || requestType == RequestType.DELETE_CARS ||
                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE) {
            matchingResourceMan = ResourceManagerType.CAR;

        } else if (requestType == RequestType.ADD_ROOMS || requestType == RequestType.DELETE_ROOMS ||
                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE) {
            matchingResourceMan = ResourceManagerType.HOTEL;

        } else if (requestType == RequestType.ADD_NEW_CUSTOMER_WITH_ID ||
                requestType == RequestType.ADD_NEW_CUSTOMER_WITHOUT_ID ||
                requestType == RequestType.QUERY_CUSTOMER_INFO || requestType == RequestType.DELETE_CUSTOMER) {
            matchingResourceMan = ResourceManagerType.CUSTOMER;
        }
        return matchingResourceMan;
    }


    private int isReadOWrite(RequestType requestType) {
        int readOWrite = VALUE_NOT_SET;

        if (requestType == RequestType.ADD_FLIGHT || requestType == RequestType.DELETE_FLIGHT ||
                requestType == RequestType.ADD_CARS || requestType == RequestType.DELETE_CARS ||
                requestType == RequestType.ADD_ROOMS || requestType == RequestType.DELETE_ROOMS ||
                requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
                requestType == RequestType.RESERVE_ROOM || requestType == RequestType.ADD_NEW_CUSTOMER_WITH_ID ||
                requestType == RequestType.ADD_NEW_CUSTOMER_WITHOUT_ID || requestType == RequestType.DELETE_CUSTOMER ||
                requestType == RequestType.UNRESERVE_RESOURCE || requestType == RequestType.RESERVE_ITINERARY ||
                requestType == RequestType.RESERVE_RESOURCE) {
            readOWrite = LockManager.WRITE;

        } else if (requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE ||
                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE ||
                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE ||
                requestType == RequestType.QUERY_CUSTOMER_INFO) {
            readOWrite = LockManager.READ;

        }
        return readOWrite;
    }

    private boolean isAddOperation(RequestType requestType) {
        return requestType == RequestType.ADD_FLIGHT || requestType == RequestType.ADD_CARS ||
                requestType == RequestType.ADD_ROOMS || requestType == RequestType.ADD_NEW_CUSTOMER_WITHOUT_ID ||
                requestType == RequestType.ADD_NEW_CUSTOMER_WITH_ID;
    }

    private boolean isDeleteOperation(RequestType requestType) {
        return requestType == RequestType.DELETE_FLIGHT || requestType == RequestType.DELETE_CARS ||
                requestType == RequestType.DELETE_ROOMS || requestType == RequestType.DELETE_CUSTOMER;
    }

    private boolean isCountQuery(RequestType requestType) {
        return requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_CARS ||
                requestType == RequestType.QUERY_ROOMS;
    }

    private boolean isPriceQuery(RequestType requestType) {
        return requestType == RequestType.QUERY_FLIGHT_PRICE || requestType == RequestType.QUERY_CAR_PRICE ||
                requestType == RequestType.QUERY_ROOM_PRICE;
    }

    private RMItem getCopyOfItem(RMItem itemToCopy) {
        RMItem copyItem = null;
        if (itemToCopy instanceof Flight) {
            copyItem = new Flight((Flight) itemToCopy);

        } else if (itemToCopy instanceof Car) {
            copyItem = new Car((Car) itemToCopy);

        } else if (itemToCopy instanceof Hotel) {
            copyItem = new Hotel((Hotel) itemToCopy);

        } else if (itemToCopy instanceof Customer) {
            copyItem = new Customer((Customer) itemToCopy);
        }
        return copyItem;
    }


    private ReservableItem getCopyOfReservedItem(ReservedItem itemToCopy) {
        ReservableItem copyItem = null;
        String itemKey = itemToCopy.getKey();
        int count = itemToCopy.getCount();
        int price = itemToCopy.getPrice();
        String location = itemToCopy.getLocation();

        if (itemKey.contains(FLIGHT_ITEM_KEY)) {
            copyItem = new Flight(Integer.parseInt(location), count, price);

        } else if (itemKey.contains(CAR_ITEM_KEY)) {
            copyItem = new Car(location, count, price);

        } else if (itemKey.contains(HOTEL_ITEM_KEY)) {
            copyItem = new Hotel(location, count, price);
        }
        return copyItem;
    }


    private void printMsg(String msg) {
        System.out.println("TM:: " + msg);
    }
}
