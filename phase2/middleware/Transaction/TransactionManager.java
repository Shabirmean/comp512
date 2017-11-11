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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    public static final int VALUE_NOT_SET = -1;
    public static final String FLIGHT_ITEM_KEY = "flight";
    public static final String CAR_ITEM_KEY = "car";
    public static final String HOTEL_ITEM_KEY = "room";
    private static LockManager lockMan = new LockManager();
    private static HashMap<ResourceManagerType, ResourceManager> resourceManagers = new HashMap<>();
    private static ConcurrentHashMap<Integer, Transaction> transactnMap = new ConcurrentHashMap<Integer, Transaction>();
    //    private static final UUID uuid = UUID.randomUUID();
    private static final Object countLock = new Object();
    private static Integer TRANSACTION_ID_COUNT = 0;


    public TransactionManager(ResourceManager carManager, ResourceManager flightManager,
                              ResourceManager hotelManager, ResourceManager customerManager) {
        resourceManagers.put(ResourceManagerType.CAR, carManager);
        resourceManagers.put(ResourceManagerType.FLIGHT, flightManager);
        resourceManagers.put(ResourceManagerType.HOTEL, hotelManager);
        resourceManagers.put(ResourceManagerType.CUSTOMER, customerManager);
    }


    public int start() throws RemoteException {
//        int newTId = uuid.hashCode();
        int newTId;
        synchronized (countLock) {
            newTId = TRANSACTION_ID_COUNT++;
        }
        Transaction newTrans = new Transaction(newTId);
        transactnMap.put(newTId, newTrans);
        return newTId;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException {
        return true;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {

    }


    @SuppressWarnings("Duplicates")
    public int submitOperation(int tId, RequestType requestType, ReservableItem resourceItem)
            throws InvalidOperationException, InvalidTransactionException {
        int responseNum = ReqStatusNo.FAILURE.getStatusCode();
        String errMsg;
        try {
            Transaction transaction = transactnMap.get(tId);
            if (transaction == null) {
                throw new InvalidTransactionException("No transaction exists with transaction id [" + tId + "]");
            }

            ResourceManagerType resManType = requestForWhichRM(requestType);
            int opType = isReadOWrite(requestType);
            String itemKey = resourceItem.getKey();
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
            if (transaction.getDeleteSet().containsKey(itemKey) && !isAddOperation(requestType)) {
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

            String itemUniqueAttrib = resourceItem.getLocation();
            int itemCount = resourceItem.getCount();
            int itemPrice = resourceItem.getPrice();
            alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);

            if (isAddOperation(requestType)) {
                //TODO:: Check if item exists
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
                transaction.getWriteSet().put(itemKey, resManType);
                responseNum = ReqStatusNo.SUCCESS.getStatusCode();

            } else if (isDeleteOperation(requestType)) {
                //TODO:: Check if item has reservations
                if (alreadyAccessedItem.getReserved() == 0) {
                    itemSet.remove(itemKey);
                    transaction.getDeleteSet().put(itemKey, resManType);
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
                alreadyAccessedItem.setCount(itemCount + reservedByCustomer);

                resManType = itemToRMTypeMatcher(itemKey);
                if (resManType != null) {
                    transaction.getWriteSet().put(itemKey, resManType);
                } else {
                    errMsg = "UnReserve request for item [" + itemKey + "] cannot be completed since no matching RM";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
                responseNum = alreadyAccessedItem.getReserved();
            }


//            else if (isReserveRequest(requestType)) {
//                int currentCount = alreadyAccessedItem.getCount();
//                int currentReserved = alreadyAccessedItem.getReserved();
//
//                if (currentCount == 0) {
//                    Trace.warn("RM::reserveItem ( " + id + ", " + customerID + ", " + key + ", " + location + ") " +
//                            "failed--No more items");
//
//                }
//
//                alreadyAccessedItem.setCount(currentCount - 1);
//                alreadyAccessedItem.setReserved(currentReserved + 1);
//                responseNum = ReqStatusNo.SUCCESS.getStatusCode();
//            }

//            switch (requestType) {
//                case ADD_FLIGHT: {
//                    Flight newFlight = (Flight) resourceItem;
//                    HashMap<String, Flight> flightMap = transaction.getFlightSet();
//                    flightMap.put(newFlight.getKey(), newFlight);
//                    transaction.getWriteSet().put(newFlight.getKey(), resManType);
//                    responseNumber = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case ADD_CARS: {
//                    Car newCar = (Car) resourceItem;
//                    HashMap<String, Car> carMap = transaction.getCarSet();
//                    carMap.put(newCar.getKey(), newCar);
//                    transaction.getWriteSet().put(newCar.getKey(), resManType);
//                    responseNumber = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case ADD_ROOMS: {
//                    Hotel newHotel = (Hotel) resourceItem;
//                    HashMap<String, Hotel> hotelMap = transaction.getHotelSet();
//                    hotelMap.put(newHotel.getKey(), newHotel);
//                    transaction.getWriteSet().put(newHotel.getKey(), resManType);
//                    responseNumber = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case DELETE_FLIGHT: {
//                    //TODO:: Need to check if updates on a deleted Item
//                    Flight delFlight = (Flight) resourceItem;
////                    String flKey = delFlight.getKey();
////                    HashMap<String, Flight> existingFMap = transaction.getFlightSet();
////                    if (existingFMap.containsKey(flKey)) {
////                        existingFMap.remove(flKey);
////                    }
//                    if (itemSet.containsKey(itemKey)) {
//                        itemSet.remove(itemKey);
//                    }
//                    transaction.getDeleteSet().put(itemKey, resManType);
//                    responseString = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case DELETE_CARS: {
//                    Car delCar = (Car) resourceItem;
//                    String carKey = delCar.getKey();
//                    HashMap<String, Car> existingCarMap = transaction.getCarSet();
//                    if (existingCarMap.containsKey(carKey)) {
//                        existingCarMap.remove(carKey);
//                    }
//                    transaction.getDeleteSet().put(carKey, resManType);
//                    responseString = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case DELETE_ROOMS: {
//                    Hotel delHotel = (Hotel) resourceItem;
//                    String hotelKey = delHotel.getKey();
//                    HashMap<String, Hotel> existingHotelMap = transaction.getHotelSet();
//                    if (existingHotelMap.containsKey(hotelKey)) {
//                        existingHotelMap.remove(hotelKey);
//                    }
//                    transaction.getDeleteSet().put(hotelKey, resManType);
//                    responseString = ReqStatusNo.SUCCESS.getStatus();
//                    break;
//                }
//                case QUERY_FLIGHT: {
//                    //TODO::Check Abort Conditions and Failed Commits
//                    Flight flight = (Flight) itemSet.get(itemKey);
//                    responseNum = flight.getCount();

//                    String reqKey = flight.getKey();
//                    HashMap<String, Flight> flSet = transaction.getFlightSet();
//                    Flight alreadyReadFlight = flSet.get(reqKey);
//
////                    if (alreadyReadFlight == null || alreadyReadFlight.getCount() == VALUE_NOT_SET) {
//                    if (alreadyReadFlight == null) {
//                        int rmTID = flightManager.start();
////                        int flightNum = Integer.parseInt(flight.getLocation());
//                        alreadyReadFlight = (Flight) flightManager.getItem(rmTID, itemKey);
////                        int flightCount = flightManager.queryFlight(rmTID, flightNum);
////                        int reservedSeats = flightManager.getReserveCount(rmTID, Flight.getKey(flightNum));
//                        boolean commitStat = flightManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            errMsg = "TId [" + tId + "] Call to Flight ResourceManger to Query [Flight] data for " +
//                                    " item with key [" + itemKey + "] could not be committed.";
//                            throw new RMTransactionFailedException(errMsg);
//
//                        } else if (alreadyReadFlight == null) {
//                            errMsg = "TId [" + tId + "] Call to Flight ResourceManger to Query [Flight] with " +
//                                    "flightNum [" + itemKey + "] returned NULL.";
//                            throw new InvalidOperationException(errMsg);
//                        }
//
////                        if (alreadyReadFlight == null) {
////                            alreadyReadFlight = new Flight(flightNum, flightCount, VALUE_NOT_SET);
//////                            alreadyReadFlight.setReserved(reservedSeats);
////                        } else {
////                            alreadyReadFlight.setCount(flightCount);
////                        }
//                        flSet.put(alreadyReadFlight.getKey(), alreadyReadFlight);
//                    }
//                    responseNum = alreadyReadFlight.getCount();
//                    break;
//                }
//                case QUERY_CARS: {
//                    Car car = (Car) itemSet.get(itemKey);
//                    responseNum = car.getCount();

//                    String reqKey = car.getKey();
//                    HashMap<String, Car> carSet = transaction.getCarSet();
//                    Car alreadyReadCar = carSet.get(reqKey);
//
////                    if (alreadyReadCar == null || alreadyReadCar.getCount() == VALUE_NOT_SET) {
//                    if (alreadyReadCar == null) {
//                        int rmTID = carManager.start();
////                        String carLocation = car.getLocation();
////                        int carCount = carManager.queryCars(rmTID, carLocation);
//                        alreadyReadCar = (Car) carManager.getItem(rmTID, reqKey);
//                        boolean commitStat = carManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            errMsg = "TId [" + tId + "] Call to Car ResourceManger to Query [Car] data for " +
//                                    " item with key [" + itemKey + "] could not be committed.";
//                            throw new RMTransactionFailedException(errMsg);
//
//                        } else if (alreadyReadCar == null) {
//                            errMsg = "TId [" + tId + "] Call to Car ResourceManger to Query [Car] with " +
//                                    "location [" + itemKey + "] returned NULL.";
//                            throw new InvalidOperationException(errMsg);
//                        }
//
////                        if (alreadyReadCar == null) {
////                            alreadyReadCar = new Car(carLocation, carCount, VALUE_NOT_SET);
////                        } else {
////                            alreadyReadCar.setCount(carCount);
////                        }
//                        carSet.put(alreadyReadCar.getKey(), alreadyReadCar);
//                    }
//                    responseNum = alreadyReadCar.getCount();
//                    break;
//                }
//                case QUERY_ROOMS: {
//                    Hotel hotel = (Hotel) resourceItem;
//                    String reqKey = hotel.getKey();
//                    HashMap<String, Hotel> hotelSet = transaction.getHotelSet();
//                    Hotel alreadyReadHotel = hotelSet.get(reqKey);
//
////                    if (alreadyReadHotel == null || alreadyReadHotel.getCount() == VALUE_NOT_SET) {
//                    if (alreadyReadHotel == null) {
//                        int rmTID = hotelManager.start();
////                        String hotelLocation = hotel.getLocation();
////                        int roomCount = hotelManager.queryRooms(rmTID, hotelLocation);
//                        alreadyReadHotel = (Hotel) hotelManager.getItem(rmTID, reqKey);
//                        boolean commitStat = hotelManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            errMsg = "TId [" + tId + "] Call to Hotel ResourceManger to Query [Hotel] data for " +
//                                    " item with key [" + itemKey + "] could not be committed.";
//                            throw new RMTransactionFailedException(errMsg);
//
//                        } else if (alreadyReadHotel == null) {
//                            errMsg = "TId [" + tId + "] Call to Hotel ResourceManger to Query [Hotel] with " +
//                                    "location [" + itemKey + "] returned NULL.";
//                            throw new InvalidOperationException(errMsg);
//                        }
//
////                        if (alreadyReadHotel == null) {
////                            alreadyReadHotel = new Hotel(hotelLocation, roomCount, VALUE_NOT_SET);
////                        } else {
////                            alreadyReadHotel.setCount(roomCount);
////                        }
//                        hotelSet.put(alreadyReadHotel.getKey(), alreadyReadHotel);
//                    }
//                    responseNum = alreadyReadHotel.getCount();
//                    break;
//                }
//                case QUERY_FLIGHT_PRICE: {
//                    //TODO::Check Abort Conditions and Failed Commits
//                    Flight pFLight = (Flight) resourceItem;
//                    String reqKey = pFLight.getKey();
//                    HashMap<String, Flight> flSet = transaction.getFlightSet();
//                    Flight alreadyReadFlight = flSet.get(reqKey);
//
////                    if (alreadyReadFlight == null || alreadyReadFlight.getPrice() == VALUE_NOT_SET) {
//                    if (alreadyReadFlight == null) {
//                        int rmTID = flightManager.start();
////                        int flightNum = Integer.parseInt(pFLight.getLocation());
////                        int flightPrice = flightManager.queryFlightPrice(rmTID, flightNum);
//                        alreadyReadFlight = (Flight) flightManager.getItem(rmTID, reqKey);
//                        boolean commitStat = flightManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            errMsg = "TId [" + tId + "] Call to Flight ResourceManger to Query [Flight] data for " +
//                                    " item with key [" + itemKey + "] could not be committed.";
//                            throw new RMTransactionFailedException(errMsg);
//
//                        } else if (alreadyReadFlight == null) {
//                            errMsg = "TId [" + tId + "] Call to Flight ResourceManger to Query [Flight] with " +
//                                    "flightNum [" + itemKey + "] returned NULL.";
//                            throw new InvalidOperationException(errMsg);
//                        }
//
////                        if (alreadyReadFlight == null) {
////                            alreadyReadFlight = new Flight(flightNum, VALUE_NOT_SET, flightPrice);
////                        } else {
////                            alreadyReadFlight.setPrice(flightPrice);
////                        }
//                        flSet.put(alreadyReadFlight.getKey(), alreadyReadFlight);
//                    }
//                    responseNum = alreadyReadFlight.getPrice();
//                    break;
//                }
//                case QUERY_CAR_PRICE: {
//                    Car pCar = (Car) resourceItem;
//                    String reqKey = pCar.getKey();
//                    HashMap<String, Car> carSet = transaction.getCarSet();
//                    Car alreadyReadCar = carSet.get(reqKey);
//
//                    if (alreadyReadCar == null || alreadyReadCar.getPrice() == VALUE_NOT_SET) {
//                        int rmTID = carManager.start();
//                        String carLocation = pCar.getLocation();
//                        int carPrice = carManager.queryCarsPrice(rmTID, carLocation);
//                        boolean commitStat = carManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            String errMsg = "Call to Car ResourceManger to Query [PRICE] data on behalf of " +
//                                    "Transaction [" + tId + "] for item with key [" + itemKey + "] could not be " +
//                                    "committed.";
//                            throw new RMTransactionFailedException(errMsg);
//                        }
//
//                        if (alreadyReadCar == null) {
//                            alreadyReadCar = new Car(carLocation, VALUE_NOT_SET, carPrice);
//                        } else {
//                            alreadyReadCar.setPrice(carPrice);
//                        }
//                        carSet.put(alreadyReadCar.getKey(), alreadyReadCar);
//                    }
//                    responseNum = alreadyReadCar.getPrice();
//                    break;
//                }
//                case QUERY_ROOM_PRICE: {
//                    Hotel pHotel = (Hotel) resourceItem;
//                    String reqKey = pHotel.getKey();
//                    HashMap<String, Hotel> hotelSet = transaction.getHotelSet();
//                    Hotel alreadyReadHotel = hotelSet.get(reqKey);
//
//                    if (alreadyReadHotel == null || alreadyReadHotel.getPrice() == VALUE_NOT_SET) {
//                        int rmTID = hotelManager.start();
//                        String hotelLocation = pHotel.getLocation();
//                        int roomPrice = hotelManager.queryRoomsPrice(rmTID, hotelLocation);
//                        boolean commitStat = hotelManager.commit(rmTID);
//
//                        if (!commitStat) {
//                            String errMsg = "Call to Hotel ResourceManger to Query [PRICE] data on behalf of " +
//                                    "Transaction [" + tId + "] for item with key [" + itemKey + "] could not be " +
//                                    "committed.";
//                            throw new RMTransactionFailedException(errMsg);
//                        }
//
//                        if (alreadyReadHotel == null) {
//                            alreadyReadHotel = new Hotel(hotelLocation, VALUE_NOT_SET, roomPrice);
//                        } else {
//                            alreadyReadHotel.setPrice(roomPrice);
//                        }
//                        hotelSet.put(alreadyReadHotel.getKey(), alreadyReadHotel);
//                    }
//                    responseNum = alreadyReadHotel.getPrice();
//                    break;
//                }
//                case RESERVE_FLIGHT: {
//                    Flight flight = (Flight) resourceItem;
//                    String reqKey = flight.getKey();
//                    HashMap<String, Flight> flightSet = transaction.getFlightSet();
//                    Flight alreadyReadFlight = flightSet.get(reqKey);
//
////                    if () {
////                    }
//
//                    break;
//                }
//                case RESERVE_CAR:
//                    Car carToReserve = (Car) resourceItem;
//                    break;
//                case RESERVE_ROOM:
//                    Hotel hotelToReserve = (Hotel) resourceItem;
//                    break;
//            case RESERVE_ITINERARY: //TODO:: Need to check these methods
//                break;
//            case ROLLBACK_ITINERARY:
//                break;
//            case UNRESERVE_RESOURCE:
//                break;
//            }
//
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
            Transaction transaction = transactnMap.get(tId);
            if (transaction == null) {
                throw new InvalidTransactionException("No transaction exists with transaction id [" + tId + "]");
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
            if (transaction.getDeleteSet().containsKey(itemKey) && !isAddOperation(requestType)) {
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
                        transaction.getWriteSet().put(itemKey, resManType);
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
                        transaction.getWriteSet().put(itemKey, resManType);
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
                    transaction.getDeleteSet().put(itemKey, resManType);
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
    public int submitReserveOperation(int tId, int customerId, RequestType requestType, ReservableItem resourceItem)
            throws InvalidOperationException, InvalidTransactionException {
        int responseNum = ReqStatusNo.FAILURE.getStatusCode();
        String errMsg;
        try {
            Transaction transaction = transactnMap.get(tId);
            if (transaction == null) {
                throw new InvalidTransactionException("No transaction exists with transaction id [" + tId + "]");
            }

            String itemKey = resourceItem.getKey();
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
            if (transaction.getDeleteSet().containsKey(itemKey) && !isAddOperation(requestType)) {
                errMsg = "This item has already been deleted by an earlier operation of this transaction.";
                printMsg(errMsg);
                throw new InvalidOperationException(errMsg);
            }

            ConcurrentHashMap<String, RMItem> itemSet = transaction.getAccessedItemSet();
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

                if (alreadyAccessedItem != null) {
                    itemSet.put(alreadyAccessedItem.getKey(), alreadyAccessedItem);

                } else {
                    errMsg = "TId [" + tId + "] Call to ResourceManger to access [" + resManType.getCodeString() + "]" +
                            " with key [" + itemKey + "] returned NULL. Hence, cannot reserve non-existent item.";
                    printMsg(errMsg);
                    throw new InvalidOperationException(errMsg);
                }
            }

            alreadyAccessedItem = (ReservableItem) itemSet.get(itemKey);





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


    private ResourceManagerType itemToRMTypeMatcher(String itemKey) {
        ResourceManagerType matchingResourceMan = null;
        if (itemKey.contains(FLIGHT_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.FLIGHT;

        } else if (itemKey.contains(CAR_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.CAR;

        } else if (itemKey.contains(HOTEL_ITEM_KEY)) {
            matchingResourceMan = ResourceManagerType.HOTEL;

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

            //TODO:: This won't work
//        } else if (requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
//                requestType == RequestType.RESERVE_ROOM || requestType == RequestType.RESERVE_ITINERARY) {
//            matchingResourceMan = ResourceManagerType.RESERVE;
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
                requestType == RequestType.UNRESERVE_RESOURCE || requestType == RequestType.RESERVE_ITINERARY) {
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

    private boolean isReserveRequest(RequestType requestType) {
        return requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
                requestType == RequestType.RESERVE_ROOM;
    }

    private boolean readRequiredOperation(RequestType requestType) {
        return requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE ||
                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE ||
                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE ||
                requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
                requestType == RequestType.RESERVE_ROOM || requestType == RequestType.RESERVE_ITINERARY ||
                requestType == RequestType.QUERY_CUSTOMER_INFO;
    }

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
