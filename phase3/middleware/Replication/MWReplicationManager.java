package Replication;

import MiddlewareImpl.MWResourceManager;
import MiddlewareImpl.MiddlewareManagerImpl;
import MiddlewareImpl.RMClusterMember;
import MiddlewareInterface.Middleware;
import ResInterface.ResourceManager;
import Transaction.TransactionManager;
import org.jgroups.JChannel;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import util.MiddlewareConstants;
import util.ResourceManagerType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static util.ResourceManagerType.*;

/**
 * Created by shabirmean on 2017-11-25 with some hope.
 */
public class MWReplicationManager {
    private static HashMap<String, String> rmConfigsMap;
    private static HashMap<ResourceManagerType, ResourceManager> resourceManagers = new HashMap<>();
    private static ConcurrentHashMap<ResourceManagerType, RMClusterMember> clusterMembers = new ConcurrentHashMap<>();

    public MWReplicationManager(HashMap<String, String> rmServerParams) {
        rmConfigsMap = rmServerParams;
    }


    public HashMap<ResourceManagerType, ResourceManager> initReplicationManager() {
        initReplicaCluster();
        initResourceManagers();
        return resourceManagers;
    }

    private void initReplicaCluster() {
        for (ResourceManagerType rmType : ResourceManagerType.values()) {
            String rmName = rmType.getCodeString();
            String rmAddress = rmConfigsMap.get(rmName).split(MiddlewareConstants.SEMICOLON)[0];
            int rmMCPort = -1;
            switch (rmType) {
                case CUSTOMER:
                    rmMCPort = MiddlewareConstants.CUSTOMER_CLUSTER_PORT;
                    continue;
//                    break;
                case CAR:
                    rmMCPort = MiddlewareConstants.CAR_CLUSTER_PORT;
                    break;
                case FLIGHT:
                    rmMCPort = MiddlewareConstants.FLIGHT_CLUSTER_PORT;
//                    break;
                    continue;
                case HOTEL:
                    rmMCPort = MiddlewareConstants.HOTEL_CLUSTER_PORT;
//                    break;
                    continue;
            }

            RMClusterMember rmClusterMember = new RMClusterMember(rmName, rmAddress, rmMCPort);
            Thread clusterJoiner = new Thread(() -> {
                try {
                    rmClusterMember.start();
                    clusterMembers.put(rmType, rmClusterMember);
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

        try {
            // create a new Server object
            MiddlewareManagerImpl obj = new MiddlewareManagerImpl();
            // dynamically generate the stub (client proxy)
            Middleware rm = (Middleware) UnicastRemoteObject.exportObject(obj, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(mwParams[1]));
            registry.rebind(MiddlewareConstants.MW_OBJECT_REG_ID, rm);
            System.err.println("Server ready");
//            server = "localhost";

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

            mw = new MWResourceManager();
            resourceManagers.put(CAR, cm);
            resourceManagers.put(FLIGHT, fm);
            resourceManagers.put(HOTEL, hm);
            resourceManagers.put(CUSTOMER, mw);
            System.out.println("Successful");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
        // Create and install a security manager
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new RMISecurityManager());
//        }
    }

    public static boolean reRegisterWithRegistry(String rmType) {
        try {
            if (rmType.equals(ResourceManagerType.CUSTOMER.getCodeString())) {
                //TODO:: Need to handle
            } else if (rmType.equals(ResourceManagerType.CAR.getCodeString())) {
                String[] cmParams = rmConfigsMap.get(CAR.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry carRegistry = LocateRegistry.getRegistry(cmParams[0], Integer.parseInt(cmParams[1]));
                ResourceManager cm = (ResourceManager) carRegistry.lookup(MiddlewareConstants.CM_OBJECT_REG_ID);
                if (cm != null) {
                    System.out.println("Connected to CarManager");
                } else {
                    System.out.println("Connecting to CarManager Unsuccessful");
                    return false;
                }
                resourceManagers.put(CAR, cm);

            } else if (rmType.equals(ResourceManagerType.FLIGHT.getCodeString())) {
                String[] fmParams = rmConfigsMap.get(FLIGHT.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry flightRegistry = LocateRegistry.getRegistry(fmParams[0], Integer.parseInt(fmParams[1]));
                ResourceManager fm = (ResourceManager) flightRegistry.lookup(MiddlewareConstants.FM_OBJECT_REG_ID);
                if (fm != null) {
                    System.out.println("Connected to FlightManager");
                } else {
                    System.out.println("Connecting to FLightManager Unsuccessful");
                    return false;
                }
                resourceManagers.put(FLIGHT, fm);

            } else if (rmType.equals(ResourceManagerType.HOTEL.getCodeString())) {
                String[] hmParams = rmConfigsMap.get(HOTEL.getCodeString()).split(MiddlewareConstants.SEMICOLON);
                Registry hotelRegistry = LocateRegistry.getRegistry(hmParams[0], Integer.parseInt(hmParams[1]));
                ResourceManager hm = (ResourceManager) hotelRegistry.lookup(MiddlewareConstants.HM_OBJECT_REG_ID);
                if (hm != null) {
                    System.out.println("Connected to HotelManager");
                } else {
                    System.out.println("Connecting to HotelrManager Unsuccessful");
                    return false;
                }
                resourceManagers.put(HOTEL, hm);
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }


}
