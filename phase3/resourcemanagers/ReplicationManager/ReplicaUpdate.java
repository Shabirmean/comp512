package ReplicationManager;

import ResImpl.RMHashtable;

import java.util.ArrayList;

/**
 * Created by shabirmean on 2017-11-23 with some hope.
 */
public interface ReplicaUpdate {
    void updateReplica(ArrayList<UpdatedItem> updatedItemList);
    void updateReplica(UpdatedItem updatedItem);
    void setRMState(RMHashtable rmHashtable);
    RMHashtable getRMState();
}
