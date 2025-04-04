package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for representing a dependency in an event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String dependencyToolId;
    private DependencyType dependencyType;

    @Builder.Default
    private List<ParameterMappingDTO> parameterMappings = new ArrayList<>();
}
