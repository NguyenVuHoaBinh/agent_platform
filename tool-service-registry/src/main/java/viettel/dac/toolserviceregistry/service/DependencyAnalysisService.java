package viettel.dac.toolserviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.graph.DirectedGraph;
import viettel.dac.toolserviceregistry.model.dto.DependencyAnalysisDTO;
import viettel.dac.toolserviceregistry.model.dto.ToolDependencyDTO;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing tool dependencies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DependencyAnalysisService {
    private final ToolRepository toolRepository;
    private final ToolDependencyGraphService graphService;

    /**
     * Analyzes the dependencies of a tool.
     *
     * @param toolId The ID of the tool
     * @return Dependency analysis results
     */
    public DependencyAnalysisDTO analyzeDependencies(String toolId) {
        log.debug("Analyzing dependencies for tool: {}", toolId);

        // Get the tool
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolId));

        // Build dependency graph
        DirectedGraph<String> graph = graphService.buildDependencyGraph(false);

        // Get direct dependencies
        Set<String> directDependencies = graph.getIncomingEdges(toolId);

        // Get all dependencies (transitive closure)
        Set<String> allDependencies = graph.getReverseTransitiveClosure(Collections.singleton(toolId));

        // Get indirect dependencies
        Set<String> indirectDependencies = new HashSet<>(allDependencies);
        indirectDependencies.removeAll(directDependencies);
        indirectDependencies.remove(toolId);

        // Get direct dependents
        Set<String> directDependents = graph.getOutgoingEdges(toolId);

        // Get all dependents
        Set<String> allDependents = graph.getTransitiveClosure(Collections.singleton(toolId));

        // Get indirect dependents
        Set<String> indirectDependents = new HashSet<>(allDependents);
        indirectDependents.removeAll(directDependents);

        // Check for cycles
        boolean hasCycles = false;
        List<List<String>> cycles = new ArrayList<>();
        if (graph.hasCycles()) {
            // Try to find cycles involving this tool
            hasCycles = true;

            for (List<String> cycle : graph.findAllCycles()) {
                if (cycle.contains(toolId)) {
                    cycles.add(cycle);
                }
            }
        }

        // Find critical dependencies (removing them would break many tools)
        Map<String, Integer> dependentCountMap = new HashMap<>();
        for (String dependency : allDependencies) {
            int dependentCount = graph.getTransitiveClosure(Collections.singleton(dependency)).size();
            dependentCountMap.put(dependency, dependentCount);
        }

        // Sort dependencies by dependent count
        List<String> sortedDependencies = new ArrayList<>(dependentCountMap.keySet());
        sortedDependencies.sort((d1, d2) -> dependentCountMap.get(d2).compareTo(dependentCountMap.get(d1)));

        List<String> criticalDependencies = sortedDependencies.stream()
                .limit(5)
                .collect(Collectors.toList());

        // Fetch tool details for dependencies and dependents
        Map<String, Tool> toolsMap = fetchTools(allDependencies, allDependents);

        // Build the analysis DTO
        DependencyAnalysisDTO analysis = new DependencyAnalysisDTO();
        analysis.setToolId(toolId);
        analysis.setToolName(tool.getName());
        analysis.setDirectDependencyCount(directDependencies.size());
        analysis.setAllDependencyCount(allDependencies.size());
        analysis.setDirectDependentCount(directDependents.size());
        analysis.setAllDependentCount(allDependents.size());
        analysis.setHasCycles(hasCycles);
        analysis.setCyclesCount(cycles.size());
        analysis.setCycles(formatCycles(cycles, toolsMap));
        analysis.setRequiredDependencyCount(countRequiredDependencies(tool.getDependencies()));
        analysis.setOptionalDependencyCount(countOptionalDependencies(tool.getDependencies()));
        analysis.setCriticalDependencies(formatCriticalDependencies(criticalDependencies, toolsMap, dependentCountMap));
        analysis.setDirectDependencies(formatDependencies(directDependencies, toolsMap));
        analysis.setIndirectDependencies(formatDependencies(indirectDependencies, toolsMap));
        analysis.setDirectDependents(formatDependencies(directDependents, toolsMap));
        analysis.setIndirectDependents(formatDependencies(indirectDependents, toolsMap));

        return analysis;
    }

    /**
     * Analyzes the dependency impact of removing a tool.
     *
     * @param toolId The ID of the tool to analyze
     * @return Impacted tools and their details
     */
    public Map<String, Object> analyzeRemovalImpact(String toolId) {
        log.debug("Analyzing removal impact for tool: {}", toolId);

        // Build dependency graph
        DirectedGraph<String> graph = graphService.buildDependencyGraph(false);

        // Get all dependents
        Set<String> allDependents = graph.getTransitiveClosure(Collections.singleton(toolId));

        // Fetch tool details
        Map<String, Tool> toolsMap = fetchTools(Collections.emptySet(), allDependents);

        // Categorize dependents by impact level
        Map<String, List<Map<String, Object>>> impactLevels = new HashMap<>();

        // Direct dependents (high impact)
        Set<String> directDependents = graph.getOutgoingEdges(toolId);
        impactLevels.put("highImpact", formatDependenciesWithDetails(directDependents, toolsMap));

        // Indirect dependents (medium impact)
        Set<String> indirectDependents = new HashSet<>(allDependents);
        indirectDependents.removeAll(directDependents);
        impactLevels.put("mediumImpact", formatDependenciesWithDetails(indirectDependents, toolsMap));

        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("toolId", toolId);
        result.put("toolName", toolsMap.containsKey(toolId) ? toolsMap.get(toolId).getName() : "Unknown Tool");
        result.put("totalImpactedTools", allDependents.size());
        result.put("impactByLevel", impactLevels);

        return result;
    }

    /**
     * Finds common dependencies between multiple tools.
     *
     * @param toolIds List of tool IDs to analyze
     * @return Common dependencies and other analysis results
     */
    public Map<String, Object> findCommonDependencies(List<String> toolIds) {
        log.debug("Finding common dependencies for tools: {}", toolIds);

        // Build dependency graph
        DirectedGraph<String> graph = graphService.buildDependencyGraph(false);

        // Get dependencies for each tool
        Map<String, Set<String>> dependenciesByTool = new HashMap<>();
        for (String toolId : toolIds) {
            Set<String> dependencies = graph.getReverseTransitiveClosure(Collections.singleton(toolId));
            dependencies.remove(toolId);  // Remove self
            dependenciesByTool.put(toolId, dependencies);
        }

        // Find common dependencies
        Set<String> commonDependencies = null;
        for (Set<String> dependencies : dependenciesByTool.values()) {
            if (commonDependencies == null) {
                commonDependencies = new HashSet<>(dependencies);
            } else {
                commonDependencies.retainAll(dependencies);
            }
        }

        if (commonDependencies == null) {
            commonDependencies = Collections.emptySet();
        }

        // Find unique dependencies for each tool
        Map<String, Set<String>> uniqueDependencies = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : dependenciesByTool.entrySet()) {
            Set<String> unique = new HashSet<>(entry.getValue());
            unique.removeAll(commonDependencies);
            uniqueDependencies.put(entry.getKey(), unique);
        }

        // Fetch tool details
        Set<String> allToolIds = new HashSet<>(toolIds);
        for (Set<String> dependencies : dependenciesByTool.values()) {
            allToolIds.addAll(dependencies);
        }

        Map<String, Tool> toolsMap = toolRepository.findAllById(allToolIds)
                .stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));

        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("totalTools", toolIds.size());
        result.put("commonDependencyCount", commonDependencies.size());
        result.put("commonDependencies", formatDependenciesWithDetails(commonDependencies, toolsMap));

        Map<String, Object> toolSpecificDependencies = new HashMap<>();
        for (String toolId : toolIds) {
            Map<String, Object> toolInfo = new HashMap<>();
            toolInfo.put("toolName", toolsMap.containsKey(toolId) ? toolsMap.get(toolId).getName() : "Unknown Tool");
            toolInfo.put("totalDependencies", dependenciesByTool.get(toolId).size());
            toolInfo.put("uniqueDependencies", formatDependenciesWithDetails(uniqueDependencies.get(toolId), toolsMap));

            toolSpecificDependencies.put(toolId, toolInfo);
        }

        result.put("toolSpecificDependencies", toolSpecificDependencies);

        return result;
    }

    /**
     * Fetches tools by IDs and returns a map of ID to Tool.
     *
     * @param dependencyIds IDs of dependencies
     * @param dependentIds IDs of dependents
     * @return Map of tool ID to Tool
     */
    private Map<String, Tool> fetchTools(Set<String> dependencyIds, Set<String> dependentIds) {
        Set<String> allIds = new HashSet<>();
        allIds.addAll(dependencyIds);
        allIds.addAll(dependentIds);

        return toolRepository.findAllById(allIds)
                .stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));
    }

    /**
     * Counts required dependencies.
     *
     * @param dependencies List of dependencies
     * @return Count of required dependencies
     */
    private int countRequiredDependencies(List<ToolDependency> dependencies) {
        return (int) dependencies.stream()
                .filter(d -> d.getDependencyType() == DependencyType.REQUIRED)
                .count();
    }

    /**
     * Counts optional dependencies.
     *
     * @param dependencies List of dependencies
     * @return Count of optional dependencies
     */
    private int countOptionalDependencies(List<ToolDependency> dependencies) {
        return (int) dependencies.stream()
                .filter(d -> d.getDependencyType() == DependencyType.OPTIONAL)
                .count();
    }

    /**
     * Formats cycles for display.
     *
     * @param cycles List of cycles
     * @param toolsMap Map of tool ID to Tool
     * @return Formatted cycles
     */
    private List<List<String>> formatCycles(List<List<String>> cycles, Map<String, Tool> toolsMap) {
        return cycles.stream()
                .map(cycle -> cycle.stream()
                        .map(id -> toolsMap.containsKey(id) ? toolsMap.get(id).getName() : id)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Formats critical dependencies for display.
     *
     * @param criticalDependencies List of critical dependency IDs
     * @param toolsMap Map of tool ID to Tool
     * @param dependentCountMap Map of dependency ID to dependent count
     * @return Formatted critical dependencies
     */
    private List<Map<String, Object>> formatCriticalDependencies(
            List<String> criticalDependencies,
            Map<String, Tool> toolsMap,
            Map<String, Integer> dependentCountMap) {

        return criticalDependencies.stream()
                .map(id -> {
                    Map<String, Object> dependency = new HashMap<>();
                    dependency.put("id", id);
                    dependency.put("name", toolsMap.containsKey(id) ? toolsMap.get(id).getName() : "Unknown Tool");
                    dependency.put("dependentCount", dependentCountMap.getOrDefault(id, 0));
                    return dependency;
                })
                .collect(Collectors.toList());
    }

    /**
     * Formats dependencies for display.
     *
     * @param dependencyIds Set of dependency IDs
     * @param toolsMap Map of tool ID to Tool
     * @return Formatted dependencies
     */
    private List<String> formatDependencies(Set<String> dependencyIds, Map<String, Tool> toolsMap) {
        return dependencyIds.stream()
                .map(id -> toolsMap.containsKey(id) ? toolsMap.get(id).getName() : "Unknown Tool (" + id + ")")
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Formats dependencies with details for display.
     *
     * @param dependencyIds Set of dependency IDs
     * @param toolsMap Map of tool ID to Tool
     * @return Formatted dependencies with details
     */
    private List<Map<String, Object>> formatDependenciesWithDetails(
            Set<String> dependencyIds,
            Map<String, Tool> toolsMap) {

        return dependencyIds.stream()
                .map(id -> {
                    Map<String, Object> dependency = new HashMap<>();
                    dependency.put("id", id);

                    if (toolsMap.containsKey(id)) {
                        Tool tool = toolsMap.get(id);
                        dependency.put("name", tool.getName());
                        dependency.put("type", tool.getToolType().name());
                        dependency.put("active", tool.isActive());
                    } else {
                        dependency.put("name", "Unknown Tool");
                        dependency.put("type", "UNKNOWN");
                        dependency.put("active", false);
                    }

                    return dependency;
                })
                .sorted((m1, m2) -> ((String) m1.get("name")).compareTo((String) m2.get("name")))
                .collect(Collectors.toList());
    }
}