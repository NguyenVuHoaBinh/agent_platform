package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for creating or updating a parameter mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMappingRequest {
    /**
     * Optional ID for the mapping. Required for updates, ignored for creation.
     */
    private String id;

    /**
     * Required name of the source parameter in the dependency tool.
     */
    @NotBlank(message = "Source parameter is required")
    private String sourceParameter;

    /**
     * Required name of the target parameter in this tool.
     */
    @NotBlank(message = "Target parameter is required")
    private String targetParameter;
}