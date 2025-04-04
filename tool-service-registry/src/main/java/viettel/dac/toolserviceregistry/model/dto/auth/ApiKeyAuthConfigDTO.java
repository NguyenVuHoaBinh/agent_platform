package viettel.dac.toolserviceregistry.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.enums.ApiKeyLocation;

/**
 * DTO for API key authentication configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyAuthConfigDTO extends ApiAuthConfigDTO {
    private String apiKey;
    private String keyName;
    private ApiKeyLocation keyLocation;
}