package viettel.dac.toolserviceregistry.model.enums;

/**
 * Enum representing OAuth2 grant types.
 */
public enum OAuth2GrantType {
    AUTHORIZATION_CODE,    // Authorization code grant
    CLIENT_CREDENTIALS,    // Client credentials grant
    PASSWORD,              // Resource owner password credentials grant
    IMPLICIT,              // Implicit grant
    REFRESH_TOKEN          // Refresh token grant
}