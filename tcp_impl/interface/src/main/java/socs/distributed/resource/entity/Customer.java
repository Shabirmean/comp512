// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------
package socs.distributed.resource.entity;


import org.apache.log4j.Logger;
import socs.distributed.resource.dto.RMConcurrentHashMap;
import socs.distributed.resource.dto.RMItem;
import socs.distributed.resource.dto.ReservedItem;

import java.io.Serializable;
import java.util.Enumeration;

public class Customer extends RMItem implements Serializable {
    private static final Logger log = Logger.getLogger(Customer.class);
    private int m_nID;
    private RMConcurrentHashMap m_Reservations;

    public Customer(int id) {
        super();
        m_Reservations = new RMConcurrentHashMap();
        m_nID = id;
    }

    public void setID(int id) {
        m_nID = id;
    }

    private int getID() {
        return m_nID;
    }

    public void reserve(String key, String location, int price) {
        ReservedItem reservedItem = getReservedItem(key);
        if (reservedItem == null) {
            // customer doesn't already have a reservation for this resource, so create a new one now
            reservedItem = new ReservedItem(key, location, 1, price);
        } else {
            reservedItem.setCount(reservedItem.getCount() + 1);
            // NOTE: latest price overrides existing price
            reservedItem.setPrice(price);
        } // else
        m_Reservations.put(reservedItem.getKey(), reservedItem);
    }


    public void unReserve(String key) {
        ReservedItem reservedItem = getReservedItem(key);
        if (reservedItem == null) {
            log.error("No reservation with key [" + key + "] exists for customer [" + m_nID + "]");
        } else {
            reservedItem.setCount(reservedItem.getCount() - 1);
        } // else
        m_Reservations.put(reservedItem.getKey(), reservedItem);
    }

    public ReservedItem getReservedItem(String key) {
        return (ReservedItem) m_Reservations.get(key);
    }

    public String printBill() {
        StringBuilder s = new StringBuilder("Bill for customer " + m_nID + "\n");
        Object key;
        for (Enumeration e = m_Reservations.keys(); e.hasMoreElements(); ) {
            key = e.nextElement();
            ReservedItem item = (ReservedItem) m_Reservations.get(key);
            s.append(item.getCount()).append(" ").append(item.getReservableItemKey()).
                    append(" $").append(item.getPrice()).append("\n");
        }
        return s.toString();
    }

    public String toString() {
        return "--- BEGIN CUSTOMER key='" + getKey() + "', id='" + getID() + "', reservations=>\n" +
                m_Reservations.toString() + "\n" +
                "--- END CUSTOMER ---";
    }

    public static String getKey(int customerID) {
        String s = "customer-" + customerID;
        return s.toLowerCase();
    }

    public String getKey() {
        return Customer.getKey(getID());
    }

    public RMConcurrentHashMap getReservations() {
        try {
            return m_Reservations;
        } catch (Exception e) {
            return null;
        } // catch
    }

}
