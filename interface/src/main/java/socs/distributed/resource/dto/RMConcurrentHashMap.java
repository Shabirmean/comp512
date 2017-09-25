package socs.distributed.resource.dto;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class RMConcurrentHashMap extends ConcurrentHashMap {
    public RMConcurrentHashMap() {
        super();
    }

    public String toString()
    {
        String s = "--- BEGIN RMHashtable ---\n";
        Object key = null;
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            key = e.nextElement();
            String value = (String)get( key );
            s = s + "[KEY='"+key+"']" + value + "\n";
        }
        s = s + "--- END RMHashtable ---";
        return s;
    }

    public void dump()
    {
        System.out.println( toString() );
    }

}
