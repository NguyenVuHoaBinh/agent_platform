package viettel.dac.intentanalysisservice.exception;

/**
 * Exception thrown when retry attempts are exhausted.
 */
public class RetryExhaustedException extends RuntimeException {

    public RetryExhaustedException(String message) {
        super(message);
    }

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}