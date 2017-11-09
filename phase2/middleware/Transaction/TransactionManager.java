package Transaction;

import LockManager.LockManager;
import LockManager.DeadlockException;
import MiddlewareImpl.*;
import ResInterface.InvalidTransactionException;
import ResInterface.ResourceManager;
import ResInterface.TransactionAbortedException;
import util.RequestType;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    static LockManager lockMan = new LockManager();
    static ConcurrentHashMap<Integer, Transaction> transactionMap = new ConcurrentHashMap<Integer, Transaction>();
    ResourceManager carManager;
    ResourceManager hotelManager;
    ResourceManager flightManager;


    public TransactionManager(ResourceManager cm, ResourceManager hm, ResourceManager fm) {
        this.carManager = cm;
        this.hotelManager = hm;
        this.flightManager = fm;
    }


    public int start() throws RemoteException {
        UUID uuid = UUID.randomUUID();
        int newTId = uuid.hashCode();
        Transaction newTrans = new Transaction(newTId);
        transactionMap.put(newTId, newTrans);
        return newTId;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException {
        return true;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {

    }


    public int submitOperation(int tId, RequestType requestType, ReservableItem resourceItem)
            throws InvalidOperationException, InvalidTransactionException {
        int responseNumber = ReqStatusNo.FAILURE.getStatusNo();
        try {
            Transaction transaction = transactionMap.get(tId);
            if (transaction == null) {
                throw new InvalidTransactionException("No transaction exists with transaction id [" + tId + "]");
            }

            ResourceManagerType resManType = requestForWhichRM(requestType);
            int opType = isReadOWrite(requestType);
            String itemKey = resourceItem.getKey();
            boolean tryLock = lockMan.Lock(tId, itemKey, opType);

            if (!tryLock) {
                String errMsg;
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

            switch (requestType) {
                case ADD_FLIGHT:
                    Flight newFlight = (Flight) resourceItem;
                    HashMap<String, Flight> flightMap = transaction.getFlightSet();
                    flightMap.put(newFlight.getKey(), newFlight);
                    transaction.getWriteSet().put(newFlight.getKey(), resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case ADD_CARS:
                    Car newCar = (Car) resourceItem;
                    HashMap<String, Car> carMap = transaction.getCarSet();
                    carMap.put(newCar.getKey(), newCar);
                    transaction.getWriteSet().put(newCar.getKey(), resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case ADD_ROOMS:
                    Hotel newHotel = (Hotel) resourceItem;
                    HashMap<String, Hotel> hotelMap = transaction.getHotelSet();
                    hotelMap.put(newHotel.getKey(), newHotel);
                    transaction.getWriteSet().put(newHotel.getKey(), resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case DELETE_FLIGHT:
                    Flight delFlight = (Flight) resourceItem;
                    String flKey = delFlight.getKey();
                    HashMap<String, Flight> existingFMap = transaction.getFlightSet();
                    if (existingFMap.containsKey(flKey)) {
                        existingFMap.remove(flKey);
                    }
                    transaction.getDeleteSet().put(flKey, resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case DELETE_CARS:
                    Car delCar = (Car) resourceItem;
                    String carKey = delCar.getKey();
                    HashMap<String, Car> existingCarMap = transaction.getCarSet();
                    if (existingCarMap.containsKey(carKey)) {
                        existingCarMap.remove(carKey);
                    }
                    transaction.getDeleteSet().put(carKey, resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case DELETE_ROOMS:
                    Hotel delHotel = (Hotel) resourceItem;
                    String hotelKey = delHotel.getKey();
                    HashMap<String, Hotel> existingHotelMap = transaction.getHotelSet();
                    if (existingHotelMap.containsKey(hotelKey)) {
                        existingHotelMap.remove(hotelKey);
                    }
                    transaction.getDeleteSet().put(hotelKey, resManType);
                    responseNumber = ReqStatusNo.SUCCESS.getStatusNo();
                    break;

                case QUERY_FLIGHT:
                    //TODO::Check Abort Conditions and Failed Commits
                    Flight flight = (Flight) resourceItem;
                    String reqKey = flight.getKey();
                    HashMap<String, Flight> flSet = transaction.getFlightSet();
                    Flight alreadyReadFlight = flSet.get(reqKey);
                    if (alreadyReadFlight == null || alreadyReadFlight.getCount() == -1) {

                        int rmTID = flightManager.start();
                        int flightNum = Integer.parseInt(flight.getLocation());
                        int flightCount = flightManager.queryFlight(rmTID, flightNum);

                        boolean commitStat = flightManager.commit(rmTID);
                        if (!commitStat) {
                            String errMsg = "Call to Flight ResourceManger to Query data on behave of Transaction " +
                                    "[" + tId + "] for item with key [" + itemKey + "] could not commit.";
                            throw new RMTransactionFailedException(errMsg);
                        }

                        if (alreadyReadFlight == null) {
                            alreadyReadFlight = new Flight(flightNum, flightCount, -1);
                        } else {
                            alreadyReadFlight.setCount(flightCount);
                        }
                        flSet.put(alreadyReadFlight.getKey(), alreadyReadFlight);
                    }
                    responseNumber = alreadyReadFlight.getCount();

                case QUERY_CARS:
                    Car car = (Car) resourceItem;
                    break;
                case QUERY_ROOMS:
                    Hotel hotel = (Hotel) resourceItem;
                    break;
                case QUERY_FLIGHT_PRICE:
                    Flight pFLight = (Flight) resourceItem;
                    break;
                case QUERY_CAR_PRICE:
                    Car pCar = (Car) resourceItem;
                    break;
                case QUERY_ROOM_PRICE:
                    Hotel pHotel = (Hotel) resourceItem;
                    break;
                case RESERVE_FLIGHT:
                    Flight flightToReserve = (Flight) resourceItem;
                    break;
                case RESERVE_CAR:
                    Car carToReserve = (Car) resourceItem;
                    break;
                case RESERVE_ROOM:
                    Hotel hotelToReserve = (Hotel) resourceItem;
                    break;
//            case RESERVE_ITINERARY: //TODO:: Need to check these methods
//                break;
//            case ROLLBACK_ITINERARY:
//                break;
//            case UNRESERVE_RESOURCE:
//                break;
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
        return responseNumber; //TODO:: NEED TO CHANGE THIS
    }


    void submitOperation(RequestType requestType, Customer customerItem) {
        switch (requestType) {
            case ADD_NEW_CUSTOMER_WITHOUT_ID:
                break;
            case ADD_NEW_CUSTOMER_WITH_ID:
                break;
            case DELETE_CUSTOMER:
                break;
            case QUERY_CUSTOMER_INFO:
                break;
        }
    }


    ResourceManagerType requestForWhichRM(RequestType requestType) {
        ResourceManagerType matchingResourceMan = null;
//        if (requestType == RequestType.UNRESERVE_RESOURCE || requestType == RequestType.ROLLBACK_ITINERARY ||
//                requestType == RequestType.RESERVE_ITINERARY) {
//            return true;
//        }

        if (requestType == RequestType.ADD_FLIGHT || requestType == RequestType.DELETE_FLIGHT ||
                requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE ||
                requestType == RequestType.RESERVE_FLIGHT) {
            matchingResourceMan = ResourceManagerType.FLIGHT;

        } else if (requestType == RequestType.ADD_CARS || requestType == RequestType.DELETE_CARS ||
                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE ||
                requestType == RequestType.RESERVE_CAR) {
            matchingResourceMan = ResourceManagerType.CAR;

        } else if (requestType == RequestType.ADD_ROOMS || requestType == RequestType.DELETE_ROOMS ||
                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE ||
                requestType == RequestType.RESERVE_ROOM) {
            matchingResourceMan = ResourceManagerType.HOTEL;
        }
        return matchingResourceMan;
    }


    int isReadOWrite(RequestType requestType) {
        int readOWrite = -1; //TODO:: Need to add for reserveItinerary

        if (requestType == RequestType.ADD_FLIGHT || requestType == RequestType.DELETE_FLIGHT ||
                requestType == RequestType.ADD_CARS || requestType == RequestType.DELETE_CARS ||
                requestType == RequestType.ADD_ROOMS || requestType == RequestType.DELETE_ROOMS ||
                requestType == RequestType.RESERVE_FLIGHT || requestType == RequestType.RESERVE_CAR ||
                requestType == RequestType.RESERVE_ROOM || requestType == RequestType.ADD_NEW_CUSTOMER_WITH_ID ||
                requestType == RequestType.ADD_NEW_CUSTOMER_WITHOUT_ID || requestType == RequestType.DELETE_CUSTOMER) {
            readOWrite = LockManager.WRITE;

        } else if (requestType == RequestType.QUERY_FLIGHT || requestType == RequestType.QUERY_FLIGHT_PRICE ||
                requestType == RequestType.QUERY_CARS || requestType == RequestType.QUERY_CAR_PRICE ||
                requestType == RequestType.QUERY_ROOMS || requestType == RequestType.QUERY_ROOM_PRICE ||
                requestType == RequestType.QUERY_CUSTOMER_INFO) {
            readOWrite = LockManager.READ;

        }
        return readOWrite;
    }

    void printMsg(String msg) {
        System.out.println("[Transaction Manager] " + msg);
    }


    public enum ReqStatusNo {
        SUCCESS(0),
        FAILURE(-1);

        private final int statusNo;

        ReqStatusNo(int statusNo) {
            this.statusNo = statusNo;
        }

        public int getStatusNo() {
            return this.statusNo;
        }

    }
}
