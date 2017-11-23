package ReplicationManager;

import ResImpl.RMHashtable;
import ResImpl.ResourceManagerImpl;
import ResInterface.ResourceManager;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by shabirmean on 2017-11-21 with some hope.
 */
public class RMReplicationManager extends ReceiverAdapter {
    public static final Object REPLICATION_LOCK = new Object();
    private static ReplicaUpdate resourceManager;
    private static int registryPort;
    private static String myReplicaId;
    private static String replicaType = "RMCluster::";
//    private boolean iAmLeader;
    private JChannel channel;
    //    private String user_name = System.getProperty("user.name", "n/a");

    private RMReplicationManager() {
//        iAmLeader = false;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("jgroups.bind_addr", "127.0.0.1");
        System.setProperty("java.net.preferIPv4Stack", "true");

        if (args.length == 1) {
            registryPort = Integer.parseInt(args[0]);
            replicaType += "ALL";
        } else if (args.length == 2) {
            registryPort = Integer.parseInt(args[0]);
            replicaType += args[1];
        } else if (args.length != 0) {
            System.err.println("Wrong usage");
            System.out.println("Usage: java ResImpl.ResourceManagerImpl [port]");
            System.exit(1);
        }
        new RMReplicationManager().start();
    }

    private void start() throws Exception {
        channel = new JChannel().setReceiver(this);
        channel.connect(replicaType);
        channel.getState(null, 10000);

        myReplicaId = channel.getAddressAsString();
        System.out.println("RM:: Connected to cluster [" + replicaType + "] with replica id-" + myReplicaId);
        eventLoop();
        channel.close();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void eventLoop() {
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("RM:: Replica initialized and waiting for update....");
        while (true) {
//            try {
//                System.out.print("> ");
//                System.out.flush();
//                String line = in.readLine().toLowerCase();
//                if (line.startsWith("quit") || line.startsWith("exit")) {
//                    break;
//                }
//                line = "[" + user_name + "] " + line;
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


    private void registerResourceManager() {
        try {
            resourceManager = new ResourceManagerImpl(this);
            ResourceManager rm = (
                    ResourceManager) UnicastRemoteObject.exportObject((ResourceManagerImpl) resourceManager, 0);
            Registry registry = LocateRegistry.getRegistry(registryPort);
            registry.rebind("ShabirJianResourceManager", rm);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    public void receive(Message msg) {
        System.out.println("RM:: Replication message received from leader: " + msg.getSrc());
        ArrayList<UpdatedItem> updatedItemList = deserializeBytes(msg.getBuffer());
        resourceManager.updateReplica(updatedItemList);
//        synchronized (REPLICATION_LOCK) {
//
//        }
    }

    public void viewAccepted(View new_view) {
        System.out.println("RM:: [View] " + new_view);
        String coordinatorId = new_view.getCoord().toString(); // new_view.getCreator().toString()
        if (coordinatorId.equals(myReplicaId)) {
            System.out.println("RM:: I am an RM Leader!");
            System.out.println("RM:: Registering with RMI registry.....");
            registerResourceManager();
//            iAmLeader = true;
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        RMHashtable rmHashtable = Util.objectFromStream(new DataInputStream(input));
        synchronized (REPLICATION_LOCK) {
            resourceManager.setRMState(rmHashtable);
        }
        System.out.println("RM:: Synchronized RM to have the same state as all other replicas.");
    }

    public void getState(OutputStream output) throws Exception {
        synchronized (REPLICATION_LOCK) {
            Util.objectToStream(resourceManager.getRMState(), new DataOutputStream(output));
        }
    }


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


    @SuppressWarnings("unchecked")
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

}
