package ReplicationManager;

import ResImpl.RMHashtable;
import ResImpl.ResourceManagerImpl;
import ResInterface.ResourceManager;
import org.jgroups.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.util.Util;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shabirmean on 2017-11-21 with some hope.
 */
public class RMReplicationManager extends ReceiverAdapter implements Serializable {
    private static final long serialVersionUID = 1400746759512286105L;

    private static final Object REPLICATION_LOCK = new Object();
    private static ReplicaUpdate resourceManager;
    private static int registryPort;
    private static String myReplicaId;
    private static String clusterName = "RM";
    private static String mwMemberPrefix = "MW:RM::";
    private static JChannel channel;
    private static boolean iAmMaster = false;

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            registryPort = Integer.parseInt(args[0]);
            clusterName += "ALL";
        } else if (args.length == 2) {
            registryPort = Integer.parseInt(args[0]);
            clusterName += args[1];
            mwMemberPrefix += args[1];
        } else if (args.length != 0) {
            System.err.println("Wrong usage");
            System.out.println("Usage: java ReplicationManager.RMReplicationManager [port]");
            System.exit(1);
        }
        new RMReplicationManager().start();
    }

    private void start() throws Exception {
        resourceManager = new ResourceManagerImpl(this);
        channel = new JChannel().setReceiver(this);
        channel.connect(clusterName);
        myReplicaId = channel.getAddressAsString();

        Address coordinator = channel.getView().getCoord();
        if (coordinator.toString().equals(myReplicaId)) {
            iAmMaster = registerResourceManager();
            System.out.println("RM::I am serving as Master.");
        } else {
            if (!coordinator.toString().contains(mwMemberPrefix)) {
                channel.getState(null, 10000);
            } else {
                changeClusterCoordinator();
            }
        }
    }

    @SuppressWarnings("Duplicates")
//    public boolean updateReplicas(ArrayList<UpdatedItem> updatedItemList) {
    public boolean updateReplicas(UpdatedItem updatedItem) {
        try {
//            Message msg = new Message(null, serializeObject(updatedItemList));
            Message msg = new Message(null, serializeObject(updatedItem));
            channel.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean registerResourceManager() {
        boolean registered = false;
        try {
            ResourceManager rm = (
                    ResourceManager) UnicastRemoteObject.exportObject((ResourceManagerImpl) resourceManager, 0);
            Registry registry = LocateRegistry.getRegistry(registryPort);
            registry.rebind("ShabirJianResourceManager", rm);
            System.err.println("RM::Server ready");
            registered = true;
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
        return registered;
    }

    public void receive(Message msg) {
        if (!iAmMaster) {
            System.out.println("RM::Replication message received from leader: " + msg.getSrc());
//        ArrayList<UpdatedItem> updatedItemList = deserializeBytes(msg.getBuffer());
//        resourceManager.updateReplica(updatedItemList);

            UpdatedItem updatedItem = deserializeBytes(msg.getBuffer());
            resourceManager.updateReplica(updatedItem);
        }
    }

    public void viewAccepted(View new_view) {
        System.out.println("RM::View-" + new_view);
        String coordinatorId = new_view.getCoord().toString();
        if (coordinatorId.equals(myReplicaId) && !iAmMaster) {
            System.out.println("RM::I am an RM Leader!");
            System.out.println("RM::Registering with RMI registry.....");
            iAmMaster = registerResourceManager();
        } else if (coordinatorId.contains(mwMemberPrefix) && new_view.getMembersRaw()[1].toString().equals
                (myReplicaId)) {
            changeClusterCoordinator();
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        RMHashtable rmHashtable = Util.objectFromStream(new DataInputStream(input));
        synchronized (REPLICATION_LOCK) {
            resourceManager.setRMState(rmHashtable);
        }
        System.out.println("RM::Synchronized RM to have the same state as all other replicas.");
    }

    public void getState(OutputStream output) throws Exception {
        synchronized (REPLICATION_LOCK) {
            Util.objectToStream(resourceManager.getRMState(), new DataOutputStream(output));
        }
    }


    @SuppressWarnings("Duplicates")
//    private byte[] serializeObject(ArrayList<UpdatedItem> updatedItemList) {
    private byte[] serializeObject(UpdatedItem updatedItem) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] objectInBytes = null;
        try {
            out = new ObjectOutputStream(bos);
//            out.writeObject(updatedItemList);
            out.writeObject(updatedItem);
            out.flush();
            objectInBytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return objectInBytes;
    }


    @SuppressWarnings({"unchecked", "Duplicates"})
//    private ArrayList<UpdatedItem> deserializeBytes(byte[] objectInBytes) {
    private UpdatedItem deserializeBytes(byte[] objectInBytes) {
//        ArrayList<UpdatedItem> updatedItemList = null;
        UpdatedItem updatedItem = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(objectInBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
//            updatedItemList = (ArrayList<UpdatedItem>) in.readObject();
            updatedItem = (UpdatedItem) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
//        return updatedItemList;
        return updatedItem;
    }

    @SuppressWarnings("Duplicates")
    private void changeClusterCoordinator() {
        View view = channel.getView();
        if (view.size() == 1) {
            System.err.println(clusterName + "-Coordinator cannot change as view only has a single member");
            return;
        }
        List<Address> mbrs = new ArrayList<>(view.getMembers());
        long new_id = view.getViewId().getId() + 1;
        Address tmp_coord = mbrs.remove(0);
        mbrs.add(tmp_coord);
        View new_view = new View(mbrs.get(0), new_id, mbrs);
        GMS gms = channel.getProtocolStack().findProtocol(GMS.class);
        gms.castViewChangeAndSendJoinRsps(new_view, null, mbrs, mbrs, null);
    }

}
