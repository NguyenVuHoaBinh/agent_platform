package viettel.dac.toolserviceregistry.model.entity.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.entity.ApiToolMetadata;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;

/**
 * Base entity for API authentication configurations.
 */
@Entity
@Table(name = "api_auth_config")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "auth_type", discriminatorType = DiscriminatorType.STRING)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ApiAuthConfig {
    @Id
    private String id;

    @OneToOne
    @JoinColumn(name = "api_metadata_id")
    private ApiToolMetadata apiToolMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", insertable = false, updatable = false)
    private AuthenticationType authType;

    private String name;
    private String description;

    @Column(name = "is_enabled")
    private boolean enabled = true;
}