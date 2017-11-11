package Transaction;

import ResImpl.RMItem;
import util.ResourceManagerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction {

    private int transactionId;
    private TStatus status;
    private ConcurrentHashMap<String, RMItem> accessedItemSet = new ConcurrentHashMap<>();
//    private HashMap<String, ResourceManagerType> writeSet = new HashMap<>();
//    private HashMap<String, ResourceManagerType> deleteSet = new HashMap<>();

    private List<String> writeCarList = new ArrayList<>();
    private List<String> deleteCarList = new ArrayList<>();

    private List<String> writeFlightList = new ArrayList<>();
    private List<String> deleteFlightList = new ArrayList<>();

    private List<String> writeHotelList = new ArrayList<>();
    private List<String> deleteHotelList = new ArrayList<>();

    private List<String> writeCustomerList = new ArrayList<>();
    private List<String> deleteCustomerList = new ArrayList<>();

    Transaction(int transactionId){
        this.transactionId = transactionId;
        this.status = TStatus.RUNNING;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setStatus(Transaction.TStatus status) {
        this.status = status;
    }

    public Transaction.TStatus getStatus() {
        return this.status;
    }

    ConcurrentHashMap<String, RMItem> getAccessedItemSet() {
        return accessedItemSet;
    }

//    HashMap<String, ResourceManagerType> getWriteSet() {
//        return writeSet;
//    }
//
//    HashMap<String, ResourceManagerType> getDeleteSet() {
//        return deleteSet;
//    }

//    public List<String> getWriteCarList() {
//        return writeCarList;
//    }
//
//    public List<String> getDeleteCarList() {
//        return DeleteCarList;
//    }
//
//    public List<String> getWriteFlightList() {
//        return writeFlightList;
//    }
//
//    public List<String> getDeleteFlightList() {
//        return DeleteFlightList;
//    }
//
//    public List<String> getWriteHotelList() {
//        return writeHotelList;
//    }
//
//    public List<String> getDeleteHotelList() {
//        return DeleteHotelList;
//    }

    @SuppressWarnings("Duplicates")
    List<String> getCorrespondingWriteList(ResourceManagerType rmType){
        List<String> list = null;
        switch (rmType){
            case CAR:
                list = writeCarList;
                break;
            case FLIGHT:
                list = writeFlightList;
                break;
            case HOTEL:
                list = writeHotelList;
                break;
            case CUSTOMER:
                list = writeCustomerList;
                break;
        }
        return list;
    }

    @SuppressWarnings("Duplicates")
    List<String> getCorrespondingDeleteList(ResourceManagerType rmType){
        List<String> list = null;
        switch (rmType){
            case CAR:
                list = deleteCarList;
                break;
            case FLIGHT:
                list = deleteFlightList;
                break;
            case HOTEL:
                list = deleteHotelList;
                break;
            case CUSTOMER:
                list = deleteCustomerList;
                break;
        }
        return list;
    }

    void clearAll(){
        this.accessedItemSet.clear();
        this.writeCustomerList.clear();
        this.writeCarList.clear();
        this.writeFlightList.clear();
        this.writeHotelList.clear();
        this.deleteCustomerList.clear();
        this.deleteCarList.clear();
        this.deleteFlightList.clear();
        this.deleteHotelList.clear();
    }

    public enum TStatus {
        RUNNING(0),
        ENDED(1);

        private final int statusCode;
        private String status;

        TStatus(int statusCode) {
            this.statusCode = statusCode;
            switch (statusCode) {
                case 0:
                    this.status = "RUNNING";
                    break;
                case 1:
                    this.status = "ENDED";
                    break;
            }
        }

        public int getStatusCode() {
            return this.statusCode;
        }

        public String getStatus() {
            return this.status;
        }
    }

}
