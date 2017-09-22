package socs.distributed.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.client.exception.ClientException;
import socs.distributed.client.util.ClientUtils;
import socs.distributed.resource.MsgType;
import socs.distributed.resource.RequestMessage;
import socs.distributed.resource.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

class ClientRequestHandler {
    private final Log log = LogFactory.getLog(ClientRequestHandler.class);
    private String middlewareIP;
    private short middlewarePort;

    ClientRequestHandler(String middlewareIP, short middlewarePort) {
        this.middlewareIP = middlewareIP;
        this.middlewarePort = middlewarePort;
    }

    void sendRequestToMiddleware(MsgType msgType, Vector arguments) throws ClientException {
        Socket clientSocket;
        ObjectOutputStream socketWriter = null;
        ObjectInputStream socketReader = null;
        RequestMessage clientReqMsg = new RequestMessage();
        clientReqMsg.setMsgType(msgType);
        clientReqMsg.setMessage("Just a HELLO for type: " + msgType.getMsgCode());
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
            System.out.println(responseFromMiddleware.getMessage());

        } catch (IOException e) {
            System.out.println("An error occurred whilst trying to READ/WRITE from socket to middleware server");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Object type received over the socket connection was not [ResponseMessage]");
            e.printStackTrace();
        } finally {
            ClientUtils.releaseSocket(clientSocket);
            ClientUtils.releaseWriter(socketWriter);
            ClientUtils.releaseReader(socketReader);
        }
    }
}