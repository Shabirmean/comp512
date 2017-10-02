package socs.distributed.client;

import org.apache.log4j.Logger;
import socs.distributed.client.exception.ClientException;
import socs.distributed.client.util.ClientUtils;
import socs.distributed.resource.message.MsgType;
import socs.distributed.resource.message.RequestMessage;
import socs.distributed.resource.message.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

class ClientRequestHandler {
    private static final Logger log = Logger.getLogger(ClientRequestHandler.class);
    private String clientID;
    private String middlewareIP;
    private short middlewarePort;

    ClientRequestHandler(String clientID, String middlewareIP, short middlewarePort) {
        this.clientID = clientID;
        this.middlewareIP = middlewareIP;
        this.middlewarePort = middlewarePort;
    }

    void sendRequestToMiddleware(MsgType msgType, Vector arguments) throws ClientException {
        Socket clientSocket;
        ObjectOutputStream socketWriter = null;
        ObjectInputStream socketReader = null;
        RequestMessage clientReqMsg = new RequestMessage();
        clientReqMsg.setClientID(this.clientID);
        clientReqMsg.setMsgType(msgType);
        clientReqMsg.setMessage("A message of type: " + msgType.getMsgCode() + " from client [" + clientID + "]");
        clientReqMsg.setMethodArguments(arguments);

        try {
            clientSocket = new Socket(middlewareIP, middlewarePort);
        } catch (IOException e) {
            String errMsg = "An error occurred whilst trying to establish Socket connection to " +
                    "MIDDLEWARE [" + middlewareIP + "] at PORT [" + middlewarePort + "]";
            log.error(errMsg);
            throw new ClientException(errMsg, e);
        }

        try {
            socketWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            socketReader = new ObjectInputStream(clientSocket.getInputStream());

            socketWriter.writeObject(clientReqMsg);
            ResponseMessage responseFromMiddleware = (ResponseMessage) socketReader.readObject();

            if (responseFromMiddleware.getStatus() == MsgType.MessageStatus.RM_SERVER_FAIL_STATUS) {
                log.error(responseFromMiddleware.getMessage());
                responseFromMiddleware.getException().printStackTrace();
            }
            log.info("\n***************" + responseFromMiddleware.getMessage());

        } catch (IOException e) {
            log.error("An error occurred whilst trying to READ/WRITE from socket to middleware server");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log.error("Object type received over the socket connection was not [ResponseMessage]");
            e.printStackTrace();
        } finally {
            ClientUtils.releaseSocket(clientSocket);
            ClientUtils.releaseWriter(socketWriter);
            ClientUtils.releaseReader(socketReader);
        }
    }
}