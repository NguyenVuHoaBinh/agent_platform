package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing where an API parameter is used in the request.
 */
public enum ApiParameterLocation {
    QUERY,          // Parameter appears in the query string
    PATH,           // Parameter is part of the URL path
    HEADER,         // Parameter appears as an HTTP header
    BODY,           // Parameter appears in the request body
    FORM,           // Parameter is sent as form data
    RESPONSE        // Parameter is extracted from the response
}