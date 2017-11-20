package LockManager;

public class InvalidTypeObjectException extends Exception {

    private static final long serialVersionUID = 1997753363232807009L;

    public InvalidTypeObjectException() {

    }

    public InvalidTypeObjectException(String message) {
        super(message);
    }

    public InvalidTypeObjectException(Throwable cause) {
        super(cause);
    }

    public InvalidTypeObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTypeObjectException(String message, Throwable cause,
                                      boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
