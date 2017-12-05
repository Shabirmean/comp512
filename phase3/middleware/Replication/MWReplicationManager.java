package Replication;

import MiddlewareImpl.MWResourceManager;
import MiddlewareImpl.MiddlewareManagerImpl;
import MiddlewareInterface.Middleware;
import ResInterface.ResourceManager;
import Transaction.TransactionManager;
import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.Protocol;
import org.jgroups.util.Util;
import util.MiddlewareConstants;
import util.ResourceManagerType;

import java.io.*;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static util.ResourceManagerType.*;

/**
 * Created by shabirmean on 2017-11-27 with some hope.
 */
public class MWReplicationManager extends ReceiverAdapter implements Serializable {
    private static final long serialVersionUID = 1421146759512286394L;

    private static final Object LOCK = new Object();
    private static final String CLUSTER_NAME = "MWCluster";

    private static String myReplicaId = "MW::";
    private static String clientMemberPrefix = "Client::MW";
    private static JChannel channel;
    private static String mwServerAdd;
    private static boolean iAmMWMaster = false;

    private static HashMap<String, String> rmConfigsMap;
    private static MiddlewareManagerImpl middlewareManager;

    private MWReplicationManager(String uuid, String mwAddress) {
        myReplicaId += uuid;
        mwServerAdd = mwAddress;
    }

    public static void main(String args[]) {
        String server = "localhost";
        String carserver = "";
        String flightserver = "";
        String hotelserver = "";
        int port = 1099;
        int carport = 1100;
        int hotelport = 1101;
        int flightport = 1102;

        if (args.length == 1) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
        } else if (args.length == 4) {
            server = args[0];
            carserver = args[1];
            flightserver = args[2];
            hotelserver = args[3];
        }

        String middlewareServerConfig = server + ":" + port;
        String carServerConfig = carserver + ":" + carport;
        String flightServerConfig = flightserver + ":" + flightport;
        String hotelServerConfig = hotelserver + ":" + hotelport;

        System.out.println("Middleware: " + middlewareServerConfig);
        System.out.println("Car Manager: " + carServerConfig);
        System.out.println("Hotel Manager: " + hotelServerConfig);
        System.out.println("Flight Manager: " + flightServerConfig);

        rmConfigsMap = new HashMap<>();
        rmConfigsMap.put(ResourceManagerType.CUSTOMER.getCodeString(), middlewareServerConfig);
        rmConfigsMap.put(ResourceManagerType.CAR.getCodeString(), carServerConfig);
        rmConfigsMap.put(ResourceManagerType.FLIGHT.getCodeString(), flightServerConfig);
        rmConfigsMap.put(ResourceManagerType.HOTEL.getCodeString(), hotelServerConfig);

        final String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        System.out.println("MW::Random UUID-" + uuid);

