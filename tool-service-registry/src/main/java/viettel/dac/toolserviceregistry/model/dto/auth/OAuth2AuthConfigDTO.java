// File: src/main/java/viettel/dac/toolserviceregistry/model/dto/auth/OAuth2AuthConfigDTO.java
package viettel.dac.toolserviceregistry.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.enums.OAuth2GrantType;

/**
 * DTO for OAuth2 authentication configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthConfigDTO extends ApiAuthConfigDTO {
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String authorizationUrl;
    private String scope;
    private OAuth2GrantType grantType;
    private String accessToken;
    private String refreshToken;
    private Long tokenExpiry;
}