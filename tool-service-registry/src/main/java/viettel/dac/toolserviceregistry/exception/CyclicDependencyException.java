package viettel.dac.toolserviceregistry.exception;

/**
 * Exception thrown when adding dependencies would create a cycle in the dependency graph.
 */
public class CyclicDependencyException extends RuntimeException {
    public CyclicDependencyException(String message) {
        super(message);
    }
}