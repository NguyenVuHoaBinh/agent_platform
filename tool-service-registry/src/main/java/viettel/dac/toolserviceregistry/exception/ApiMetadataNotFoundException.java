package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when API metadata is not found for a tool.
 */
public class ApiMetadataNotFoundException extends RuntimeException {
    public ApiMetadataNotFoundException(String toolId) {
        super("API metadata not found for tool with ID: " + toolId);
    }
}