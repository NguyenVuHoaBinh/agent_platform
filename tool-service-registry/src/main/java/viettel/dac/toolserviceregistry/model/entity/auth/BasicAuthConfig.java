package viettel.dac.toolserviceregistry.model.entity.auth;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Basic authentication configuration.
 */
@Entity
@DiscriminatorValue("BASIC")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BasicAuthConfig extends ApiAuthConfig {
    private String username;
    private String password;
}