package ReplicationManager;

import ResImpl.RMItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by shabirmean on 2017-11-23 with some hope.
 */
public class UpdatedItem implements Serializable {
    private static final long serialVersionUID = 1400746759512286101L;
    private int transactionId;
    private String itemKey;
    private RMItem itemVal;

    public UpdatedItem(int tId, String itemKey, RMItem itemVal){
        this.transactionId = tId;
        this.itemKey = itemKey;
        this.itemVal = itemVal;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public RMItem getItemVal() {
        return itemVal;
    }

    public void setItemVal(RMItem itemVal) {
        this.itemVal = itemVal;
    }
}
