package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a parameter name is duplicated within a tool.
 */
public class DuplicateParameterNameException extends RuntimeException {
    public DuplicateParameterNameException(String name) {
        super("Duplicate parameter name '" + name + "' in tool");
    }
}
