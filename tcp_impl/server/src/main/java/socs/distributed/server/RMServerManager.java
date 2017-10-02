package socs.distributed.server;

import socs.distributed.resource.dto.RMConcurrentHashMap;
import socs.distributed.resource.dto.RMItem;
import socs.distributed.resource.dto.ReservableItem;
import socs.distributed.resource.entity.Car;
import socs.distributed.resource.entity.Flight;
import socs.distributed.resource.entity.Hotel;
import socs.distributed.resource.util.Trace;

import java.util.Vector;

@SuppressWarnings({"Duplicates", "unchecked"})
class RMServerManager {
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

    // deletes the entire item
    private boolean deleteItem(int id, String key) {
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
    private int queryNum(int id, String key) {
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
    private int queryPrice(int id, String key) {
        Trace.info("RM::queryPrice(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryPrice(" + id + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // reserve an item
    private ReservableItem reserveItem(int id, int customerID, String key, String location) {
        // check if the item is available
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") failed--item " +
                    "doesn't exist");
            return null;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") failed--No " +
                    "more items");
            return null;
        } else {
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return item;
        }
    }


    // reserve an item
    private boolean unReserveItem(int id, int customerID, String key, String location) {
        // check if the item is available
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::UnReserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") " +
                    "failed--item doesn't exist");
            return false;
        } else {
            item.setCount(item.getCount() + 1);
            item.setReserved(item.getReserved() - 1);
            Trace.info("RM::UnReserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }


    //    ========
    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
        Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));
        if (curObj == null) {
            // doesn't exist...add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice);
        } else {
            // add seats to existing flight and update the price...
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            } // if
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats=" + curObj
                    .getCount() + ", price=$" + flightPrice);
        } // else
        return (true);
    }

    boolean deleteFlight(int id, int flightNum) {
        return deleteItem(id, Flight.getKey(flightNum));
    }

    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    boolean addRooms(int id, String location, int count, int price) {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
        Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));
        if (curObj == null) {
            // doesn't exist...add it
            Hotel newObj = new Hotel(location, count, price);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count=" + count + ", " +
                    "price=$" + price);
        } else {
            // add count to existing object and update price...
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            } // if
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count=" + curObj
                    .getCount() + ", price=$" + price);
        } // else
        return (true);
    }

    // Delete rooms from a location
    boolean deleteRooms(int id, String location) {
        return deleteItem(id, Hotel.getKey(location));
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    boolean addCars(int id, String location, int count, int price) {
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
        Car curObj = (Car) readData(id, Car.getKey(location));
        if (curObj == null) {
            // car location doesn't exist...add it
            Car newObj = new Car(location, count, price);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count + ", price=$"
                    + price);
        } else {
            // add count to existing car location and update price...
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            } // if
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj
                    .getCount() + ", price=$" + price);
        } // else
        return (true);
    }

    // Delete cars from a location
    boolean deleteCars(int id, String location) {
        return deleteItem(id, Car.getKey(location));
    }

    // Returns the number of empty seats on this flight
    int queryFlight(int id, int flightNum) {
        return queryNum(id, Flight.getKey(flightNum));
    }

    // Returns price of this flight
    int queryFlightPrice(int id, int flightNum) {
        return queryPrice(id, Flight.getKey(flightNum));
    }

    // Returns the number of rooms available at a location
    int queryRooms(int id, String location) {
        return queryNum(id, Hotel.getKey(location));
    }

    // Returns room price at this location
    int queryRoomsPrice(int id, String location) {
        return queryPrice(id, Hotel.getKey(location));
    }

    // Returns the number of cars available at a location
    int queryCars(int id, String location) {
        return queryNum(id, Car.getKey(location));
    }

    // Returns price of cars at this location
    int queryCarsPrice(int id, String location) {
        return queryPrice(id, Car.getKey(location));
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
    ReservableItem reserveCar(int id, int customerID, String location) {
        return reserveItem(id, customerID, Car.getKey(location), location);
    }

    // Adds room reservation to this customer.
    ReservableItem reserveRoom(int id, int customerID, String location) {
        return reserveItem(id, customerID, Hotel.getKey(location), location);
    }

    // Adds flight reservation to this customer.
    ReservableItem reserveFlight(int id, int customerID, int flightNum) {
        return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    Vector<ReservableItem> reserveFlights(int id, int customerID, Vector flightNums) {
        Vector<ReservableItem> reservedFlights = new Vector<>();
        for (Object obj: flightNums) {
            int flightNum = (int) obj;
            ReservableItem item = reserveFlight(id, customerID, flightNum);
            if (item != null) {
                reservedFlights.add(item);
            }
        }
        return reservedFlights;
    }


    // removes car reservation to this customer.
    boolean unReserveCar(int id, int customerID, String location) {
        return unReserveItem(id, customerID, Car.getKey(location), location);
    }

    // removes room reservation to this customer.
    boolean unReserveRoom(int id, int customerID, String location) {
        return unReserveItem(id, customerID, Hotel.getKey(location), location);
    }

    // removes flight reservation to this customer.
    private boolean unReserveFlight(int id, int customerID, int flightNum) {
        return unReserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    boolean unReserveFlights(int id, int customerID, Vector flightNums) {
        boolean unReserveStatus = false;
        for (Object obj: flightNums) {
            int flightNum = (int) obj;
            unReserveStatus = unReserveFlight(id, customerID, flightNum);
        }
        return unReserveStatus;
    }
}