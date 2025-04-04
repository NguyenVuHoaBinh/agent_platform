package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a tool's type is not compatible with the requested operation.
 */
public class ToolTypeNotCompatibleException extends RuntimeException {
    public ToolTypeNotCompatibleException(String toolId, String requiredType) {
        super("Tool with ID " + toolId + " is not of type " + requiredType);
    }
}