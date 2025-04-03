package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when an error occurs during event publishing.
 */
public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}