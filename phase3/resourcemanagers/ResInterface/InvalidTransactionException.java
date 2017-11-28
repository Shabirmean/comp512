package ResInterface;

import java.io.Serializable;

public class InvalidTransactionException extends Exception implements Serializable {
    private static final long serialVersionUID = 1923753363232071239L;

    public InvalidTransactionException() {

    }

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(Throwable cause) {
        super(cause);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTransactionException(String message, Throwable cause,
                                       boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
