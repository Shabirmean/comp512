// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package socs.distributed.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import socs.distributed.resource.dto.RMHashtable;

import java.io.IOException;
import java.rmi.RemoteException;

public class ResourceManagerImpl {
    private static final Log log = LogFactory.getLog(ResourceManagerImpl.class);
    protected RMHashtable m_itemHT = new RMHashtable();

    public ResourceManagerImpl() throws RemoteException {
    }

    public static void main(String args[]) throws IOException {

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.dateTimeFormat", "HH:mm:ss");

        if (args.length != 1) {
            log.warn("USAGE: <JAR_FILE> <CONF_FILE_PATH>");
            System.exit(1);
        }

        RMServer comp512RMServer = new RMServer();
        comp512RMServer.initRMServer(args[0]);
    }
}