        MWReplicationManager mwReplicationManager = new MWReplicationManager(uuid, server);
        middlewareManager = new MiddlewareManagerImpl(new TransactionManager());
        middlewareManager.getTransactionMan().initTransactionManager();
        mwReplicationManager.start();
    }

    public void start() {
        Protocol[] protocolStack;
        Address coordinator;

        try {
            protocolStack = new Protocol[]{
                    new UDP()
                            .setValue(MiddlewareConstants.JGRP_BIND_ADDRESS, InetAddress.getByName(mwServerAdd))
                            .setValue(MiddlewareConstants.JGRP_MC_PORT, MiddlewareConstants.MW_CLUSTER_PORT),
                    new PING(),
                    new MERGE3(),
                    new FD_SOCK(),
                    new FD_ALL(),
                    new VERIFY_SUSPECT(),
                    new BARRIER(),
                    new NAKACK2(),
                    new UNICAST3(),
                    new STABLE(),
                    new GMS(),
                    new UFC(),
                    new MFC(),
                    new FRAG2(),
                    new STATE_TRANSFER()};

            channel = new JChannel(protocolStack).setReceiver(this).setName(myReplicaId);
            channel.connect(CLUSTER_NAME);

            coordinator = channel.getView().getCoord();
            if (coordinator.toString().equals(myReplicaId)) {
                synchronized (LOCK) {
                    if (!iAmMWMaster) {
                        iAmMWMaster = switchToCoordinatorMode();
                    }
                }
                System.out.println("MW::I am serving as Master.");
            } else {
                if (!coordinator.toString().contains(clientMemberPrefix)) {
                    channel.getState(null, 10000);
                } else {
                    changeClusterCoordinator();
                }
            }
        } catch (Exception e) {
            //TODO:: Handle this please
            e.printStackTrace();
        }
    }


    private boolean switchToCoordinatorMode() {
        initRMReplicaCluster();
        initResourceManagers();
        return true;
    }


    @SuppressWarnings("Duplicates")
    public static void updateReplicas(ReplicationObject repObj) {
        try {
            Message msg = new Message(null, serializeObject(repObj));
            channel.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receive(Message msg) {
        if (!iAmMWMaster) {
            System.out.println("MW::Replication message received from MW-leader: " + msg.getSrc());
            ReplicationObject repObj = deserializeBytes(msg.getBuffer());
            middlewareManager.setMWState(repObj);
        }
    }

    public void viewAccepted(View newView) {
        System.out.println("RM::View-" + newView);
        String coordinatorId = newView.getCoord().toString();
        if (coordinatorId.equals(myReplicaId) && !iAmMWMaster) {
            System.out.println("RM::I am now the MW Leader!");
            System.out.println("RM::Switching MW Leader mode.....");
            synchronized (LOCK) {
                if (!iAmMWMaster) {
                    iAmMWMaster = switchToCoordinatorMode();
                }
            }
        } else if (coordinatorId.contains(clientMemberPrefix) &&
                newView.getMembersRaw()[1].toString().equals(myReplicaId)) {
            changeClusterCoordinator();
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        ReplicationObject replicationObject = Util.objectFromStream(new DataInputStream(input));
        synchronized (LOCK) {
            middlewareManager.setMWState(replicationObject);
        }
        System.out.println("MW::Synchronized MW to have the same state as all other replicas.");
    }

    public void getState(OutputStream output) throws Exception {
        synchronized (LOCK) {
            Util.objectToStream(middlewareManager.getMWState(), new DataOutputStream(output));
        }
    }


    @SuppressWarnings("Duplicates")
    private static byte[] serializeObject(ReplicationObject repObj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] objectInBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(repObj);
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
    private ReplicationObject deserializeBytes(byte[] objectInBytes) {
        ReplicationObject repObj = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(objectInBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            repObj = (ReplicationObject) in.readObject();
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
        return repObj;
    }

    @SuppressWarnings("Duplicates")
    private void changeClusterCoordinator() {
        View view = channel.getView();
        if (view.size() == 1) {
            System.err.println(CLUSTER_NAME + "-Coordinator cannot change as view only has a single member");
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

    private void initRMReplicaCluster() {
        for (ResourceManagerType rmType : ResourceManagerType.values()) {
            String rmName = rmType.getCodeString();
//            String rmAddress = rmConfigsMap.get(rmName).split(MiddlewareConstants.SEMICOLON)[0];
            String rmAddress = rmConfigsMap.get(ResourceManagerType.CUSTOMER.getCodeString()).
                    split(MiddlewareConstants.SEMICOLON)[0];
            int rmMCPort = -1;
            switch (rmType) {
                case CUSTOMER:
                    rmMCPort = MiddlewareConstants.MW_CLUSTER_PORT;
                    continue;
                case CAR:
                    rmMCPort = MiddlewareConstants.CAR_CLUSTER_PORT;
                    break;
                case FLIGHT:
                    rmMCPort = MiddlewareConstants.FLIGHT_CLUSTER_PORT;
                    break;
                case HOTEL:
                    rmMCPort = MiddlewareConstants.HOTEL_CLUSTER_PORT;
                    break;
            }

            RMClusterMember rmClusterMember = new RMClusterMember(rmName, rmAddress, rmMCPort);
            Thread clusterJoiner = new Thread(() -> {
                try {
                    rmClusterMember.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            clusterJoiner.start();
        }
    }

    private void initResourceManagers() {
        ResourceManager cm;
        ResourceManager hm;
        ResourceManager fm;
        MWResourceManager mw;

        String[] mwParams = rmConfigsMap.get(CUSTOMER.getCodeString()).split(MiddlewareConstants.SEMICOLON);
        String[] cmParams = rmConfigsMap.get(CAR.getCodeString()).split(MiddlewareConstants.SEMICOLON);
        String[] fmParams = rmConfigsMap.get(FLIGHT.getCodeString()).split(MiddlewareConstants.SEMICOLON);
        String[] hmParams = rmConfigsMap.get(HOTEL.getCodeString()).split(MiddlewareConstants.SEMICOLON);
        HashMap<ResourceManagerType, ResourceManager> resourceManagers =
                middlewareManager.getTransactionMan().getResourceManagers();

        try {
            Registry carRegistry = LocateRegistry.getRegistry(cmParams[0], Integer.parseInt(cmParams[1]));
            cm = (ResourceManager) carRegistry.lookup(MiddlewareConstants.CM_OBJECT_REG_ID);
            if (cm != null) {
                System.out.println("Connected to CarManager");
            } else {
                System.out.println("Connecting to CarManager Unsuccessful");
            }

            Registry hotelRegistry = LocateRegistry.getRegistry(hmParams[0], Integer.parseInt(hmParams[1]));
            hm = (ResourceManager) hotelRegistry.lookup(MiddlewareConstants.HM_OBJECT_REG_ID);
            if (hm != null) {
                System.out.println("Connected to HotelManager");
            } else {
                System.out.println("Connecting to HotelrManager Unsuccessful");
            }

            Registry flightRegistry = LocateRegistry.getRegistry(fmParams[0], Integer.parseInt(fmParams[1]));
            fm = (ResourceManager) flightRegistry.lookup(MiddlewareConstants.FM_OBJECT_REG_ID);
            if (fm != null) {
                System.out.println("Connected to FlightManager");
            } else {
                System.out.println("Connecting to FLightManager Unsuccessful");
            }

            mw = (MWResourceManager) resourceManagers.get(CUSTOMER);
            if (mw == null) {
                mw = new MWResourceManager();
                resourceManagers.put(CUSTOMER, mw);
            }
            resourceManagers.put(CAR, cm);
            resourceManagers.put(FLIGHT, fm);
            resourceManagers.put(HOTEL, hm);
            System.out.println("Successful");

            Middleware mwRM = (Middleware) UnicastRemoteObject.exportObject(middlewareManager, 0);
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(mwParams[1]));
            registry.rebind(MiddlewareConstants.MW_OBJECT_REG_ID, mwRM);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
        // Create and install a security manager
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new RMISecurityManager());
//        }
    }

    static void reRegisterWithRegistry(String rmType) {
        try {
            if (rmType.equals(ResourceManagerType.CUSTOMER.getCodeString())) {
                //TODO:: Need to handle
            } else if (rmType.equals(ResourceManagerType.CAR.getCodeString())) {
                String[] cmParams = rmConfigsMap.get(CAR.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry carRegistry = LocateRegistry.getRegistry(cmParams[0], Integer.parseInt(cmParams[1]));
                ResourceManager cm = (ResourceManager) carRegistry.lookup(MiddlewareConstants.CM_OBJECT_REG_ID);
                if (cm != null) {
                    System.out.println("MW:: Re-Connected to CarManager");
                } else {
                    System.out.println("MW:: Re-Connecting to CarManager Unsuccessful");
                }
                middlewareManager.getTransactionMan().getResourceManagers().put(CAR, cm);

            } else if (rmType.equals(ResourceManagerType.FLIGHT.getCodeString())) {
                String[] fmParams = rmConfigsMap.get(FLIGHT.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry flightRegistry = LocateRegistry.getRegistry(fmParams[0], Integer.parseInt(fmParams[1]));
                ResourceManager fm = (ResourceManager) flightRegistry.lookup(MiddlewareConstants.FM_OBJECT_REG_ID);
                if (fm != null) {
                    System.out.println("MW:: Re-Connected to FlightManager");
                } else {
                    System.out.println("MW:: Re-Connecting to FLightManager Unsuccessful");
                }
                middlewareManager.getTransactionMan().getResourceManagers().put(FLIGHT, fm);

            } else if (rmType.equals(ResourceManagerType.HOTEL.getCodeString())) {
                String[] hmParams = rmConfigsMap.get(HOTEL.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry hotelRegistry = LocateRegistry.getRegistry(hmParams[0], Integer.parseInt(hmParams[1]));
                ResourceManager hm = (ResourceManager) hotelRegistry.lookup(MiddlewareConstants.HM_OBJECT_REG_ID);
                if (hm != null) {
                    System.out.println("MW:: Re-Connected to HotelManager");
                } else {
                    System.out.println("MW:: Re-Connecting to HotelrManager Unsuccessful");
                }
                middlewareManager.getTransactionMan().getResourceManagers().put(HOTEL, hm);
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
