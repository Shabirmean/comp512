package socs.distributed.middleware;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.middleware.server.MiddlewareServer;

public class Bootstrap {
    private static final Log log = LogFactory.getLog(Bootstrap.class);

    public static void main(String[] args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.dateTimeFormat", "HH:mm:ss");

        if (args.length != 1) {
            log.warn("USAGE: <JAR_FILE> <CONF_FILE_PATH>");
            System.exit(1);
        }

        MiddlewareServer comp512MiddlewareServer = new MiddlewareServer();
        comp512MiddlewareServer.initMiddlewareServer(args[0]);
    }
}
