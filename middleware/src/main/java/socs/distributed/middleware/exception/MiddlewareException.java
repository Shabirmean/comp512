package socs.distributed.middleware.exception;


public class MiddlewareException extends Exception{
    public MiddlewareException() {
    }

    public MiddlewareException(String message) {
        super(message);
    }

    public MiddlewareException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
