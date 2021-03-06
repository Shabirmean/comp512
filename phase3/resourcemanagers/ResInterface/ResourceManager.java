package ResInterface;


import ResImpl.RMItem;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Simplified version from CSE 593 Univ. of Washington
 * <p>
 * Distributed  System in Java.
 * <p>
 * failure reporting is done using two pieces, exceptions and boolean
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * <p>
 * If there is a boolean return value and you're not sure how it
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface ResourceManager extends Remote, Serializable {
    long serialVersionUID = 1400746759512286101L;
    //TODO:: Use these and see
    String MW_OBJECT_REG_ID = "ShabirJianMiddleware";
    String CM_OBJECT_REG_ID = "ShabirJianRMCar";
    String FM_OBJECT_REG_ID = "ShabirJianRMFlight";
    String HM_OBJECT_REG_ID = "ShabirJianRMHotel";

    public void writeData(int id, String key, RMItem value) throws RemoteException;

    public boolean deleteItem(int id, String key) throws RemoteException;

    /* Add seats to a flight.  In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return success.
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException;

    /* Add cars to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException;

    /* Add rooms to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException;


    /* new customer just returns a unique customer identifier */
    public int newCustomer(int id) throws RemoteException;

    /* new customer with providing id */
    public boolean newCustomer(int id, int cid) throws RemoteException;

    /**
     * Delete the entire flight.
     * deleteflight implies whole deletion of the flight.
     * all seats, all reservations.  If there is a reservation on the flight,
     * then the flight cannot be deleted
     *
     * @return success.
     */
    public boolean deleteFlight(int id, int flightNum) throws RemoteException;

    /* Delete all Cars from a location.
     * It may not succeed if there are reservations for this location
     *
     * @return success
     */
    public boolean deleteCars(int id, String location) throws RemoteException;

    /* Delete all Rooms from a location.
     * It may not succeed if there are reservations for this location.
     *
     * @return success
     */
    public boolean deleteRooms(int id, String location) throws RemoteException;

    /* deleteCustomer removes the customer and associated reservations */
    public boolean deleteCustomer(int id, int customer) throws RemoteException;

//    /* returns the number of reserved items (seats/rooms/cars) of the given resource. */
//    public int getReserveCount(int id, String key) throws RemoteException;
    public RMItem getItem(int id, String itemKey) throws RemoteException;

    /* queryFlight returns the number of empty seats. */
    public int queryFlight(int id, int flightNumber) throws RemoteException;

    /* return the number of cars available at a location */
    public int queryCars(int id, String location) throws RemoteException;

    /* return the number of rooms available at a location */
    public int queryRooms(int id, String location) throws RemoteException;

    /* return a bill */
    public String queryCustomerInfo(int id, int customer) throws RemoteException;

    /* queryFlightPrice returns the price of a seat on this flight. */
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException;

    /* return the price of a car at a location */
    public int queryCarsPrice(int id, String location) throws RemoteException;

    /* return the price of a room at a location */
    public int queryRoomsPrice(int id, String location) throws RemoteException;

    /* Reserve a seat on this flight*/
    public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException;

    /* reserve a car at this location */
    public boolean reserveCar(int id, int customer, String location) throws RemoteException;

    /* reserve a room certain at this location */
    public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException;

    /* reserve an itinerary */
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room)
            throws RemoteException;

    public int start() throws RemoteException;

    public boolean commit(int transactionId)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException;

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException;

    public boolean shutdown() throws RemoteException;
}
