package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when parsing an API response fails.
 */
public class ApiResponseParsingException extends RuntimeException {
    public ApiResponseParsingException(String message) {
        super(message);
    }

    public ApiResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}