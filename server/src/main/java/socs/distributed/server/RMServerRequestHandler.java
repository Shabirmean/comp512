package socs.distributed.server;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.resource.MsgType;
import socs.distributed.resource.RequestMessage;
import socs.distributed.resource.ResponseMessage;
import socs.distributed.server.util.RMServerUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

@SuppressWarnings("Duplicates")
public class RMServerRequestHandler implements Runnable {
    private final Log log = LogFactory.getLog(RMServerRequestHandler.class);
    // the unique socket allocated for this new request instance via which future communications happen.
    private final Socket middlewareClientSocket;

    RMServerRequestHandler(Socket middlewareClientSocket) {
        this.middlewareClientSocket = middlewareClientSocket;
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
            socketWriter = new ObjectOutputStream(middlewareClientSocket.getOutputStream());
            socketReader = new ObjectInputStream(middlewareClientSocket.getInputStream());
        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to open Input/Output stream on the " +
                    "client socket connection for READ/WRITE.");
            return;
        }

        try {
            RequestMessage requestMsgFromMW = (RequestMessage) socketReader.readObject();
            MsgType requestMsgType = requestMsgFromMW.getMsgType();

            ResponseMessage responseToMW = new ResponseMessage();
            responseToMW.setMessage("BLING!!! IT WORKED");

            switch (requestMsgType) {
                case ADD_FLIGHT:
                    System.out.println("ADD_FLIGHT :" + requestMsgFromMW.getMessage());
                    Thread.sleep(60000);
                    responseToMW.setMessage("Response to C1");
                    break;
                case ADD_CARS:
                    System.out.println("ADD_CARS :" + requestMsgFromMW.getMessage());
                    responseToMW.setMessage("Response to C2");
                    break;
                case ADD_ROOMS:
                    System.out.println("ADD_ROOMS :" + requestMsgFromMW.getMessage());
                    break;
                case ADD_NEW_CUSTOMER_WITHOUT_ID:
                    System.out.println("ADD_NEW_CUSTOMER_WITHOUT_ID :" + requestMsgFromMW.getMessage());
                    break;
                case ADD_NEW_CUSTOMER_WITH_ID:
                    System.out.println("ADD_NEW_CUSTOMER_WITH_ID");
                    break;
                case DELETE_FLIGHT:
                    System.out.println("DELETE_FLIGHT :" + requestMsgFromMW.getMessage());
                    break;
                case DELETE_CARS:
                    System.out.println("DELETE_CARS :" + requestMsgFromMW.getMessage());
                    break;
                case DELETE_ROOMS:
                    System.out.println("DELETE_ROOMS :" + requestMsgFromMW.getMessage());
                    break;
                case DELETE_CUSTOMER:
                    System.out.println("DELETE_CUSTOMER :" + requestMsgFromMW.getMessage());
                    break;
                case QUERY_FLIGHT:
                    System.out.println("QUERY_FLIGHT :" + requestMsgFromMW.getMessage());
                    break;

                case QUERY_CARS:
                    System.out.println("QUERY_CARS :" + requestMsgFromMW.getMessage());
                    break;
                case QUERY_ROOMS:
                    System.out.println("QUERY_ROOMS :" + requestMsgFromMW.getMessage());
                    break;
                case QUERY_CUSTOMER_INFO:
                    System.out.println("QUERY_CUSTOMER_INFO :" + requestMsgFromMW.getMessage());
                    break;
                case QUERY_FLIGHT_PRICE:
                    System.out.println("QUERY_FLIGHT_PRICE :" + requestMsgFromMW.getMessage());
                    break;
                case QUERY_CAR_PRICE:
                    System.out.println("QUERY_CAR_PRICE :" + requestMsgFromMW.getMessage());
                    break;

                case QUERY_ROOM_PRICE:
                    System.out.println("QUERY_ROOM_PRICE :" + requestMsgFromMW.getMessage());
                    break;
                case RESERVE_FLIGHT:
                    System.out.println("RESERVE_FLIGHT :" + requestMsgFromMW.getMessage());
                    break;
                case RESERVE_CAR:
                    System.out.println("RESERVE_CAR :" + requestMsgFromMW.getMessage());
                    break;
                case RESERVE_ROOM:
                    System.out.println("RESERVE_ROOM :" + requestMsgFromMW.getMessage());
                    break;
                case RESERVE_ITINERARY:
                    System.out.println("RESERVE_ITINERARY :" + requestMsgFromMW.getMessage());
                    break;
            }

            socketWriter.writeObject(responseToMW);

        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to READ [SOSPFPacket] object from socket stream.", e);
        } catch (ClassNotFoundException e) {
            log.error("An object type other than [SOSPFPacket] was recieved over the socket connection", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            RMServerUtils.releaseSocket(middlewareClientSocket);
            RMServerUtils.releaseWriter(socketWriter);
            RMServerUtils.releaseReader(socketReader);
        }
    }
}
