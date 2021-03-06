// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package MidImpl;

import MidInterface.*;
import ResInterface.*;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MidWareImpl implements MidWare {

    protected RMHashtable m_itemHT = new RMHashtable();
    static ResourceManager cm = null;
    static ResourceManager hm = null;
    static ResourceManager fm = null;


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
            MidWareImpl obj = new MidWareImpl();
            // dynamically generate the stub (client proxy)
            MidWare rm = (MidWare) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("GroupOneMiddleware", rm);

            System.err.println("Server ready");

            server = "localhost";

            Registry carregistry = LocateRegistry.getRegistry(carserver, carport);
            cm = (ResourceManager) carregistry.lookup("GroupOneResourceManager");
            if (cm != null) {
                System.out.println("Successful");
                System.out.println("Connected to CarManager");
            } else {
                System.out.println("Unsuccessful");
            }

            Registry hotelregistry = LocateRegistry.getRegistry(hotelserver, hotelport);
            hm = (ResourceManager) hotelregistry.lookup("GroupOneResourceManager");
            if (hm != null) {
                System.out.println("Successful");
                System.out.println("Connected to HotelManager");
            } else {
                System.out.println("Unsuccessful");
            }

            Registry flightregistry = LocateRegistry.getRegistry(flightserver, flightport);
            fm = (ResourceManager) flightregistry.lookup("GroupOneResourceManager");
            if (fm != null) {
                System.out.println("Successful");
                System.out.println("Connected to FlightManager");
            } else {
                System.out.println("Unsuccessful");
            }


        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    public MidWareImpl() throws RemoteException {
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

    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key, String location) {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " + key + ", " + location + " ) called");
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", " + location + ")  " +
                    "failed--customer doesn't exist");
            return false;
        }

        // check if the item is available
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") failed--item " +
                    "doesn't exist");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") failed--No " +
                    "more items");
            return false;
        } else {
            cust.reserve(key, location, item.getPrice());
            writeData(id, cust.getKey(), cust);

            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);

            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
        if (fm.addFlight(id, flightNum, flightSeats, flightPrice)) return true;
        else return false;
    }


    public boolean deleteFlight(int id, int flightNum)
            throws RemoteException {
        if (fm.deleteFlight(id, flightNum)) return true;
        else return false;
    }


    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
            throws RemoteException {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
        if (hm.addRooms(id, location, count, price)) return true;
        else return false;
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
            throws RemoteException {
        return hm.deleteRooms(id, location);

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
            throws RemoteException {
        // Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        if (cm.addCars(id, location, count, price)) return true;
        else return false;
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
            throws RemoteException {
        if (cm.deleteCars(id, location)) return true;
        else return false;
    }


    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
            throws RemoteException {
        return fm.queryFlight(id, flightNum);
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
    public int queryFlightPrice(int id, int flightNum)
            throws RemoteException {
        return fm.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
            throws RemoteException {
        return hm.queryRooms(id, location);
    }


    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
            throws RemoteException {
        return hm.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
            throws RemoteException {
        return cm.queryCars(id, location);
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
            throws RemoteException {
        return cm.queryCarsPrice(id, location);
    }

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
    public String queryCustomerInfo(int id, int customerID)
            throws RemoteException {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
        String fbill = fm.queryCustomerInfo(id, customerID);
        String hbill = hm.queryCustomerInfo(id, customerID);
        String cbill = cm.queryCustomerInfo(id, customerID);
        return " Bill for customer " + customerID + ":\n" + fbill + hbill + cbill;
    }

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ") called");
        int cid = cm.newCustomer(id);
        hm.newCustomer(id, cid);
        fm.newCustomer(id, cid);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
        cm.newCustomer(id, customerID);
        hm.newCustomer(id, customerID);
        fm.newCustomer(id, customerID);
        return true;
    }


    // Deletes customer from the database. 
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        cm.deleteCustomer(id, customerID);
        hm.deleteCustomer(id, customerID);
        fm.deleteCustomer(id, customerID);
        return true;
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
        if (cm.reserveCar(id, customerID, location)) return true;
        else return false;
    }


    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        if (hm.reserveRoom(id, customerID, location)) return true;
        else return false;
    }


    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException {
        if (fm.reserveFlight(id, customerID, flightNum)) return true;
        else return false;
    }

    // Reserve an itinerary 
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room)
            throws RemoteException {

        for (Object o : flightNumbers) {
            fm.reserveFlight(id, customer, Integer.parseInt((String) o));
        }
        if (Car) cm.reserveCar(id, customer, location);
        if (Room) hm.reserveRoom(id, customer, location);
        return true;
    }

}
