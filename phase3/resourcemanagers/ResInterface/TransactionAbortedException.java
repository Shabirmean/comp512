package ResInterface;


import java.io.Serializable;

public class TransactionAbortedException extends Exception implements Serializable {
    private static final long serialVersionUID = 1997753363232071239L;

    public TransactionAbortedException() {

    }

    public TransactionAbortedException(String message) {
        super(message);
    }

    public TransactionAbortedException(Throwable cause) {
        super(cause);
    }

    public TransactionAbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionAbortedException(String message, Throwable cause,
                                       boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
