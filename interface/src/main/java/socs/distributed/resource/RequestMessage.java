package socs.distributed.resource;

import java.io.Serializable;

public class RequestMessage implements Serializable {
    private MsgType msgType;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }
}

