package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a parameter has an invalid validation pattern.
 */
public class InvalidValidationPatternException extends RuntimeException {
    public InvalidValidationPatternException(String parameter, String message) {
        super("Invalid validation pattern for parameter '" + parameter + "': " + message);
    }
}