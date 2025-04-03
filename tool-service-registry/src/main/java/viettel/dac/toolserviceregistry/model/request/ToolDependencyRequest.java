package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for creating or updating a tool dependency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDependencyRequest {
    /**
     * Optional ID for the dependency. Required for updates, ignored for creation.
     */
    private String id;

    /**
     * Required ID of the dependency tool.
     */
    @NotBlank(message = "Dependency tool ID is required")
    private String dependencyToolId;

    /**
     * Required type of the dependency.
     */
    @NotNull(message = "Dependency type is required")
    private DependencyType dependencyType;

    /**
     * Optional description of the dependency.
     */
    private String description;

    /**
     * Optional list of parameter mappings for the dependency.
     */
    @Valid
    @Builder.Default
    private List<ParameterMappingRequest> parameterMappings = new ArrayList<>();
}