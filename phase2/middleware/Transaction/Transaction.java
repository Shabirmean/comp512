package Transaction;

import ResImpl.RMItem;
import util.ResourceManagerType;
import ResImpl.ReservableItem;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction {

    private int transactionId;
//    private HashMap<ResourceManagerType, HashMap<String, ReservableItem>> readSetMap = new HashMap<>();
//    private HashMap<ResourceManagerType, HashMap<String, ReservableItem>> writeSetMap = new HashMap<>();

    private ConcurrentHashMap<String, RMItem> accessedItemSet = new ConcurrentHashMap<>();
//    Vector<String> writeVector = new Vector<>();
//    Vector<String> deleteVector = new Vector<>();
//    private HashMap<String, Flight> flightSet = new HashMap<>();
//    private HashMap<String, Car> CarSet = new HashMap<>();
//    private HashMap<String, Hotel> HotelSet = new HashMap<>();
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

    public ConcurrentHashMap<String, RMItem> getAccessedItemSet() {
        return accessedItemSet;
    }

//    public Vector<String> getWriteVector() {
//        return writeVector;
//    }
//
//    public Vector<String> getDeleteVector() {
//        return deleteVector;
//    }

//    public HashMap<String, Flight> getFlightSet() {
//        return flightSet;
//    }
//
//    public HashMap<String, Car> getCarSet() {
//        return CarSet;
//    }
//
//    public HashMap<String, Hotel> getHotelSet() {
//        return HotelSet;
//    }
//
    public HashMap<String, ResourceManagerType> getWriteSet() {
        return writeSet;
    }

    public HashMap<String, ResourceManagerType> getDeleteSet() {
        return deleteSet;
    }

}
