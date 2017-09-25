package socs.distributed.middleware.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.middleware.exception.MiddlewareException;
import socs.distributed.middleware.util.MiddlewareUtils;
import socs.distributed.resource.exception.COMP512Exception;
import socs.distributed.resource.message.MsgType;
import socs.distributed.resource.message.RequestMessage;
import socs.distributed.resource.message.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;


/**
 * An class which holds all the methods related to handling a new incoming request. Implements "Runnable"
 * whose run method is called by the parent class upon receiving a new incoming request. Consists of separate
 * methods to handle different types of incoming requests.
 */
@SuppressWarnings("Duplicates")
public class MiddlewareRequestHandler implements Runnable {
    private final Log log = LogFactory.getLog(MiddlewareRequestHandler.class);
    // the unique socket allocated for this new request instance via which future communications happen.
    private final Socket clientSocket;
    private String clientHostName;

    MiddlewareRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientHostName = clientSocket.getInetAddress().getHostName();
    }

    /**
     * Overridden "run" method from Runnable that initiates new-request handling procedures. Sets up the socket
     * reader and writer objects and invokes appropriate method according to the incoming message type.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        ResponseMessage responseToClient = new ResponseMessage();
        ObjectOutputStream socketWriter;
        ObjectInputStream socketReader;
        int id, customerId;

        try {
            log.info("Received request from client: " + clientHostName);
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
            Vector msgArgs = requestMsgFromClient.getMethodArguments();

            if (MiddlewareServer.isRequestForMW(requestMsgType)) {
                switch (requestMsgType) {
                    case ADD_NEW_CUSTOMER_WITHOUT_ID:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = MiddlewareServer.internalResourceManager.newCustomer(id);

                        log.info("New customer added with ID:" + customerId);
                        responseToClient.setMessage("A new flight was successfully added");
                        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;

                    case ADD_NEW_CUSTOMER_WITH_ID:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));

                        if (MiddlewareServer.internalResourceManager.newCustomer(id, customerId)) {
                            log.info("New customer added with ID:" + customerId);
                            responseToClient.setMessage("New customer added with ID:" + customerId);
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("New customer could not be added with ID:" + customerId);
                            COMP512Exception exception = new COMP512Exception("New customer could not be added");
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setException(exception);
                            responseToClient.setMessage("New customer could not be added with ID:" + customerId);
                        }
                        break;

                    case DELETE_CUSTOMER:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));
                        if (MiddlewareServer.internalResourceManager.newCustomer(id, customerId)) {
                            log.info("Customer deleted. ID:" + customerId);
                            responseToClient.setMessage("Customer deleted. ID:" + customerId);
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Customer could not be deleted. ID:" + customerId);
                            COMP512Exception exception = new COMP512Exception("Customer could not be deleted.");
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setException(exception);
                            responseToClient.setMessage("Customer could not be deleted. ID:" + customerId);
                        }
                        break;

                    case QUERY_CUSTOMER_INFO:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));
                        String cusBill = MiddlewareServer.internalResourceManager.queryCustomerInfo(id, customerId);

                        log.info("Customer info:" + cusBill);
                        responseToClient.setMessage("Customer info:" + cusBill);
                        responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        break;

                    case RESERVE_ITINERARY:
                        id = this.getInt(msgArgs.elementAt(1));
                        customerId = this.getInt(msgArgs.elementAt(2));

                        Vector flightNumbers = new Vector();
                        for (int i = 0; i < msgArgs.size() - 6; i++)
                            flightNumbers.addElement(msgArgs.elementAt(3 + i));
                        String location = this.getString(msgArgs.elementAt(msgArgs.size() - 3));
                        boolean bookCars = this.getBoolean(msgArgs.elementAt(msgArgs.size() - 2));
                        boolean bookRooms = this.getBoolean(msgArgs.elementAt(msgArgs.size() - 1));

                        if (MiddlewareServer.internalResourceManager.
                                itinerary(id, customerId, flightNumbers, location, bookCars, bookRooms)) {
                            log.info("Itinerary Reserved");
                            responseToClient.setMessage("Itinerary Reserved");
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_SUCCESS_STATUS);
                        } else {
                            log.info("Itinerary could not be reserved.");
                            COMP512Exception exception = new COMP512Exception("Itinerary could not be reserved.");
                            responseToClient.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
                            responseToClient.setException(exception);
                            responseToClient.setMessage("Itinerary could not be reserved.");
                        }
                        break;
                }
            } else {
                MWResourceManager.RM_Type rmTypeOfRequest =
                        MWResourceManager.getRMCorrespondingToRequest(requestMsgType);
                MWResourceManager mwResourceManager = MiddlewareServer.externalResourceManagers.get(rmTypeOfRequest);
                responseToClient = contactResourceManager(mwResourceManager, requestMsgFromClient);
                socketWriter.writeObject(responseToClient);
            }

        } catch (IOException e) {
            log.error("An IO error occurred whilst trying to READ [SOSPFPacket] object from socket stream.", e);
        } catch (ClassNotFoundException e) {
            log.error("An object type other than [SOSPFPacket] was recieved over the socket connection", e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
    }

    @SuppressWarnings({"StatementWithEmptyBody", "InfiniteLoopStatement"})
    private ResponseMessage contactResourceManager(MWResourceManager mwResourceManager,
                                                   RequestMessage requestMsgFromClient) throws MiddlewareException {
        ResponseMessage responseFromRMServer;
        String rmIP = mwResourceManager.getRmIP();
        short rmPort = mwResourceManager.getRmPort();

        Socket clientSocket;
        ObjectOutputStream socketWriter = null;
        ObjectInputStream socketReader = null;

        try {
            clientSocket = new Socket(rmIP, rmPort);
            log.info("Connected to [" + mwResourceManager.getType() + "] resource-manager at " +
                    "[" + rmIP + ":" + rmPort + "] to serve request from client: " + clientHostName);
        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to establish Socket connection to " +
                    "resource-manager at [" + rmIP + ":" + rmPort + "]";
            log.error(errMsg);
            throw new MiddlewareException(errMsg, e);
        }

        try {
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());
            socketWriter.writeObject(requestMsgFromClient);

            responseFromRMServer = (ResponseMessage) socketReader.readObject();
            int responseStatus = responseFromRMServer.getStatus();
            if (responseStatus == 0) {
                log.error("There was an error when processing request from [" + clientHostName + "]");
            } else {
                log.info("Resource manager replied successfully for request from [" + clientHostName + "]");
            }

        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to READ/WRITE from socket to RM server";
            responseFromRMServer = new ResponseMessage();
            responseFromRMServer.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
            responseFromRMServer.setMessage(errMsg);
            responseFromRMServer.setException(new COMP512Exception(errMsg));
            log.error(errMsg);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            String errMsg = "Object type received from RM was not [ResponseMessage]";
            responseFromRMServer = new ResponseMessage();
            responseFromRMServer.setStatus(MsgType.MessageStatus.RM_SERVER_FAIL_STATUS);
            responseFromRMServer.setMessage(errMsg);
            responseFromRMServer.setException(new COMP512Exception(errMsg));
            log.error(errMsg);
            e.printStackTrace();
        } finally {
            MiddlewareUtils.releaseSocket(clientSocket);
            MiddlewareUtils.releaseWriter(socketWriter);
            MiddlewareUtils.releaseReader(socketReader);
        }
        return responseFromRMServer;
    }


    private int getInt(Object temp) throws Exception {
        return new Integer((String) temp);
    }

    private String getString(Object temp) throws Exception {
        return (String) temp;
    }

    private boolean getBoolean(Object temp) throws Exception {
        return Boolean.valueOf((String) temp);
    }

}
