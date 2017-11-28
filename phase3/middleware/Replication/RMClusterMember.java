package Replication;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.Protocol;
import util.MiddlewareConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by shabirmean on 2017-11-24 with some hope.
 */
@SuppressWarnings("Duplicates")
public class RMClusterMember extends ReceiverAdapter {
    private String rmType;
    private String mwRMNodeId = "MW:RM::";
    private String clusterId = "RM";
    private String rmServerAdd;
    private int rmClusterPort;
    private Address clusterCoordinator;
    private JChannel channel;

    RMClusterMember(String rmName, String rmAddress, int mcPort) {
        rmType = rmName;
        mwRMNodeId += rmName.toLowerCase();
        clusterId += rmName.toLowerCase();
        rmServerAdd = rmAddress;
        rmClusterPort = mcPort;
    }

    public void start() throws Exception {
        System.setProperty("jgroups.bind_addr", rmServerAdd);
        System.setProperty("jgroups.udp.mcast_port", "" + rmClusterPort);

        Protocol[] protocolStack;
        try {
            protocolStack = new Protocol[]{
                    new UDP()
//                            .setValue(MiddlewareConstants.JGRP_BIND_ADDRESS, InetAddress.getByName(rmServerAdd))
                            .setValue(MiddlewareConstants.JGRP_BIND_ADDRESS, InetAddress.getByName(rmServerAdd))
                            .setValue(MiddlewareConstants.JGRP_MC_PORT, rmClusterPort),
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

//            channel = new JChannel(protocolStack).setReceiver(this).setName(mwRMNodeId);
            channel = new JChannel().setReceiver(this).setName(mwRMNodeId);
            channel.connect(clusterId);

            clusterCoordinator = channel.getView().getCoord();
            System.out.println("MW::RM::Connected to cluster-" + clusterId + " with id-" + mwRMNodeId +
                    " under leader-" + clusterCoordinator);

        } catch (UnknownHostException e) {
            //TODO::Handle exceptions
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void viewAccepted(View newView) {
        System.out.println("MW::ViewChange " + clusterId + "-" + newView);
        Address previousHead = clusterCoordinator;
        clusterCoordinator = newView.getCoord();

        if (newView.size() > 1) {
            if (previousHead != null &&
                    (!newView.containsMember(previousHead) || previousHead.toString().equals(mwRMNodeId))) {
                System.out.println("MW::Lost leader node of custer-" + clusterId);
                System.out.println("MW::New leader for cluster-" + clusterId + " is " + clusterCoordinator.toString());
                MWReplicationManager.reRegisterWithRegistry(rmType);
            }
        } else {
            System.out.println(mwRMNodeId + "-Currently MW is leader. Waiting for a " + clusterId);
        }
    }
}
