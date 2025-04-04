package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for dependency analysis results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyAnalysisDTO {
    private String toolId;
    private String toolName;
    private int directDependencyCount;
    private int allDependencyCount;
    private int directDependentCount;
    private int allDependentCount;
    private boolean hasCycles;
    private int cyclesCount;
    private List<List<String>> cycles;
    private int requiredDependencyCount;
    private int optionalDependencyCount;
    private List<Map<String, Object>> criticalDependencies;
    private List<String> directDependencies;
    private List<String> indirectDependencies;
    private List<String> directDependents;
    private List<String> indirectDependents;
}