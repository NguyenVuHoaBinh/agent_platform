package viettel.dac.intentanalysisservice.exception;

/**
 * Exception thrown when bulkhead rejects a request.
 */
public class BulkheadFullException extends RuntimeException {

    public BulkheadFullException(String message) {
        super(message);
    }

    public BulkheadFullException(String message, Throwable cause) {
        super(message, cause);
    }
}