package viettel.dac.toolserviceregistry.exception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception thrown when attempting to delete a tool that has other tools depending on it.
 */
public class ToolHasDependentsException extends RuntimeException {
    public ToolHasDependentsException(String id, List<String> dependentTools) {
        super("Cannot delete tool with ID " + id + " because it is depended on by: " +
                dependentTools.stream().collect(Collectors.joining(", ")));
    }
}
