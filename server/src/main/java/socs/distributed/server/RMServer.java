package socs.distributed.server;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.resource.message.MsgType;
import socs.distributed.server.util.Configuration;
import socs.distributed.server.util.RMServerConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("InfiniteLoopStatement")
class RMServer {
    private static final Log log = LogFactory.getLog(RMServer.class);
    // executor service with a thread pool to assign each incoming request to.
    private static final ExecutorService clientProcessingPool = Executors.newCachedThreadPool();
    static final RMServerManager RM_SERVER_MANAGER = new RMServerManager();
    static String rmServerRole;


    public static void main(String args[]) throws IOException {

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.dateTimeFormat", "HH:mm:ss");

        if (args.length != 1) {
            log.warn("USAGE: <JAR_FILE> <CONF_FILE_PATH>");
            System.exit(1);
        }
        initRMServer(args[0]);
    }


    private static void initRMServer(String args) {
        Configuration middlewareConfigs = new Configuration(args);
        rmServerRole = middlewareConfigs.getString(RMServerConstants.RM_SERVER_ROLE);
        String rmServerIP = middlewareConfigs.getString(RMServerConstants.RM_SERVER_IP);
        short rmPort = middlewareConfigs.getShort(RMServerConstants.RM_SERVER_PORT);

        try {
            ServerSocket rmSocket = new ServerSocket(rmPort);
            log.info("Resource Manager [" + rmServerRole.toUpperCase() + "] " +
                    "started on [" + rmServerIP + ":" + rmPort + "]");
            log.info("Waiting for middleware requests...");
            // listen for incoming connection continuously
            // for every incoming request, create a new socket and assign it to a new thread in the pool.
            while (true) {
                Socket mwClientSocket = rmSocket.accept();
                clientProcessingPool.submit(new RMServerRequestHandler(mwClientSocket));
            }
        } catch (IOException e) {
            log.error("Unable to process client requests from middleware");
            e.printStackTrace();
        }
    }

    static boolean isAllowedRequest(MsgType msgType) {
        if (rmServerRole.toUpperCase().equals(RMServerConstants.RM_FLIGHT_SERVER)) {
            return (msgType == MsgType.ADD_FLIGHT || msgType == MsgType.DELETE_FLIGHT ||
                    msgType == MsgType.QUERY_FLIGHT || msgType == MsgType.QUERY_FLIGHT_PRICE ||
                    msgType == MsgType.RESERVE_FLIGHT || msgType == MsgType.RESERVE_ITINERARY);
        } else if (rmServerRole.toUpperCase().equals(RMServerConstants.RM_CAR_SERVER)) {
            return (msgType == MsgType.ADD_CARS || msgType == MsgType.DELETE_CARS ||
                    msgType == MsgType.QUERY_CARS || msgType == MsgType.QUERY_CAR_PRICE ||
                    msgType == MsgType.RESERVE_CAR || msgType == MsgType.RESERVE_ITINERARY);
        } else if (rmServerRole.toUpperCase().equals(RMServerConstants.RM_HOTEL_SERVER)) {
            return (msgType == MsgType.ADD_ROOMS || msgType == MsgType.DELETE_ROOMS ||
                    msgType == MsgType.QUERY_ROOMS || msgType == MsgType.QUERY_ROOM_PRICE ||
                    msgType == MsgType.RESERVE_ROOM || msgType == MsgType.RESERVE_ITINERARY);
        }
        return false;
    }
}
