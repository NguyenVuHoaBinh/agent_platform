package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing the types of parameters supported by tools.
 */
public enum ParameterType {
    STRING,        // Text values
    NUMBER,        // Numeric values (integers or decimals)
    BOOLEAN,       // True/false values
    ARRAY,         // List of values
    OBJECT,        // Complex structured object
    DATE,          // Date values
    DATETIME,      // Date and time values
    EMAIL,         // Email addresses
    URL,           // URL/URI values
    FILE,          // File references
    ENUM,          // Enumeration of predefined values
    JSON,          // JSON formatted string
    XML,           // XML formatted string
    SECRET         // Sensitive values (passwords, tokens, etc.)
}