package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a parameter mapping references a parameter that doesn't exist.
 */
public class InvalidParameterMappingException extends RuntimeException {
    public InvalidParameterMappingException(String parameter, String message) {
        super("Invalid parameter mapping for parameter '" + parameter + "': " + message);
    }
}
