package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when attempting to create a tool with a name that already exists.
 */
public class DuplicateToolNameException extends RuntimeException {
    public DuplicateToolNameException(String name) {
        super("Tool with name '" + name + "' already exists");
    }
}
