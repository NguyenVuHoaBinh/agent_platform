package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a tool parameter is not found.
 */
public class ToolParameterNotFoundException extends RuntimeException {
    public ToolParameterNotFoundException(String parameterId) {
        super("Tool parameter not found with ID: " + parameterId);
    }
}