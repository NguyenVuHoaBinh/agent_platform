// File: src/main/java/viettel/dac/toolserviceregistry/model/entity/auth/OAuth2AuthConfig.java
package viettel.dac.toolserviceregistry.model.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.enums.OAuth2GrantType;

/**
 * OAuth2 authentication configuration.
 */
@Entity
@DiscriminatorValue("OAUTH2")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthConfig extends ApiAuthConfig {
    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "token_url")
    private String tokenUrl;

    @Column(name = "authorization_url")
    private String authorizationUrl;

    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type")
    private OAuth2GrantType grantType;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "token_expiry")
    private Long tokenExpiry;
}