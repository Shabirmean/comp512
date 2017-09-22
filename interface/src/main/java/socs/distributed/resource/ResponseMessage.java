package socs.distributed.resource;

import java.io.Serializable;

public class ResponseMessage implements Serializable {
    private static final long serialVersionUID = 7526472295622776147L;

    private int status; // 0 means failure and 1 means success
    private String message;
    private COMP512Exception exception;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public COMP512Exception getException() {
        return exception;
    }

    public void setException(COMP512Exception exception) {
        this.exception = exception;
    }
}
