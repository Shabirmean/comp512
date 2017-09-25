package socs.distributed.middleware.server;


import socs.distributed.resource.dto.*;
import socs.distributed.resource.entity.Customer;
import socs.distributed.resource.util.Trace;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;

@SuppressWarnings("ALL")
public class CustomerResourceManager {
    private static final RMConcurrentHashMap RM_ITEMS_MAP = new RMConcurrentHashMap();

    // Reads a data item
    private RMItem readData(int id, String key) {
        return (RMItem) RM_ITEMS_MAP.get(key);
    }

    // Writes a data item
    private void writeData(int id, String key, RMItem value) {
        RM_ITEMS_MAP.put(key, value);
    }

    // Remove the item out of storage
    private RMItem removeData(int id, String key) {
        return (RMItem) RM_ITEMS_MAP.remove(key);
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

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id) {
        Trace.info("INFO: RM::newCustomer(" + id + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(cid);
        writeData(id, cust.getKey(), cust);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID) {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            cust = new Customer(customerID);
            writeData(id, cust.getKey(), cust);
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
            return false;
        } // else
    }


    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID) {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMConcurrentHashMap reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements(); ) {
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
                        + " " + reserveditem.getCount() + " times");
                ReservableItem item = (ReservableItem) readData(id, reserveditem.getKey());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
                        + "which is reserved" + item.getReserved() + " times and is still available " + item.getCount
                        () + " times");
                item.setReserved(item.getReserved() - reserveditem.getCount());
                item.setCount(item.getCount() + reserveditem.getCount());
            }

            // remove the customer from the storage
            removeData(id, cust.getKey());

            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
            return true;
        } // if
    }


    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMConcurrentHashMap getCustomerReservations(int id, int customerID) {
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
    public String queryCustomerInfo(int id, int customerID) {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows...");
            System.out.println(s);
            return s;
        } // if
    }

    // Reserve an itinerary
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room) {
        return false;
    }

}
