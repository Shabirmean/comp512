package socs.distributed.middleware;

import org.apache.log4j.Logger;
import socs.distributed.middleware.server.MiddlewareRequestHandler;
import socs.distributed.middleware.server.MiddlewareServer;

public class Bootstrap {
    private static final Logger log = Logger.getLogger(MiddlewareRequestHandler.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            log.warn("USAGE: <JAR_FILE> <CONF_FILE_PATH>");
            System.exit(1);
        }

        MiddlewareServer comp512MiddlewareServer = new MiddlewareServer();
        comp512MiddlewareServer.initMiddlewareServer(args[0]);
    }
}
