package viettel.dac.intentanalysisservice.exception;

/**
 * Exception thrown when an analysis is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}