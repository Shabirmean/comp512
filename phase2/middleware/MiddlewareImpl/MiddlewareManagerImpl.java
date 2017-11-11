package MiddlewareImpl;

import MiddlewareInterface.*;
import ResImpl.*;
import ResInterface.*;
import exception.InvalidOperationException;
import Transaction.TransactionManager;
import util.RequestType;
import util.ResourceManagerType;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddlewareManagerImpl implements Middleware {

    protected RMHashtable m_itemHT = new RMHashtable();
    static TransactionManager transactionMan;
    static ResourceManager cm = null;
    static ResourceManager hm = null;
    static ResourceManager fm = null;

    public MiddlewareManagerImpl() throws RemoteException {
    }

    public static void main(String args[]) {
        // Figure out where server is running
        String server = "localhost";
        String carserver = "";
        String flightserver = "";
        String hotelserver = "";
        int port = 1099;
        int carport = 1100;
        int hotelport = 1101;
        int flightport = 1102;

        if (args.length == 1) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
        } else if (args.length == 4) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
            carserver = args[1];
            hotelserver = args[2];
            flightserver = args[3];
        }

        // else if (args.length != 0 &&  args.length != 1) {
        //     System.err.println ("Wrong usage");
        //     System.out.println("Usage: java ResImpl.ResourceManagerImpl [port]");
        //     System.exit(1);
        // }

        try {
            // create a new Server object
            MiddlewareManagerImpl obj = new MiddlewareManagerImpl();
            // dynamically generate the stub (client proxy)
            Middleware rm = (Middleware) UnicastRemoteObject.exportObject(obj, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("ShabirJianMiddleware", rm);

            System.err.println("Server ready");
            server = "localhost";

            Registry carregistry = LocateRegistry.getRegistry(carserver, carport);
            cm = (ResourceManager) carregistry.lookup("ShabirJianResourceManager");
            if (cm != null) {
                System.out.println("Successful");
                System.out.println("Connected to CarManager");
            } else {
                System.out.println("Unsuccessful");
            }

            Registry hotelregistry = LocateRegistry.getRegistry(hotelserver, hotelport);
            hm = (ResourceManager) hotelregistry.lookup("ShabirJianResourceManager");
            if (hm != null) {
                System.out.println("Successful");
                System.out.println("Connected to HotelManager");
            } else {
                System.out.println("Unsuccessful");
            }

            Registry flightregistry = LocateRegistry.getRegistry(flightserver, flightport);
            fm = (ResourceManager) flightregistry.lookup("ShabirJianResourceManager");
            if (fm != null) {
                System.out.println("Successful");
                System.out.println("Connected to FlightManager");
            } else {
                System.out.println("Unsuccessful");
            }

            MWResourceManager mw = new MWResourceManager();
            transactionMan = new TransactionManager(cm, hm, fm, mw);
            transactionMan.initTransactionManager();

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    @Override
    public int start() throws RemoteException {
        return transactionMan.start();
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException {
        try {
            return transactionMan.commit(transactionId);
        } catch (InvalidOperationException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        transactionMan.abort(transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        return false;
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
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }


    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        Flight flightItem = new Flight(flightNum, -1, -1);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_FLIGHT, flightItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }


    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price) throws RemoteException {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
        Hotel hotelItem = new Hotel(location, count, price);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.ADD_ROOMS, hotelItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location) throws RemoteException {
        Hotel hotelItem = new Hotel(location, -1, -1);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_ROOMS, hotelItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
            throws RemoteException {
        Car carItem = new Car(location, count, price);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.ADD_CARS, carItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException {
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        int tManResponse;
        try {
            tManResponse = transactionMan.submitOperation(id, RequestType.DELETE_CARS, carItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }


    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum) throws RemoteException {
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_FLIGHT, flightItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
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
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_FLIGHT_PRICE, flightItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location) throws RemoteException {
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_ROOMS, hotelItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
    }


    // Returns room price at this location
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_ROOM_PRICE, hotelItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location) throws RemoteException {
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_CARS, carItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location) throws RemoteException {
        Car carItem = new Car(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            return transactionMan.submitOperation(id, RequestType.QUERY_CAR_PRICE, carItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
    }


    //TODO:: Handle Cutomer Related Requests
    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
            throws RemoteException {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't " +
                    "exist");
            return null;
        } else {
            return cust.getReservations();
        } // if
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        try {
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
            Customer customer = new Customer(customerID);
            return transactionMan.submitOperation(id, RequestType.QUERY_CUSTOMER_INFO, customer);
        } catch (InvalidTransactionException | InvalidOperationException e) {
            throw new RemoteException(e.getMessage());
        }
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
        } catch (InvalidTransactionException | InvalidOperationException e) {
            throw new RemoteException(e.getMessage());
        }

        if (response.equals(TransactionManager.ReqStatusNo.SUCCESS.getStatus())){
            Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
            returnId = cid;
        } else {
            Trace.info("RM::newCustomer(" + cid + ") there was some error creating customer");
            returnId = TransactionManager.VALUE_NOT_SET;
        }
        return returnId;
    }

    public boolean newCustomer(int id, int customerID) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
        Customer customer = new Customer(customerID);
        String response;

        try {
            response = transactionMan.submitOperation(id, RequestType.ADD_NEW_CUSTOMER_WITH_ID, customer);
        } catch (InvalidTransactionException | InvalidOperationException e) {
            throw new RemoteException(e.getMessage());
        }
        return response.equals(TransactionManager.ReqStatusNo.SUCCESS.getStatus());
    }


    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        Customer customer = new Customer(customerID);
        String response;
        try {
            response = transactionMan.submitOperation(id, RequestType.DELETE_CUSTOMER, customer);
        } catch (InvalidTransactionException | InvalidOperationException e) {
            throw new RemoteException(e.getMessage());
        }
        return response.equals(TransactionManager.ReqStatusNo.SUCCESS.getStatus());
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
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }

    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        int tManResponse;
        Customer customer = new Customer(customerID);
        Hotel hotelItem = new Hotel(location, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManResponse = transactionMan.submitReserveOperation(id, customer, RequestType.RESERVE_RESOURCE, hotelItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManResponse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException {
        int tManRspnse;
        Customer customer = new Customer(customerID);
        Flight flightItem = new Flight(flightNum, TransactionManager.VALUE_NOT_SET, TransactionManager.VALUE_NOT_SET);
        try {
            tManRspnse = transactionMan.submitReserveOperation(id, customer, RequestType.RESERVE_RESOURCE, flightItem);
        } catch (InvalidOperationException | InvalidTransactionException e) {
            throw new RemoteException(e.getMessage());
        }
        return tManRspnse == TransactionManager.ReqStatusNo.SUCCESS.getStatusCode();
    }

    // Reserve an itinerary
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        return transactionMan.submitReserveItineraryOperation(id, customer, flightNumbers, location, car, room);
    }


}