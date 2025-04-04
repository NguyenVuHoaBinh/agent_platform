package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a tool is not found.
 */
public class ToolNotFoundException extends RuntimeException {
    public ToolNotFoundException(String id) {
        super("Tool not found with ID: " + id);
    }
    public ToolNotFoundException(String field, String value) {
        super("Tool not found with " + field + ": " + value);
    }
}
