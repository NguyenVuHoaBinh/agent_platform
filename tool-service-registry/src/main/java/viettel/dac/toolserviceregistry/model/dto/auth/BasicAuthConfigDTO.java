package viettel.dac.toolserviceregistry.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for basic authentication configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BasicAuthConfigDTO extends ApiAuthConfigDTO {
    private String username;
    private String password;
}