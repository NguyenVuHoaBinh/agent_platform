package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing where an API key is located in the request.
 */
public enum ApiKeyLocation {
    HEADER,     // API key is in a header
    QUERY,      // API key is in a query parameter
    COOKIE      // API key is in a cookie
}