package MiddlewareImpl;

import MiddlewareInterface.Middleware;
import Replication.ReplicationObject;
import ResImpl.*;
import ResInterface.InvalidTransactionException;
import ResInterface.TransactionAbortedException;
import Transaction.ReqStatus;
import Transaction.TransactionManager;
import exception.TransactionManagerException;
import util.RequestType;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Vector;

public class MiddlewareManagerImpl implements Middleware {

    private RMHashtable m_itemHT = new RMHashtable();
    private TransactionManager transactionMan;

    public MiddlewareManagerImpl(TransactionManager tManger) {
        transactionMan = tManger;
    }

    public TransactionManager getTransactionMan() {
        return transactionMan;
    }

//    public void setTransactionMan(TransactionManager transactionMan) {
//        this.transactionMan = transactionMan;
//    }

    public void setMWState(ReplicationObject replicationObject) {
        this.transactionMan.updateState(replicationObject);
    }

    public ReplicationObject getMWState() {
        return transactionMan.getReplicationObject();
    }

//    public static void main(String args[]) {
//        // Figure out where server is running
//        String server = "localhost";
//        String carserver = "";
//        String flightserver = "";
//        String hotelserver = "";
//        int port = 1099;
//        int carport = 1100;
//        int hotelport = 1101;
//        int flightport = 1102;
//
//        if (args.length == 1) {
//            server = server + ":" + args[0];
//            port = Integer.parseInt(args[0]);
//        } else if (args.length == 4) {
//            server = args[0];
//            carserver = args[1];
//            flightserver = args[2];
//            hotelserver = args[3];
//        }
//
//        String middlewareServerConfig = server + ":" + port;
//        String carServerConfig = carserver + ":" + carport;
//        String flightServerConfig = flightserver + ":" + flightport;
//        String hotelServerConfig = hotelserver + ":" + hotelport;
//
//        System.out.println("Middleware: " + middlewareServerConfig);
//        System.out.println("Car Manager: " + carServerConfig);
//        System.out.println("Hotel Manager: " + hotelServerConfig);
//        System.out.println("Flight Manager: " + flightServerConfig);
//
//        HashMap<String, String> rmConfigs = new HashMap<>();
//        rmConfigs.put(ResourceManagerType.CUSTOMER.getCodeString(), middlewareServerConfig);
//        rmConfigs.put(ResourceManagerType.CAR.getCodeString(), carServerConfig);
//        rmConfigs.put(ResourceManagerType.FLIGHT.getCodeString(), flightServerConfig);
//        rmConfigs.put(ResourceManagerType.HOTEL.getCodeString(), hotelServerConfig);
//
//        final String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
//        System.out.println("MW::Random UUID-" + uuid);
//        MWReplicationManager mwClusterMember = new MWReplicationManager(uuid, server);
//        try {
////            String memberId = mwClusterMember.start();
//            mwClusterMember.start();
//            mwClusterMember.connectToReplicationServers(rmConfigs);
//            transactionMan = rmClusterManager.transactionMan;
////            rmClusterManager rmClusterManager = new rmClusterManager(rmConfigs, isClusterManager);
////            HashMap<ResourceManagerType, ResourceManager> resourceManagers = rmClusterManager
//// .connectToReplicationServers();
////            transactionMan = new TransactionManager(resourceManagers);
////            transactionMan.initTransactionManager();
//
//        } catch (Exception e) {
//            //TODO:: Handle this please
//            e.printStackTrace();
//        }
//    }

    @Override
    public int start() throws RemoteException {
        return transactionMan.start();
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException {
        try {
            return transactionMan.commit(transactionId);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new TransactionAbortedException(e.getReason().getStatus());
        }
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        try {
            transactionMan.abort(transactionId);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new InvalidTransactionException(e.getReason().getStatus());
        }
    }

    @Override
    public boolean shutdown() throws RemoteException {
        try {
            return transactionMan.shutdown();
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
    }

    // Reads a data item
    private RMItem readData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Writes a data item
    private void writeData(int id, String key, RMItem value) {
        synchronized (m_itemHT) {
            m_itemHT.put(key, value);
        }
    }

    // Remove the item out of storage
    protected RMItem removeData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.remove(key);
        }
    }

