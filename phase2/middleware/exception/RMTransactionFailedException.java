package exception;


public class RMTransactionFailedException extends Exception{
    private static final long serialVersionUID = 1923739363232071239L;

    public RMTransactionFailedException() {

    }

    public RMTransactionFailedException(String message) {
        super(message);
    }

    public RMTransactionFailedException(Throwable cause) {
        super(cause);
    }

    public RMTransactionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RMTransactionFailedException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
