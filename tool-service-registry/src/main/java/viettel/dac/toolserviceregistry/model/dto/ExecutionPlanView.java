package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Enhanced DTO for representing the execution plan for a set of tools.
 * Added support for parallel execution groups and versioning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanView implements Serializable {
    private static final long serialVersionUID = 2L;

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
     * Timestamp when the plan was generated
     */
    private LocalDateTime generatedAt;

    /**
     * Flag indicating whether the plan has been optimized
     */
    private boolean optimized;

    /**
     * Estimated execution time in milliseconds
     */
    private long estimatedExecutionTime;

    /**
     * Gets a flat map of all parameter requirements by tool ID.
     *
     * @return Map of tool ID to parameter requirements
     */
    public Map<String, List<ParameterRequirement>> getAllParameterRequirements() {
        Map<String, List<ParameterRequirement>> result = new HashMap<>();

        for (Map.Entry<String, Set<ParameterRequirement>> entry : missingParameters.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return result;
    }

    /**
     * Gets tools in a specific parallel execution group.
     *
     * @param groupIndex The index of the group
     * @return Set of tool IDs in the group, or empty set if the index is invalid
     */
    public Set<String> getParallelGroup(int groupIndex) {
        if (groupIndex >= 0 && groupIndex < parallelExecutionGroups.size()) {
            return parallelExecutionGroups.get(groupIndex);
        }
        return Collections.emptySet();
    }

    /**
     * Gets the number of parallel execution groups.
     *
     * @return The number of groups
     */
    public int getParallelGroupCount() {
        return parallelExecutionGroups.size();
    }

    /**
     * Checks if a tool has any missing parameters.
     *
     * @param toolId The ID of the tool
     * @return true if the tool has missing parameters
     */
    public boolean hasToolMissingParameters(String toolId) {
        Set<ParameterRequirement> requirements = missingParameters.get(toolId);
        return requirements != null && !requirements.isEmpty();
    }
}