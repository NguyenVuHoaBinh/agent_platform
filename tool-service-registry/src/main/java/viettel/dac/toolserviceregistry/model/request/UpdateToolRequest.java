package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for updating an existing tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolRequest {
    /**
     * Required name of the tool. Must be unique.
     */
    @NotBlank(message = "Tool name is required")
    private String name;

    /**
     * Required description of the tool.
     */
    @NotBlank(message = "Tool description is required")
    private String description;

    /**
     * List of parameters for the tool. All existing parameters will be replaced.
     */
    @Valid
    @Builder.Default
    private List<ToolParameterRequest> parameters = new ArrayList<>();

    /**
     * List of dependencies for the tool. All existing dependencies will be replaced.
     */
    @Valid
    @Builder.Default
    private List<ToolDependencyRequest> dependencies = new ArrayList<>();

    /**
     * List of category IDs for the tool. All existing categories will be replaced.
     */
    @Builder.Default
    private List<String> categoryIds = new ArrayList<>();

    /**
     * List of examples for the tool. All existing examples will be replaced.
     */
    @Valid
    @Builder.Default
    private List<ToolExampleRequest> examples = new ArrayList<>();

    @NotNull(message = "Tool type is required")
    private ToolType toolType;

    @Valid
    private ApiToolMetadataRequest apiMetadata;
}