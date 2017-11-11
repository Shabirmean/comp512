package Transaction;

import LockManager.LockManager;
import LockManager.DeadlockException;
import ResImpl.*;
import ResInterface.InvalidTransactionException;
import ResInterface.ResourceManager;
import ResInterface.TransactionAbortedException;
import exception.InvalidOperationException;
import exception.RMTransactionFailedException;
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

    private static Integer TRANSACTION_ID_COUNT = 0;
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
                try {
                    for (Transaction transaction : transMap.values()) {
                        int ttl = transaction.updateTimeToLive();
                        if (ttl <= 0) {
                            abort(transaction.getTransactionId());
                        }
                    }
                } catch (InvalidTransactionException e) {
                    e.printStackTrace();
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

    public boolean commit(int transactionId) throws TransactionAbortedException, InvalidOperationException {
        String errMsg;
        boolean status = false;
        Transaction thisTransaction = transMap.get(transactionId);
        //noinspection Duplicates
        if (thisTransaction == null) {
            errMsg = "TId [" + transactionId + "] No valid transactions found with the given transactionId";
            printMsg(errMsg);
            throw new InvalidOperationException(errMsg);
        } else {
            thisTransaction.resetTTL();
        }

        ConcurrentHashMap<String, RMItem> tAccessedItemSet = thisTransaction.getAccessedItemSet();
        try {
            for (ResourceManagerType rmType : ResourceManagerType.values()) {
                ResourceManager rm = resourceManagers.get(rmType);
                List<String> writeList = thisTransaction.getCorrespondingWriteList(rmType);
                List<String> deleteList = thisTransaction.getCorrespondingDeleteList(rmType);

                int rmTId = rm.start();
                for (String itemKey : writeList) {
                    RMItem thisItem = tAccessedItemSet.get(itemKey);
                    rm.writeData(rmTId, itemKey, thisItem);

                    if (deleteList.contains(itemKey)) {
                        rm.deleteItem(rmTId, itemKey);
                        deleteList.remove(itemKey);
                    }
                    writeList.remove(itemKey);
                }

                status = rm.commit(rmTId);
                if (!status) {
                    errMsg = "[" + transactionId + "] Commit attempt to resource manager " +
                            "[" + rmType.getCodeString() + "] with rmTId [" + rmTId + "] failed.";
                    printMsg(errMsg);
                    throw new RMTransactionFailedException(errMsg);
                }

                if (!deleteList.isEmpty()) {
                    errMsg = "[" + transactionId + "] After complete commit to resource manager " +
                            "[" + rmType.getCodeString() + "] with rmTId [" + rmTId + "] deleteList is not empty.";
                    printMsg(errMsg);
                    throw new RMTransactionFailedException(errMsg);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (RMTransactionFailedException e) {
            e.printStackTrace();
        }

        lockMan.UnlockAll(transactionId);
        thisTransaction.clearAll();
        thisTransaction.setStatus(Transaction.TStatus.ENDED);
        transMap.remove(transactionId);
        return status;
    }

    public void abort(int transactionId) throws InvalidTransactionException {
        String errMsg;
        try {
            Transaction thisTransaction = transMap.get(transactionId);
            //noinspection Duplicates
            if (thisTransaction == null) {
                errMsg = "TId [" + transactionId + "] No valid transactions found with the given transactionId";
                printMsg(errMsg);
                throw new InvalidOperationException(errMsg);
            } else {
                thisTransaction.resetTTL();
            }

            lockMan.UnlockAll(transactionId);
            thisTransaction.clearAll();
            thisTransaction.setStatus(Transaction.TStatus.ENDED);
            transMap.remove(transactionId);
        } catch (InvalidOperationException e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("Duplicates")
    public int submitOperation(int tId, RequestType requestType, ReservableItem resourceItem)
            throws InvalidOperationException, InvalidTransactionException {
        int responseNum = ReqStatusNo.FAILURE.getStatusCode();
        String errMsg;
        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                throw new InvalidTransactionException("No transaction exists with transaction id [" + tId + "]");
            } else {
                transaction.resetTTL();
            }

            String itemKey = resourceItem.getKey();
            ResourceManagerType resManType = requestForWhichRM(requestType);
            if (resManType == null) {
                resManType = itemToRMTypeMatcher(itemKey);
                if (resManType == null) {
                    errMsg = "Reserve/UnReserve request for item [" + itemKey + "] cannot be completed since no " +
                            "matching RM found.";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
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
                throw new InvalidOperationException(errMsg);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
//            if (transaction.getDeleteSet().containsKey(itemKey) && !isAddOperation(requestType)) {
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey) && !isAddOperation(requestType)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new InvalidOperationException(errMsg);
            }

//            if (transaction.getDeleteVector().contains(itemKey) && !isAddOperation(requestType)) {
//                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
//                printMsg(errMsg);
//                throw new InvalidOperationException(errMsg);
//            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
//            if (readRequiredOperation(requestType)) {
            ReservableItem alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                int rmTID = resMan.start();
                alreadyAccessedItem = (ReservableItem) resMan.getItem(rmTID, itemKey);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    throw new RMTransactionFailedException(errMsg);
                }

//                if (alreadyAccessedItem == null && !isAddOperation(requestType)) {
//                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
//                            "] with key [" + itemKey + "] returned NULL.";
//                    throw new InvalidOperationException(errMsg);
//
//                } else

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);
                } else if (!isAddOperation(requestType)) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL.";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
            }
//            }

            alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);
            if (isAddOperation(requestType)) {
                //TODO:: Check if item exists
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
//                transaction.getWriteSet().put(itemKey, resManType);
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = ReqStatusNo.SUCCESS.getStatusCode();

            } else if (isDeleteOperation(requestType)) {
                //TODO:: Check if item has reservations
                if (alreadyAccessedItem.getReserved() == 0) {
                    itemSet.remove(itemKey);
//                    transaction.getDeleteSet().put(itemKey, resManType);
                    transaction.getCorrespondingDeleteList(resManType).add(itemKey);
                    responseNum = ReqStatusNo.SUCCESS.getStatusCode();
                    Trace.info("TManager::deleteItem(" + tId + ", " + itemKey + ") item deleted");
                } else {
                    Trace.info("TManager::deleteItem(" + tId + ", " + itemKey + ") item can't be deleted " +
                            "because some customers reserved it");
                }

            } else if (isCountQuery(requestType)) {
                Trace.info("RM::queryNum(" + tId + ", " + itemKey + ") called");
                responseNum = alreadyAccessedItem.getCount();
                Trace.info("RM::queryNum(" + tId + ", " + itemKey + ") returns count=" + responseNum);

            } else if (isPriceQuery(requestType)) {
                Trace.info("RM::queryPrice(" + tId + ", " + itemKey + ") called");
                responseNum = alreadyAccessedItem.getPrice();
                Trace.info("RM::queryPrice(" + tId + ", " + itemKey + ") returns cost=$" + responseNum);

            } else if (requestType == RequestType.UNRESERVE_RESOURCE) {
                int reservedCountOfItem = alreadyAccessedItem.getReserved();
                int reservedByCustomer = resourceItem.getCount();
                alreadyAccessedItem.setReserved(reservedCountOfItem - reservedByCustomer);
                alreadyAccessedItem.setCount(resourceItem.getCount() + reservedByCustomer);
//                transaction.getWriteSet().put(itemKey, resManType);
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = alreadyAccessedItem.getReserved();

            } else if (requestType == RequestType.RESERVE_RESOURCE) {
                String location = alreadyAccessedItem.getLocation();
                Trace.info("RM::reserveItem( " + tId + ", " + itemKey + ", " + location + " ) called");
                int currentCount = alreadyAccessedItem.getCount();
                int currentReservd = alreadyAccessedItem.getReserved();
                alreadyAccessedItem.setCount(currentCount - 1);
                alreadyAccessedItem.setReserved(currentReservd + 1);
//                transaction.getWriteSet().put(itemKey, resManType);
                transaction.getCorrespondingWriteList(resManType).add(itemKey);
                responseNum = alreadyAccessedItem.getPrice();
            }
        } catch (DeadlockException e) {
            e.printStackTrace();
            //TODO:: Handle Deadlock and all Exception
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (TransactionAbortedException e) {
            e.printStackTrace();
        } catch (RMTransactionFailedException e) {
            e.printStackTrace();
        }
        return responseNum; //TODO:: NEED TO CHANGE THIS
    }


    @SuppressWarnings("Duplicates")
    public String submitOperation(int tId, RequestType requestType, Customer customerItem)
            throws InvalidTransactionException, InvalidOperationException {
        String response = ReqStatusNo.FAILURE.getStatus();
        String errMsg;
        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                errMsg = "No transaction exists with transaction id [" + tId + "]";
                printMsg(errMsg);
                throw new InvalidTransactionException(errMsg);
            } else {
                transaction.resetTTL();
            }

            ResourceManagerType resManType = requestForWhichRM(requestType);
            int opType = isReadOWrite(requestType);
            String itemKey = customerItem.getKey();
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
                throw new InvalidOperationException(errMsg);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