    // deletes the entire item
    protected boolean deleteItem(int id, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers " +
                        "reserved it");
                return false;
            }
        } // if
    }


    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        } // else
        Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
        return value;
    }

    // query the price of an item
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
        Flight flightItem = new Flight(flightNum, flightSeats, flightPrice);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.ADD_FLIGHT, flightItem);
        } catch (TransactionManagerException e) {
            e.printStackTrace();
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }


    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        Flight flightItem = new Flight(flightNum, -1, -1);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_FLIGHT, flightItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }


    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price) throws RemoteException {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
        Hotel hotelItem = new Hotel(location, count, price);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.ADD_ROOMS, hotelItem);
        } catch (TransactionManagerException e) {
            e.printStackTrace();
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location) throws RemoteException {
        Hotel hotelItem = new Hotel(location, -1, -1);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_ROOMS, hotelItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
            throws RemoteException {
        Car carItem = new Car(location, count, price);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.ADD_CARS, carItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException {
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_CARS, carItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }


    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum) throws RemoteException {
        int tManResponse;
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_FLIGHT, flightItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }

    // Returns the number of reservations for this flight.
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }


    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum) throws RemoteException {
        int tManResponse;
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_FLIGHT_PRICE, flightItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location) throws RemoteException {
        int tManResponse;
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_ROOMS, hotelItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }


    // Returns room price at this location
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        int tManResponse;
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_ROOM_PRICE, hotelItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location) throws RemoteException {
        int tManResponse;
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_CARS, carItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location) throws RemoteException {
        int tManResponse;
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.QUERY_CAR_PRICE, carItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse;
    }


    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't " +
                    "exist");
            return null;
        } else {
            System.out.println("");
            return cust.getReservations();
        } // if
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        String response;
        try {
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
            Customer customer = new Customer(customerID);
            response = transactionMan.submitOperation(id, RequestType.QUERY_CUSTOMER_INFO, customer);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return response;
    }

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id) throws RemoteException {
        int returnId;
        String response;
        Trace.info("INFO: RM::newCustomer(" + id + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        try {
            Customer customer = new Customer(cid);
            response = transactionMan.submitOperation(id, RequestType.ADD_NEW_CUSTOMER_WITH_ID, customer);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }

        if (response.equals(ReqStatus.SUCCESS.getStatus())) {
            Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
            returnId = cid;
        } else {
            Trace.info("RM::newCustomer(" + cid + ") there was some error creating customer");
            returnId = TransactionManager.VALUE_NOT_SET;
        }
        System.out.println("");
        return returnId;
    }

    public boolean newCustomer(int id, int customerID) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
        Customer customer = new Customer(customerID);
        String response;

        try {
            response = transactionMan.submitOperation(id, RequestType.ADD_NEW_CUSTOMER_WITH_ID, customer);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return response.equals(ReqStatus.SUCCESS.getStatus());
    }


    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        Customer customer = new Customer(customerID);
        String response;
        try {
            response = transactionMan.submitOperation(id, RequestType.DELETE_CUSTOMER, customer);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return response.equals(ReqStatus.SUCCESS.getStatus());
    }



    /*
    // Frees flight reservation record. Flight reservation records help us make sure we
    // don't delete a flight if one or more customers are holding reservations
    public boolean freeFlightReservation(int id, int flightNum)
        throws RemoteException
    {
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
        if ( numReservations != null ) {
            numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
        } // if
        writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
                + numReservations + " reservations" );
        return true;
    }
    */

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        int tManResponse;
        Customer customer = new Customer(customerID);
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitReserveOperation(id, customer, RequestType.RESERVE_RESOURCE, carItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }

    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        int tManResponse;
        Customer customer = new Customer(customerID);
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitReserveOperation(id, customer, RequestType.RESERVE_RESOURCE, hotelItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManResponse == ReqStatus.SUCCESS.getStatusCode();
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException {
        int tManRspnse;
        Customer customer = new Customer(customerID);
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManRspnse = transactionMan.submitReserveOperation(id, customer, RequestType.RESERVE_RESOURCE, flightItem);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return tManRspnse == ReqStatus.SUCCESS.getStatusCode();
    }

    // Reserve an itinerary
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        boolean itinStat = false;
        try {
            itinStat = transactionMan.submitReserveItineraryOperation(id, customer, flightNumbers, location, car, room);
        } catch (TransactionManagerException e) {
            System.out.println("");
            throw new RemoteException(e.getReason().getStatus());
        }
        System.out.println("");
        return itinStat;
    }


}