package viettel.dac.intentanalysisservice.exception;

/**
 * Exception thrown when a request times out.
 */
public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}