package viettel.dac.toolserviceregistry.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.dto.ParameterMappingDTO;
import viettel.dac.toolserviceregistry.model.dto.ParameterRequirement;
import viettel.dac.toolserviceregistry.model.event.BaseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced response model with an execution plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanResponse extends BaseEvent {
    private String requestId;

    @Builder.Default
    private List<String> toolsInOrder = new ArrayList<>();

    @Builder.Default
    private Map<String, List<ParameterRequirement>> missingParameters = new HashMap<>();

    @Builder.Default
    private Map<String, List<ParameterMappingDTO>> parameterMappings = new HashMap<>();

    private boolean hasMissingRequiredParameters;

    /**
     * Groups of tools that can be executed in parallel
     */
    @Builder.Default
    private List<Set<String>> parallelExecutionGroups = new ArrayList<>();

    /**
     * Version of the execution plan
     */
    private int version;

    /**
     * Flag indicating whether the plan has been optimized
     */
    private boolean optimized;
}