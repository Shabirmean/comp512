package socs.distributed.middleware.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.middleware.util.Configuration;
import socs.distributed.middleware.util.MiddlewareConstants;
import socs.distributed.middleware.util.MiddlewareUtils;
import socs.distributed.resource.MsgType;
import socs.distributed.resource.RequestMessage;
import socs.distributed.resource.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiddlewareServer {
    private final Log log = LogFactory.getLog(MiddlewareServer.class);

    private short middlewarePort;
    private HashMap<ResourceManager.RM_Type, ResourceManager> resourceManagers;
    // executor service with a thread pool to assign each incoming request to.
    private final ExecutorService clientProcessingPool =
            Executors.newFixedThreadPool(MiddlewareConstants.SERVER_THREAD_POOL_COUNT);

    public MiddlewareServer() {
        this.middlewarePort = 22000;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void initMiddlewareServer(String args) {
        Configuration middlewareConfigs = new Configuration(args);
        middlewarePort = middlewareConfigs.getShort(MiddlewareConstants.MIDDLEWARE_PORT);
        initializeResourceManagers(middlewareConfigs);

        try {
            ServerSocket middlewareSocket = new ServerSocket(middlewarePort);
            log.info("The ShaJian middleware is listening on PORT: " + middlewarePort);
            log.info("Waiting for clients to connect...");

            // listen for incoming connection continuously
            // for every incoming request, create a new socket and assign it to a new thread in the pool.
            while (true) {
                Socket clientSocket = middlewareSocket.accept();
                clientProcessingPool.submit(new ClientRequest(clientSocket));
            }
        } catch (IOException e) {
            log.error("Unable to process client request");
            e.printStackTrace();
        }
    }

    private void initializeResourceManagers(Configuration middlewareConfigs) {
        String flightsRM_IP = middlewareConfigs.getString(MiddlewareConstants.FLIGHTS_MANAGER_IP);
        short flightsRM_Port = middlewareConfigs.getShort(MiddlewareConstants.FLIGHTS_MANAGER_PORT);
        String carsRM_IP = middlewareConfigs.getString(MiddlewareConstants.CARS_MANAGER_IP);
        short carsRM_Port = middlewareConfigs.getShort(MiddlewareConstants.CARS_MANAGER_PORT);
        String roomsRM_IP = middlewareConfigs.getString(MiddlewareConstants.ROOMS_MANAGER_IP);
        short roomsRM_Port = middlewareConfigs.getShort(MiddlewareConstants.ROOMS_MANAGER_PORT);

        resourceManagers.put(ResourceManager.RM_Type.FLIGHTS,
                new ResourceManager(ResourceManager.RM_Type.FLIGHTS, flightsRM_IP, flightsRM_Port));
        resourceManagers.put(ResourceManager.RM_Type.CARS,
                new ResourceManager(ResourceManager.RM_Type.CARS, carsRM_IP, carsRM_Port));
        resourceManagers.put(ResourceManager.RM_Type.ROOMS,
                new ResourceManager(ResourceManager.RM_Type.ROOMS, roomsRM_IP, roomsRM_Port));
    }
}