//            if (transaction.getDeleteSet().containsKey(itemKey) && !isAddOperation(requestType)) {
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey) && !isAddOperation(requestType)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new InvalidOperationException(errMsg);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
            Customer alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            int customerId = customerItem.getID();

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                int rmTID = resMan.start();
                alreadyAccessedItem = (Customer) resMan.getItem(rmTID, itemKey);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    throw new RMTransactionFailedException(errMsg);
                }

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);

                } else if (!isAddOperation(requestType)) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL.";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
            }

            alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            switch (requestType) {
                case ADD_NEW_CUSTOMER_WITHOUT_ID: {
                    if (alreadyAccessedItem == null) {
                        itemSet.put(itemKey, customerItem);
                        Trace.info("RM::newCustomer(" + tId + ") returns ID=" + customerId);
//                        transaction.getWriteSet().put(itemKey, resManType);
                        transaction.getCorrespondingWriteList(resManType).add(itemKey);
                        response = ReqStatusNo.SUCCESS.getStatus();

                    } else {
                        Trace.info("RM::newCustomer(" + tId + ") returns error. " +
                                "Generated Id : " + customerId + " conflicts with an existing customer.");
                    }
                    break;
                }
                case ADD_NEW_CUSTOMER_WITH_ID: {
                    if (alreadyAccessedItem == null) {
                        itemSet.put(itemKey, customerItem);
                        Trace.info("INFO: RM::newCustomer(" + tId + ", " + customerId + ") created a new customer");
//                        transaction.getWriteSet().put(itemKey, resManType);
                        transaction.getCorrespondingWriteList(resManType).add(itemKey);
                        response = ReqStatusNo.SUCCESS.getStatus();

                    } else {
                        Trace.info("INFO: RM::newCustomer(" + tId + ", " + customerId + ") failed--" +
                                "customer already exists");
                    }
                    break;
                }
                case DELETE_CUSTOMER: {
                    RMHashtable reservationHT = alreadyAccessedItem.getReservations();

                    for (Enumeration e = reservationHT.keys(); e.hasMoreElements(); ) {
                        String reservedkey = (String) (e.nextElement());
                        ReservedItem reserveditem = alreadyAccessedItem.getReservedItem(reservedkey);
                        Trace.info("RM::deleteCustomer(" + tId + ", " + customerId + ") has reserved " +
                                reserveditem.getKey() + " " + reserveditem.getCount() + " times");

                        String resItemKey = reserveditem.getKey();
                        ReservableItem reservableItem = (ReservableItem) itemSet.get(resItemKey);
                        int newReserveCount = submitOperation(tId, RequestType.UNRESERVE_RESOURCE, reservableItem);
                        int newCount = reservableItem.getCount() + (reservableItem.getReserved() - newReserveCount);
                        Trace.info("RM::deleteCustomer(" + tId + ", " + customerId + ") has reserved " +
                                reserveditem.getKey() + "which is now updated to be reserved" + newReserveCount +
                                " times and is now available " + newCount + " times");
                    }

                    itemSet.remove(itemKey);
//                    transaction.getDeleteSet().put(itemKey, resManType);
                    transaction.getCorrespondingDeleteList(resManType).add(itemKey);
                    response = ReqStatusNo.SUCCESS.getStatus();
                    Trace.info("RM::deleteCustomer(" + tId + ", " + customerId + ") succeeded");
                    break;
                }
                case QUERY_CUSTOMER_INFO: {
                    Trace.info("RM::queryCustomerInfo(" + tId + ", " + customerId + ") called");
                    String customerBill = alreadyAccessedItem.printBill();
                    Trace.info("RM::queryCustomerInfo(" + tId + ", " + customerId + "), bill follows...");
                    System.out.println(customerBill);
                    response = customerBill;
                    break;
                }
            }

        } catch (DeadlockException e) {
            e.printStackTrace();
            //TODO:: Handle Deadlock and all Exception
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (TransactionAbortedException e) {
            e.printStackTrace();
        } catch (RMTransactionFailedException e) {
            e.printStackTrace();
        }

        return response;
    }


    @SuppressWarnings("Duplicates")
    public int submitReserveOperation(int tId, Customer customerItem,
                                      RequestType requestType, ReservableItem resourceItem)
            throws InvalidOperationException, InvalidTransactionException {
        int responseNum = ReqStatusNo.FAILURE.getStatusCode();
        String errMsg;
        try {
            Transaction transaction = transMap.get(tId);
            if (transaction == null) {
                errMsg = "No transaction exists with transaction id [" + tId + "]";
                printMsg(errMsg);
                throw new InvalidTransactionException(errMsg);
            } else {
                transaction.resetTTL();
            }

            String itemKey = customerItem.getKey();
            ResourceManagerType resManType = itemToRMTypeMatcher(itemKey);
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
                throw new InvalidOperationException(errMsg);
            }

//             If the item has already deleted (found in this transactions delete set and operation is not ADD)
//            if (transaction.getDeleteSet().containsKey(itemKey)) {
            if (transaction.getCorrespondingDeleteList(resManType).contains(itemKey)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new InvalidOperationException(errMsg);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
            Customer alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            int customerId = customerItem.getID();

            if (alreadyAccessedItem == null) {
                ResourceManager resMan = resourceManagers.get(resManType);
                int rmTID = resMan.start();
                alreadyAccessedItem = (Customer) resMan.getItem(rmTID, itemKey);
                boolean commitStat = resMan.commit(rmTID);

                if (!commitStat) {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to Query [" + resManType.getCodeString() +
                            "] data for item with key [" + itemKey + "] could not be committed.";
                    printMsg(errMsg);
                    throw new RMTransactionFailedException(errMsg);
                }

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);

                } else {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL. Cannot make reservations for this customer.";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
            }

            int reservedItemPrice = submitOperation(tId, RequestType.RESERVE_RESOURCE, resourceItem);
            String resourceKey = resourceItem.getKey();
            String resourceLoc = resourceItem.getLocation();
            alreadyAccessedItem = (Customer) itemSet.get(itemKey);
            alreadyAccessedItem.reserve(resourceKey, resourceLoc, reservedItemPrice);
            Trace.info("RM::reserveItem( " + tId + ", " + customerId + ", " + resourceKey + ", " +
                    resourceLoc + ") succeeded");
//            transaction.getWriteSet().put(itemKey, resManType);
            transaction.getCorrespondingWriteList(resManType).add(itemKey);
            responseNum = ReqStatusNo.SUCCESS.getStatusCode();

        } catch (DeadlockException e) {
            e.printStackTrace();
            //TODO:: Handle Deadlock and all Exception
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (TransactionAbortedException e) {
            e.printStackTrace();
        } catch (RMTransactionFailedException e) {
            e.printStackTrace();
        }
        return responseNum;
    }


    public boolean submitReserveItineraryOperation(int tId, int cId, Vector flightNumbers,
                                                   String location, boolean bookCar, boolean bookHotel) {
        String errMsg;
        int responseVal = ReqStatusNo.FAILURE.getStatusCode();
        Customer customer = new Customer(cId);
        try {
            for (Object flightNumber : flightNumbers) {
                int flNumber = (int) flightNumber;
                Flight flightToBook = new Flight(flNumber, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, flightToBook);
                if (responseVal == ReqStatusNo.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + flightToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }

            if (bookCar) {
                Car carToBook = new Car(location, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, carToBook);
                if (responseVal == ReqStatusNo.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + carToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }

            if (bookHotel) {
                Hotel hotelToBook = new Hotel(location, VALUE_NOT_SET, VALUE_NOT_SET);
                responseVal = submitReserveOperation(tId, customer, RequestType.RESERVE_RESOURCE, hotelToBook);
                if (responseVal == ReqStatusNo.FAILURE.getStatusCode()) {
                    errMsg = "[" + tId + "] Itinerary reservation failed whilst trying to " +
                            "reserve " + hotelToBook.getKey() + ". Aborting transaction...";
                    printMsg(errMsg);
                    abort(tId);
                }
            }
        } catch (InvalidOperationException | InvalidTransactionException e) {
            e.printStackTrace();
        }
        return responseVal == ReqStatusNo.SUCCESS.getStatusCode();
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
        int readOWrite = VALUE_NOT_SET; //TODO:: Need to add for reserveItinerary

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


//    private boolean isReserveRequest(RequestType requestType) {
//        return requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
//                requestType == RequestType.RESERVE_ROOM;
//    }
//
//    private boolean readRequiredOperation(RequestType requestType) {
//        return requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE ||
//                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE ||
//                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE ||
//                requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
//                requestType == RequestType.RESERVE_ROOM || requestType == RequestType.RESERVE_ITINERARY ||
//                requestType == RequestType.QUERY_CUSTOMER_INFO;
//    }

    private void printMsg(String msg) {
        System.out.println("[Transaction Manager] " + msg);
    }

    public enum ReqStatusNo {
        SUCCESS(0),
        FAILURE(-1);

        private final int statusCode;
        private String status;

        ReqStatusNo(int statusCode) {
            this.statusCode = statusCode;
            switch (statusCode) {
                case 0:
                    this.status = "SUCCESS";
                    break;
                case 1:
                    this.status = "FAILURE";
                    break;
            }
        }

        public int getStatusCode() {
            return this.statusCode;
        }

        public String getStatus() {
            return this.status;
        }
    }
}
