package viettel.dac.toolserviceregistry.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for bearer token authentication configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BearerTokenAuthConfigDTO extends ApiAuthConfigDTO {
    private String token;
    private String tokenPrefix;
}