package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " not found with ID: " + resourceId);
    }
}