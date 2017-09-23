package socs.distributed.middleware.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.middleware.exception.MiddlewareException;
import socs.distributed.middleware.util.MiddlewareUtils;
import socs.distributed.resource.MsgType;
import socs.distributed.resource.RequestMessage;
import socs.distributed.resource.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * An class which holds all the methods related to handling a new incoming request. Implements "Runnable"
 * whose run method is called by the parent class upon receiving a new incoming request. Consists of separate
 * methods to handle different types of incoming requests.
 */
@SuppressWarnings("Duplicates")
public class MiddlewareRequestHandler implements Runnable{
    private final Log log = LogFactory.getLog(MiddlewareRequestHandler.class);
    // the unique socket allocated for this new request instance via which future communications happen.
    private final Socket clientSocket;

    MiddlewareRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Overridden "run" method from Runnable that initiates new-request handling procedures. Sets up the socket
     * reader and writer objects and invokes appropriate method according to the incoming message type.
     */
    @Override
    public void run() {
        ObjectOutputStream socketWriter;
        ObjectInputStream socketReader;
        try {
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to open Input/Output stream on the " +
                    "client socket connection for READ/WRITE.");
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
                    responseToClient.setMessage(contactResourceManager(requestMsgFromClient));
                    break;
                case ADD_CARS:
                    System.out.println("ADD_CARS :" + requestMsgFromClient.getMessage());
                    responseToClient.setMessage(contactResourceManager(requestMsgFromClient));
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
                case QUERY_CAR_PRICE:
                    System.out.println("QUERY_CAR_PRICE :" + requestMsgFromClient.getMessage());
                    break;

                case QUERY_ROOM_PRICE:
                    System.out.println("QUERY_ROOM_PRICE :" + requestMsgFromClient.getMessage());
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
        } catch (MiddlewareException e) {
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
    }


    @SuppressWarnings({"StatementWithEmptyBody", "InfiniteLoopStatement"})
    private String contactResourceManager(RequestMessage requestMsgFromClient) throws
            MiddlewareException {
        String responseString = "NOTHING";
        Socket clientSocket;
        ObjectOutputStream socketWriter = null;
        ObjectInputStream socketReader = null;

        try {
            clientSocket = new Socket("cs-27.cs.mcgill.ca", 22000);
        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to establish Socket connection to " +
                    "MIDDLEWARE [" + "cs-27.cs.mcgill.ca" + "] at PORT [" + 22000 + "]";
            log.error(errMsg);
            throw new MiddlewareException(errMsg, e);
        }

        try {
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());
            socketWriter.writeObject(requestMsgFromClient);
            ResponseMessage responseFromRMServer = (ResponseMessage) socketReader.readObject();
            responseString = responseFromRMServer.getMessage();

            log.info("RM Server responded with: " + responseFromRMServer);
        } catch (IOException e) {
            System.out.println("An error occurred whilst trying to READ/WRITE from socket to middleware server");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Object type received over the socket connection was not [ResponseMessage]");
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
        return responseString;
    }

}
