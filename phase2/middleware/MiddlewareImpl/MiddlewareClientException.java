package MiddlewareImpl;


public class MiddlewareClientException extends Exception{
    private static final long serialVersionUID = 1923343363232071239L;

    public MiddlewareClientException() {

    }

    public MiddlewareClientException(String message) {
        super(message);
    }

    public MiddlewareClientException(Throwable cause) {
        super(cause);
    }

    public MiddlewareClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MiddlewareClientException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
