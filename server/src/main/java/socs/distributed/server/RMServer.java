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
    private final Log log = LogFactory.getLog(RMServer.class);
    // executor service with a thread pool to assign each incoming request to.
    private final ExecutorService clientProcessingPool = Executors.newCachedThreadPool();
    static final ResourceManager resourceManager = new ResourceManager();
    static String rmServerRole;


    void initRMServer(String args) throws IOException {
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
                    msgType == MsgType.RESERVE_FLIGHT);
        } else if (rmServerRole.toUpperCase().equals(RMServerConstants.RM_CAR_SERVER)) {
            return (msgType == MsgType.ADD_CARS || msgType == MsgType.DELETE_CARS ||
                    msgType == MsgType.QUERY_CARS || msgType == MsgType.QUERY_CAR_PRICE ||
                    msgType == MsgType.RESERVE_CAR);
        } else if (rmServerRole.toUpperCase().equals(RMServerConstants.RM_HOTEL_SERVER)) {
            return (msgType == MsgType.ADD_ROOMS || msgType == MsgType.DELETE_ROOMS ||
                    msgType == MsgType.QUERY_ROOMS || msgType == MsgType.QUERY_ROOM_PRICE ||
                    msgType == MsgType.RESERVE_ROOM);
        }
        return false;
    }
}
