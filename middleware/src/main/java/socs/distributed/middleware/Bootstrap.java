package socs.distributed.middleware;

import socs.distributed.middleware.server.MiddlewareServer;

public class Bootstrap {

    public static void main(String[] args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.dateTimeFormat", "HH:mm:ss");

        MiddlewareServer comp512MiddlewareServer = new MiddlewareServer();
        comp512MiddlewareServer.initMiddlewareServer();
    }
}
