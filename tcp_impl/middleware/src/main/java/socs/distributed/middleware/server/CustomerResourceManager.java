package socs.distributed.middleware.server;


import socs.distributed.resource.dto.*;
import socs.distributed.resource.entity.Customer;
import socs.distributed.resource.util.Trace;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;

@SuppressWarnings("ALL")
public class CustomerResourceManager {
    private static final RMConcurrentHashMap CUSTOMER_RM_ITEMS_MAP = new RMConcurrentHashMap();

    // Reads a data item
    private RMItem readData(int id, String key) {
        return (RMItem) CUSTOMER_RM_ITEMS_MAP.get(key);
    }

    // Writes a data item
    private void writeData(int id, String key, RMItem value) {
        CUSTOMER_RM_ITEMS_MAP.put(key, value);
    }

    // Remove the item out of storage
    private RMItem removeData(int id, String key) {
        return (RMItem) CUSTOMER_RM_ITEMS_MAP.remove(key);
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
    public RMConcurrentHashMap deleteCustomer(int id, int customerID) {
        RMConcurrentHashMap reservationHT = null;
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist");
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            //TODO:: Increase reservations
            reservationHT = cust.getReservations();
//            for (Enumeration e = reservationHT.keys(); e.hasMoreElements(); ) {
//                String reservedkey = (String) (e.nextElement());
//                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
//                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
//                        + " " + reserveditem.getCount() + " times");
//                ReservableItem item = (ReservableItem) readData(id, reserveditem.getKey());
//                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
//                        + "which is reserved" + item.getReserved() + " times and is still available " + item.getCount
//                        () + " times");
//                item.setReserved(item.getReserved() - reserveditem.getCount());
//                item.setCount(item.getCount() + reserveditem.getCount());
//            }

            // remove the customer from the storage
            removeData(id, cust.getKey());
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
        } // if
        return reservationHT;
    }


    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMConcurrentHashMap if customer exists but has no
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
            return null;   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows...");
            System.out.println(s);
            return s;
        } // if
    }


//    // Adds car reservation to this customer.
//    boolean reserveCar(int id, int customerID, String location) {
//        return reserveItem(id, customerID, Car.getKey(location), location);
//    }
//
//    // Adds room reservation to this customer.
//    boolean reserveRoom(int id, int customerID, String location) {
//        return reserveItem(id, customerID, Hotel.getKey(location), location);
//    }
//
//    // Adds flight reservation to this customer.
//    boolean reserveFlight(int id, int customerID, int flightNum) {
//        return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
//    }

    // Reserve an itinerary
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room) {
        return false;
    }

    public Customer isValidCustomer(int id, int customerId) {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerId + " ) called");
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) readData(id, Customer.getKey(customerId));
        if (customer == null) {
            Trace.warn("RM::ReserveItem( " + id + ", " + customerId + ") failed--customer doesn't exist");
            return null;
        }
        return customer;
    }

    // reserve an item
    protected boolean reserveItem(int id, Customer customer) {
        writeData(id, customer.getKey(), customer);
        Trace.info("RM::ReserveItem(" + id + ", Customer Key: " + customer.getKey() + ") succeeded");
        return true;
    }
}
