package Transaction;

import MiddlewareImpl.Car;
import MiddlewareImpl.Flight;
import MiddlewareImpl.Hotel;
import MiddlewareImpl.ResourceManagerType;

import java.util.HashMap;

public class Transaction {

    private int transactionId;
//    private HashMap<ResourceManagerType, HashMap<String, ReservableItem>> readSetMap = new HashMap<>();
//    private HashMap<ResourceManagerType, HashMap<String, ReservableItem>> writeSetMap = new HashMap<>();

    private HashMap<String, Flight> flightSet = new HashMap<>();
    private HashMap<String, Car> CarSet = new HashMap<>();
    private HashMap<String, Hotel> HotelSet = new HashMap<>();
    private HashMap<String, ResourceManagerType> writeSet = new HashMap<>();
    private HashMap<String, ResourceManagerType> deleteSet = new HashMap<>();

    Transaction(int Transaction){
        this.transactionId = transactionId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public HashMap<String, Flight> getFlightSet() {
        return flightSet;
    }

    public HashMap<String, Car> getCarSet() {
        return CarSet;
    }

    public HashMap<String, Hotel> getHotelSet() {
        return HotelSet;
    }

    public HashMap<String, ResourceManagerType> getWriteSet() {
        return writeSet;
    }

    public HashMap<String, ResourceManagerType> getDeleteSet() {
        return deleteSet;
    }

}
