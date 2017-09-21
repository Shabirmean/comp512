package socs.distributed.middleware.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiddlewareServer {
    private final Log log = LogFactory.getLog(MiddlewareServer.class);

    private ServerSocket middlewareSocket;
    // the port of this ServerSocket.
    private short middlewarePort;
    // executor service with a thread pool to assign each incoming request to.
    private final ExecutorService clientProcessingPool =
            Executors.newFixedThreadPool(MiddlewareConstants.SERVER_THREAD_POOL_COUNT);

    public MiddlewareServer() {
    }


    public void initMiddlewareServer() {
//        Runnable serverTask = new Runnable() {
//            @Override
//            public void run() {
        try {
            middlewareSocket = new ServerSocket(9898);
            middlewarePort = (short) middlewareSocket.getLocalPort();

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
//    }
//        };
//        Thread serverThread = new Thread(serverTask);
//        serverThread.start();
    }


// ---------------------------------------------------------------------------------------------------------------

    /**
     * An inner class which holds all the methods related to handling a new incoming request. Implements "Runnable"
     * whose run method is called by the parent class upon receiving a new incoming request. Consists of separate
     * methods to handle different types of incoming requests.
     */
    private class ClientRequest implements Runnable {
        // the unique socket allocated for this new request instance via which future communications happen.
        private final Socket clientSocket;
        // the OutputStream used to write messages out into the socket.
        private ObjectOutputStream socketWriter;
        // the InputStream from which incoming messages are read from.
        private ObjectInputStream socketReader;

        private ClientRequest(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        /**
         * Overridden "run" method from Runnable that initiates new-request handling procedures. Sets up the socket
         * reader and writer objects and invokes appropriate method according to the incoming message type.
         */
        @Override
        public void run() {
            try {
                this.socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
                this.socketReader = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                log.error("An IO error occurred whilst trying to open Input/Output stream on the " +
                        "socket connection for READ/WRITE.");
                return;
            }

            try {
                RequestMessage requestMsgFromClient = (RequestMessage) socketReader.readObject();
                MsgType requestMsgType = requestMsgFromClient.getMsgType();

                ResponseMessage responseToClient = new ResponseMessage();
                responseToClient.setMessage("BLING!!! IT WORKED");

                switch (requestMsgType) {
                    case ADD_FLIGHT:
                        System.out.println("ADD_FLIGHT :" + requestMsgFromClient.getMessage());
                        break;
                    case ADD_CARS:
                        System.out.println("ADD_CARS :" + requestMsgFromClient.getMessage());
                        break;
                    case ADD_ROOMS:
                        System.out.println("ADD_ROOMS :" + requestMsgFromClient.getMessage());
                        break;
                    case ADD_NEW_CUSTOMER_WITHOUT_ID:
                        System.out.println("ADD_NEW_CUSTOMER_WITHOUT_ID :" + requestMsgFromClient.getMessage());
                        break;
                    case ADD_NEW_CUSTOMER_WITH_ID:
                        System.out.println("ADD_NEW_CUSTOMER_WITH_ID");
                        break;
                    case DELETE_FLIGHT:
                        System.out.println("DELETE_FLIGHT :" + requestMsgFromClient.getMessage());
                        break;
                    case DELETE_CARS:
                        System.out.println("DELETE_CARS :" + requestMsgFromClient.getMessage());
                        break;
                    case DELETE_ROOMS:
                        System.out.println("DELETE_ROOMS :" + requestMsgFromClient.getMessage());
                        break;
                    case DELETE_CUSTOMER:
                        System.out.println("DELETE_CUSTOMER :" + requestMsgFromClient.getMessage());
                        break;
                    case QUERY_FLIGHT:
                        System.out.println("QUERY_FLIGHT :" + requestMsgFromClient.getMessage());
                        break;

                    case QUERY_CARS:
                        System.out.println("QUERY_CARS :" + requestMsgFromClient.getMessage());
                        break;
                    case QUERY_ROOMS:
                        System.out.println("QUERY_ROOMS :" + requestMsgFromClient.getMessage());
                        break;
                    case QUERY_CUSTOMER_INFO:
                        System.out.println("QUERY_CUSTOMER_INFO :" + requestMsgFromClient.getMessage());
                        break;
                    case QUERY_FLIGHT_PRICE:
                        System.out.println("QUERY_FLIGHT_PRICE :" + requestMsgFromClient.getMessage());
                        break;
                    case QUERY_CARS_PRICE:
                        System.out.println("QUERY_CARS_PRICE :" + requestMsgFromClient.getMessage());
                        break;

                    case QUERY_ROOMS_PRICE:
                        System.out.println("QUERY_ROOMS_PRICE :" + requestMsgFromClient.getMessage());
                        break;
                    case RESERVE_FLIGHT:
                        System.out.println("RESERVE_FLIGHT :" + requestMsgFromClient.getMessage());
                        break;
                    case RESERVE_CAR:
                        System.out.println("RESERVE_CAR :" + requestMsgFromClient.getMessage());
                        break;
                    case RESERVE_ROOM:
                        System.out.println("RESERVE_ROOM :" + requestMsgFromClient.getMessage());
                        break;
                    case RESERVE_ITINERARY:
                        System.out.println("RESERVE_ITINERARY :" + requestMsgFromClient.getMessage());
                        break;
                }

                socketWriter.writeObject(responseToClient);

            } catch (IOException e) {
                log.error("An IO error occurred whilst trying to READ [SOSPFPacket] object from socket stream.", e);
            } catch (ClassNotFoundException e) {
                log.error("An object type other than [SOSPFPacket] was recieved over the socket connection", e);
            } finally {
                MiddlewareUtils.releaseSocket(clientSocket);
                MiddlewareUtils.releaseWriter(socketWriter);
                MiddlewareUtils.releaseReader(socketReader);
            }
        }
    }

}
