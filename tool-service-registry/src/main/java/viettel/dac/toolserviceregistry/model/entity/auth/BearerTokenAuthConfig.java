package viettel.dac.toolserviceregistry.model.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Bearer token authentication configuration.
 */
@Entity
@DiscriminatorValue("BEARER_TOKEN")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BearerTokenAuthConfig extends ApiAuthConfig {
    private String token;

    @Column(name = "token_prefix")
    private String tokenPrefix = "Bearer";
}