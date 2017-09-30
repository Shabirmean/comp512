package socs.distributed.client.exception;


public class ClientException extends Exception{
    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
