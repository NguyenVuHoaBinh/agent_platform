package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for transferring tool dependency data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDependencyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String dependencyToolId;
    private String dependencyToolName;
    private viettel.dac.toolserviceregistry.model.enums.DependencyType dependencyType;
    private String description;

    @Builder.Default
    private List<ParameterMappingDTO> parameterMappings = new ArrayList<>();
}