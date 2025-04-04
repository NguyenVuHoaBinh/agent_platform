package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when an API call fails.
 */
public class ApiCallException extends RuntimeException {
    public ApiCallException(String message) {
        super(message);
    }

    public ApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}