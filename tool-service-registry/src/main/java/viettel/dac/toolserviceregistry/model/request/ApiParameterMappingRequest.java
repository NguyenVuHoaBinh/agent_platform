package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;

/**
 * Request model for creating or updating an API parameter mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterMappingRequest {
    private String id;

    @NotNull(message = "API parameter location is required")
    private ApiParameterLocation apiLocation;

    @NotBlank(message = "API parameter name is required")
    private String apiParameterName;

    private boolean requiredForApi;

    private String transformationExpression;

    private String responseExtractionPath;
}