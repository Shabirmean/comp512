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
    private boolean isCommit;

    public UpdatedItem(int tId, String itemKey, RMItem itemVal, boolean isEnd){
        this.transactionId = tId;
        this.itemKey = itemKey;
        this.itemVal = itemVal;
        this.isCommit = isEnd;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getItemKey() {
        return itemKey;
    }

    public RMItem getItemVal() {
        return itemVal;
    }

    public boolean isCommit() {
        return isCommit;
    }
}
