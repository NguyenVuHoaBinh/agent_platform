package viettel.dac.toolserviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.toolserviceregistry.exception.CyclicDependencyException;
import viettel.dac.toolserviceregistry.exception.DependencyToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.InvalidParameterMappingException;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.graph.DirectedGraph;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;
import viettel.dac.toolserviceregistry.model.request.ParameterMappingRequest;
import viettel.dac.toolserviceregistry.model.request.ToolDependencyRequest;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for dependency-related validation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DependencyValidator {
    private final ToolRepository toolRepository;

    /**
     * Validates that adding dependencies would not create a cycle.
     *
     * @param toolId The ID of the tool
     * @param dependencies The dependencies to add
     */
    public void validateNoCycles(String toolId, List<ToolDependencyRequest> dependencies) {
        log.debug("Validating dependencies for tool: {}", toolId);

        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        // Build dependency graph
        DirectedGraph<String> graph = new DirectedGraph<>();

        // Add the current tool
        graph.addNode(toolId);

        // Add all direct dependencies
        for (ToolDependencyRequest dependency : dependencies) {
            String dependencyId = dependency.getDependencyToolId();

            // Check that dependency exists
            if (!toolRepository.existsById(dependencyId)) {
                throw new DependencyToolNotFoundException(dependencyId);
            }

            // Add edge from dependency to tool (dependency -> tool)
            graph.addNode(dependencyId);
            graph.addEdge(dependencyId, toolId);
        }

        // Add all existing dependencies (except for this tool) to check for cycles
        List<ToolDependency> allDependencies = toolRepository.findAllDependencies();
        for (ToolDependency dep : allDependencies) {
            // Skip dependencies of the current tool (we've already added them from the request)
            if (dep.getTool().getId().equals(toolId)) {
                continue;
            }

            graph.addNode(dep.getTool().getId());
            graph.addNode(dep.getDependencyTool().getId());
            graph.addEdge(dep.getDependencyTool().getId(), dep.getTool().getId());
        }

        // Check for cycles
        if (graph.hasCycles()) {
            throw new CyclicDependencyException("Adding these dependencies would create a cycle");
        }
    }

    /**
     * Validates parameter mappings between tools.
     *
     * @param toolId The ID of the tool
     * @param dependencyToolId The ID of the dependency tool
     * @param mappings The parameter mappings
     */
    public void validateParameterMappings(
            String toolId,
            String dependencyToolId,
            List<ParameterMappingRequest> mappings) {
        log.debug("Validating parameter mappings for tool: {} and dependency: {}",
                toolId, dependencyToolId);

        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        // Load both tools
        Tool tool = toolRepository.findByIdWithParameters(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        Tool dependencyTool = toolRepository.findByIdWithParameters(dependencyToolId)
                .orElseThrow(() -> new ToolNotFoundException(dependencyToolId));

        // Get parameter names for both tools
        Set<String> toolParamNames = tool.getParameters().stream()
                .map(ToolParameter::getName)
                .collect(Collectors.toSet());

        Set<String> dependencyParamNames = dependencyTool.getParameters().stream()
                .map(ToolParameter::getName)
                .collect(Collectors.toSet());

        // Validate each mapping
        for (ParameterMappingRequest mapping : mappings) {
            // Check source parameter exists in dependency tool
            if (!dependencyParamNames.contains(mapping.getSourceParameter())) {
                throw new InvalidParameterMappingException(
                        mapping.getSourceParameter(),
                        "Source parameter does not exist in dependency tool");
            }

            // Check target parameter exists in this tool
            if (!toolParamNames.contains(mapping.getTargetParameter())) {
                throw new InvalidParameterMappingException(
                        mapping.getTargetParameter(),
                        "Target parameter does not exist in this tool");
            }
        }
    }
}
