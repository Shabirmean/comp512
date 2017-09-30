package socs.distributed.middleware.util;

import org.apache.log4j.Logger;
import socs.distributed.middleware.server.MiddlewareRequestHandler;

import java.io.*;
import java.net.Socket;

public class MiddlewareUtils {
    private static final Logger log = Logger.getLogger(MiddlewareRequestHandler.class);

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
