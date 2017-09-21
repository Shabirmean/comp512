package socs.distributed.resource;

import java.io.Serializable;

public class ResponseMessage implements Serializable {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
