package socs.distributed.resource.message;

import socs.distributed.resource.dto.ReservableItem;

import java.io.Serializable;
import java.util.Vector;

public class RequestMessage implements Serializable {
    private static final long serialVersionUID = 7526472295622776109L;

    private MsgType msgType;
    private String message;
    private Vector methodArguments;

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Vector getMethodArguments() {
        return methodArguments;
    }

    public void setMethodArguments(Vector methodArguments) {
        this.methodArguments = methodArguments;
    }
}

