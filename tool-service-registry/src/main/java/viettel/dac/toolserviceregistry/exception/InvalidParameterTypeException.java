package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a parameter has an invalid type.
 */
public class InvalidParameterTypeException extends RuntimeException {
    public InvalidParameterTypeException(String parameter, String message) {
        super("Invalid type for parameter '" + parameter + "': " + message);
    }
}