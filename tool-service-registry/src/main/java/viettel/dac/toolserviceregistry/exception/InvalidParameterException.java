package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a parameter value is invalid.
 */
public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String parameterName, String message) {
        super("Invalid value for parameter '" + parameterName + "': " + message);
    }
}