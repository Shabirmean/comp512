package socs.distributed.client.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.Socket;

public class ClientUtils {
    private static final Log log = LogFactory.getLog(ClientUtils.class);

    public static void releaseSocket(Socket socket) {
        if (socket != null) {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
//                log.error("Error occurred when attempted to close socket.");
            }
        }
    }

    public static void releaseWriter(PrintWriter socketWriter) {
        if (socketWriter != null) {
            socketWriter.flush();
            socketWriter.close();
        }
    }

    public static void releaseWriter(ObjectOutputStream socketWriter) {
        if (socketWriter != null) {
            try {
                socketWriter.flush();
                socketWriter.close();
            } catch (IOException e) {
//                log.error("Error occurred when attempted to close ObjectOutputStream of socket.");
            }
        }
    }

    public static void releaseReader(BufferedReader socketReader) {
        if (socketReader != null) {
            try {
                socketReader.close();
            } catch (IOException e) {
//                log.error("Error occurred when attempted to close BufferedReader of socket.");
            }
        }

    }

    public static void releaseReader(ObjectInputStream socketReader) {
        if (socketReader != null) {
            try {
                socketReader.close();
            } catch (IOException e) {
//                log.error("Error occurred when attempted to close ObjectInputStream of socket.");
            }
        }

    }
}
