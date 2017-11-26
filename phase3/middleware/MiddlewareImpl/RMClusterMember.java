package MiddlewareImpl;

import Replication.MWReplicationManager;
import ReplicationManager.UpdatedItem;
import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
import util.MiddlewareConstants;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by shabirmean on 2017-11-24 with some hope.
 */
@SuppressWarnings("Duplicates")
public class RMClusterMember extends ReceiverAdapter {
    public final Object REPLICATION_LOCK = new Object();
    private String rmType;
    private String channelName = "MW:RM::";
    private String clusterId = "RM";
    private String rmServerAdd;
    private int rmClusterPort;
    private Address clusterCoordinator;
    private JChannel channel;
    private boolean mwIsLeader;
    private Timer waitForLeaderTimer;

    public RMClusterMember(String rmName, String rmAddress, int mcPort) {
        this.rmType = rmName;
        this.channelName += rmName.toLowerCase();
        this.clusterId += rmName.toLowerCase();
        this.rmServerAdd = rmAddress;
        this.rmClusterPort = mcPort;
    }

    public void start() throws Exception {
        Protocol[] protocolStack;
        try {
            protocolStack = new Protocol[]{
                    new UDP()
                            .setValue(MiddlewareConstants.JGRP_BIND_ADDRESS, InetAddress.getByName(rmServerAdd))
                            .setValue(MiddlewareConstants.JGRP_MC_PORT, this.rmClusterPort),
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
                    new FRAG2()};

            this.channel = new JChannel(protocolStack).setReceiver(this).setName(channelName);
            this.channel.connect(this.clusterId);
//            channel.getState(null, 10000);
//            this.myReplicaId = this.channel.getAddressAsString();
            this.clusterCoordinator = this.channel.getView().getCoord();
//
//            while (this.clusterCoordinator.toString().equals(this.channelName)) {
//                System.out.println("MW::I am the only node in the " + clusterId + " cluster.");
//                System.out.println("MW::Exiting cluster temporarily [5s] to wait for someone else to join first.");
//                this.channel.disconnect();
//                Thread.sleep(5000);
//                this.channel.connect(clusterId);
////                this.myReplicaId = this.channel.getAddressAsString();
//                this.clusterCoordinator = this.channel.getView().getCoord();
//            }
            System.out.println("MW::RM::Connected to cluster-" + clusterId + " with id-" + channelName);
//            eventLoop();
//            channel.close();
        } catch (UnknownHostException e) {
            //TODO::Handle exceptions
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void eventLoop() {
        System.out.println("RM::Connected to cluster [" + clusterId + "] with replica id-" + channelName);
        while (true) {

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


    public void receive(Message msg) {
//        System.out.println("RM:: Replication message received from leader: " + msg.getSrc());
//        ArrayList<UpdatedItem> updatedItemList = deserializeBytes(msg.getBuffer());

    }

    public void viewAccepted(View newView) {
        System.out.println("MW::ViewChange" + this.clusterId + "-" + newView);
        Address previousCoordinator = this.clusterCoordinator;
        this.clusterCoordinator = newView.getCoord();

        if (newView.size() > 1) {
//            if (clusterCoordinator.toString().equals(channelName)) {
////                waitForLeaderTimer = new Timer();
////                waitForLeaderTimer.schedule(new TimerTask() {
////                    @Override
////                    public void run() {
////                        changeClusterCoordinator();
////                    }
////                }, 5 * 1000);
//
//                boolean viewChangeStatus = changeClusterCoordinator();
//                if (!viewChangeStatus) {
//                    System.out.println(channelName + "-Currently MW is leader. Waiting for a " + clusterId);
////                mwIsLeader = true;
////                if (channel.isConnected()) {
////                    channel.disconnect();
////                }
////                channel.close();
////                if (waitForLeaderTimer == null) {
////                    waitForLeaderTimer = new Timer();
////                    waitForLeaderTimer.scheduleAtFixedRate(new TimerTask() {
////                        @Override
////                        public void run() {
////                            try {
////                                System.out.println("Running....");
////                                channel.connect(clusterId);
////                            } catch (Exception e) {
////                                e.printStackTrace();
////                            }
////                        }
////                    }, 5 * 1000, 5 * 1000);
//                }
////                }
//            } else {
//                if (!newView.containsMember(previousCoordinator) || previousCoordinator.toString().equals(channelName)) {
//                    System.out.println("MW::Lost leader node of custer-" + clusterId);
//                    System.out.println("MW::New leader for cluster-" + clusterId + " is " + clusterCoordinator.toString());
//                    MWReplicationManager.reRegisterWithRegistry(rmType);
//                }
//            }

            if (!newView.containsMember(previousCoordinator) || previousCoordinator.toString().equals(channelName)) {
                System.out.println("MW::Lost leader node of custer-" + clusterId);
                System.out.println("MW::New leader for cluster-" + clusterId + " is " + clusterCoordinator.toString());
                MWReplicationManager.reRegisterWithRegistry(rmType);
            }
            
        } else {
            System.out.println(channelName + "-Currently MW is leader. Waiting for a " + clusterId);
        }
    }


    private boolean changeClusterCoordinator() {
        View view = channel.getView();
        Address local_addr = channel.getAddress();
        Address coord = view.getMembersRaw()[0];
        if (!local_addr.equals(coord)) {
            System.err.println(channelName + "-View can only be changed on coordinator");
            return false;
        }
        if (view.size() == 1) {
            System.err.println(channelName + "-Coordinator cannot change as view only has a single member");
            return false;
        }

        List<Address> mbrs = new ArrayList<>(view.getMembers());
        long new_id = view.getViewId().getId() + 1;
        Address tmp_coord = mbrs.remove(0);
        mbrs.add(tmp_coord);
        View new_view = new View(mbrs.get(0), new_id, mbrs);
        GMS gms = channel.getProtocolStack().findProtocol(GMS.class);
        gms.castViewChangeAndSendJoinRsps(new_view, null, mbrs, mbrs, null);
        return true;
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
