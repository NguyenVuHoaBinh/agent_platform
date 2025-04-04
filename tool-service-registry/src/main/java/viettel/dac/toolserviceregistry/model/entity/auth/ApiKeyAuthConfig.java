package viettel.dac.toolserviceregistry.model.entity.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.enums.ApiKeyLocation;

/**
 * API key authentication configuration.
 */
@Entity
@DiscriminatorValue("API_KEY")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyAuthConfig extends ApiAuthConfig {
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "key_name")
    private String keyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_location")
    private ApiKeyLocation keyLocation;
}
