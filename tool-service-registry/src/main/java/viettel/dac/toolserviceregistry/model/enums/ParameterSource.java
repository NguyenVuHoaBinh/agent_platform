package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing the source of parameters.
 */
public enum ParameterSource {
    USER_INPUT,           // Parameter must be provided by the user
    SYSTEM_PROVIDED,      // Parameter is provided by the system
    DEPENDENT_TOOL,       // Parameter comes from another tool
    DEFAULT_VALUE,        // Parameter uses its default value if not specified
    CONTEXT_VARIABLE,     // Parameter comes from execution context
    API_RESPONSE,         // Parameter comes from an API response
    COMPUTED              // Parameter is computed during execution
}