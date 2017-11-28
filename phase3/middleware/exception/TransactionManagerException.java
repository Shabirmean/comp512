package exception;

import Transaction.ReqStatus;

import java.io.Serializable;

/**
 * Created by shabirmean on 2017-11-12 with some hope.
 */
public class TransactionManagerException extends Exception implements Serializable {
    private static final long serialVersionUID = 1922753363232071239L;
    private ReqStatus reason;

    public TransactionManagerException() {

    }

    public TransactionManagerException(String message) {
        super(message);
    }

    public TransactionManagerException(String message, ReqStatus reason) {
        super(message);
        this.reason = reason;
    }

    public TransactionManagerException(Throwable cause) {
        super(cause);
    }

    public TransactionManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionManagerException(String message, Throwable cause,
                                       boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ReqStatus getReason() {
        return reason;
    }
}
