// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------
package socs.distributed.resource.util;

import org.apache.log4j.Logger;

// A simple wrapper around log.info, allows us to disable some
//  of the verbose output from RM, TM, and WC if we want
public class Trace {
    private static final Logger log = Logger.getLogger(Trace.class);
    public static void info(String msg) {
        log.info(getThreadID() + " INFO: " + msg);
    }

    public static void warn(String msg) {
        log.info(getThreadID() + " WARN: " + msg);
    }

    public static void error(String msg) {
        System.err.println(getThreadID() + " ERROR: " + msg);
    }

    private static String getThreadID() {
        String s = Thread.currentThread().getName();
        // shorten...
        // "RMI TCP Connection(x)-hostname/99.99.99.99"
        // to
        // "RMI TCP Cx(x)"
        if (s.startsWith("RMI TCP Connection(")) {
            return "RMI Cx" + s.substring(s.indexOf('('), s.indexOf(')')) + ")";
        } // if
        return s;
    }
}
