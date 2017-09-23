package socs.distributed.server;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.server.util.Configuration;
import socs.distributed.server.util.RMServerConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RMServer {
    private final Log log = LogFactory.getLog(RMServer.class);
    // executor service with a thread pool to assign each incoming request to.
    private final ExecutorService clientProcessingPool = Executors.newCachedThreadPool();
//            Executors.newFixedThreadPool(RMServerConstants.SERVER_THREAD_POOL_COUNT);
    private String rmServerRole;
    private String rmServerIP;
    private short rmPort;

    void initRMServer(String args) throws IOException {
        Configuration middlewareConfigs = new Configuration(args);
        rmServerRole = middlewareConfigs.getString(RMServerConstants.RM_SERVER_ROLE);
        rmServerIP = middlewareConfigs.getString(RMServerConstants.RM_SERVER_IP);
        rmPort = middlewareConfigs.getShort(RMServerConstants.RM_SERVER_PORT);

        try {
            ServerSocket rmSocket = new ServerSocket(rmPort);
            log.info("Resource manager [" + rmServerRole + "] started on [" + rmServerIP + ":" + rmPort + "]");
            log.info("Waiting for middleware requests...");
            // listen for incoming connection continuously
            // for every incoming request, create a new socket and assign it to a new thread in the pool.
            while (true) {
                Socket clientSocket = rmSocket.accept();
                clientProcessingPool.submit(new RMServerRequestHandler(clientSocket));
            }
        } catch (IOException e) {
            log.error("Unable to process client requests from middleware");
            e.printStackTrace();
        }
    }
}
