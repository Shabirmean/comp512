package socs.distributed.resource;

import java.io.Serializable;

public class COMP512Exception extends Exception implements Serializable {
    private static final long serialVersionUID = 7526472295622776144L;

    public COMP512Exception() {
    }

    public COMP512Exception(String message) {
        super(message);
    }

    public COMP512Exception(String message, Throwable throwable) {
        super(message, throwable);
    }
}
