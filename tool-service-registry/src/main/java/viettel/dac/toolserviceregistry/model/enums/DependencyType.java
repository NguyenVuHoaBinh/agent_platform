package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing the types of dependencies between tools.
 */
public enum DependencyType {
    /**
     * The dependency tool must be executed before this tool
     */
    REQUIRED,

    /**
     * The dependency tool can enhance this tool but is not necessary
     */
    OPTIONAL
}