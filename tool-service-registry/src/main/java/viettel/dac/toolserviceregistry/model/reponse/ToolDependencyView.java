package viettel.dac.toolserviceregistry.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

/**
 * Response model for tool dependency relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDependencyView {
    private String toolId;
    private String toolName;
    private String dependencyToolId;
    private String dependencyToolName;
    private String dependencyType;
    private String description;
    @Builder.Default
    private List<ParameterMappingView> parameterMappings = new ArrayList<>();
}
