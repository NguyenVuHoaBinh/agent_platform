package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * DTO for representing the execution plan for a set of tools.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanView implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Ordered list of tool IDs to execute
     */
    @Builder.Default
    private List<String> toolsInOrder = new ArrayList<>();

    /**
     * Map of tool ID to set of missing parameters
     */
    @Builder.Default
    private Map<String, Set<ParameterRequirement>> missingParameters = new HashMap<>();

    /**
     * Map of tool ID to list of parameter mappings
     */
    @Builder.Default
    private Map<String, List<ParameterMappingDTO>> parameterMappings = new HashMap<>();

    /**
     * Flag indicating whether there are missing required parameters
     */
    private boolean hasMissingRequiredParameters;
}