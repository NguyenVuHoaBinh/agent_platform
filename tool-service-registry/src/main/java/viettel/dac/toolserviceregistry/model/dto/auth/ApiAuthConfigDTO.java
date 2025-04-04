package viettel.dac.toolserviceregistry.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;

/**
 * Base DTO for API authentication configurations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApiKeyAuthConfigDTO.class, name = "API_KEY"),
        @JsonSubTypes.Type(value = BasicAuthConfigDTO.class, name = "BASIC"),
        @JsonSubTypes.Type(value = BearerTokenAuthConfigDTO.class, name = "BEARER_TOKEN"),
        @JsonSubTypes.Type(value = OAuth2AuthConfigDTO.class, name = "OAUTH2")
})
public abstract class ApiAuthConfigDTO {
    private String id;
    private AuthenticationType authType;
    private String name;
    private String description;
    private boolean enabled = true;
}