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
public class RMReplicationManager extends ReceiverAdapter {
    public static final Object REPLICATION_LOCK = new Object();
    private static ReplicaUpdate resourceManager;
    private static int registryPort;
    private static String myReplicaId;
    private static String replicaType = "RM";
    private static String mwMemberId = "MW:RM::";
    private JChannel channel;
    private boolean iAmMaster = false;


    public static void main(String[] args) throws Exception {
//        System.setProperty("jgroups.bind_addr", "127.0.0.1");
//        System.setProperty("java.net.preferIPv4Stack", "true");
//        System.setProperty("java.net.preferIPv6Addresses", "true");
//        System.setProperty("jgroups.udp.mcast_port", "12345");

        if (args.length == 1) {
            registryPort = Integer.parseInt(args[0]);
            replicaType += "ALL";
        } else if (args.length == 2) {
            registryPort = Integer.parseInt(args[0]);
            replicaType += args[1];
            mwMemberId += args[1];
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
        channel.connect(replicaType);

        myReplicaId = channel.getAddressAsString();

        Address coordinator = channel.getView().getCoord();
        if (coordinator.toString().equals(myReplicaId)) {
            iAmMaster = registerResourceManager();
            System.out.println("RM::I am serving as Master.");
        } else {
            if (!coordinator.toString().equals(mwMemberId)) {
                System.out.println("########Ran this");
                channel.getState(null, 10000);
            } else {
                changeClusterCoordinator();
            }
        }

//        else if (coordinator.toString().equals(mwMemberId)) {
//            System.out.println("RM:: Middleware seems to be coordinator. Changing it.");
//        }
        eventLoop();
        channel.close();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void eventLoop() {
        System.out.println("RM::Connected to cluster [" + replicaType + "] with replica id-" + myReplicaId);
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//        System.out.println("RM:: Replica initialized and waiting for update....");
        while (true) {
//            try {
//                System.out.print("> ");
//                System.out.flush();
//                String line = in.readLine().toLowerCase();
//                if (line.startsWith("quit") || line.startsWith("exit")) {
//                    break;
//                }
//                line = "[" + myReplicaId + "] " + line;
//                Message msg = new Message(null, line);
//                channel.send(msg);
//            } catch (Exception e) {
//            }
        }
    }

    public boolean updateReplicas(ArrayList<UpdatedItem> updatedItemList) {
        try {
            Message msg = new Message(null, serializeObject(updatedItemList));
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

        // Create and install a security manager
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new RMISecurityManager());
//        }
        return registered;
    }

    public void receive(Message msg) {
        System.out.println("RM::Replication message received from leader: " + msg.getSrc());
        ArrayList<UpdatedItem> updatedItemList = deserializeBytes(msg.getBuffer());
        resourceManager.updateReplica(updatedItemList);
//        synchronized (REPLICATION_LOCK) {
//
//        }
    }

    public void viewAccepted(View new_view) {
        System.out.println("RM::View-" + new_view);
        String coordinatorId = new_view.getCoord().toString(); // new_view.getCreator().toString()
        System.out.println("#####" + coordinatorId);
        System.out.println("#####" + myReplicaId);
        System.out.println("#####" + iAmMaster);
        if (coordinatorId.equals(myReplicaId) && !iAmMaster) {
            System.out.println("RM::I am an RM Leader!");
            System.out.println("RM::Registering with RMI registry.....");
            iAmMaster = registerResourceManager();
        } else if (coordinatorId.equals(mwMemberId) && new_view.getMembersRaw()[1].toString().equals(myReplicaId)) {
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
    private byte[] serializeObject(ArrayList<UpdatedItem> updatedItemList) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] objectInBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(updatedItemList);
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
    private ArrayList<UpdatedItem> deserializeBytes(byte[] objectInBytes) {
        ArrayList<UpdatedItem> updatedItemList = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(objectInBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            updatedItemList = (ArrayList<UpdatedItem>) in.readObject();
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
        return updatedItemList;
    }

    @SuppressWarnings("Duplicates")
    private void changeClusterCoordinator() {
        View view = channel.getView();
        Address local_addr = channel.getAddress();
        Address coord = view.getMembersRaw()[0];
//        if (!local_addr.equals(coord)) {
//            System.err.println(replicaType + "-View can only be changed on coordinator");
//            return false;
//        }
        if (view.size() == 1) {
            System.err.println(replicaType + "-Coordinator cannot change as view only has a single member");
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
