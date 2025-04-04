package viettel.dac.toolserviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.graph.DirectedGraph;
import viettel.dac.toolserviceregistry.model.dto.graph.GraphEdgeDTO;
import viettel.dac.toolserviceregistry.model.dto.graph.GraphNodeDTO;
import viettel.dac.toolserviceregistry.model.dto.graph.GraphVisualizationDTO;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolCategory;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating graph visualization data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GraphVisualizationService {
    private final ToolRepository toolRepository;
    private final ToolDependencyGraphService dependencyGraphService;

    /**
     * Generates visualization data for the complete dependency graph.
     *
     * @param includeInactive Whether to include inactive tools
     * @return Graph visualization data
     */
    public GraphVisualizationDTO generateCompleteGraph(boolean includeInactive) {
        log.debug("Generating complete graph visualization data, includeInactive: {}", includeInactive);

        List<Tool> allTools;
        if (includeInactive) {
            allTools = toolRepository.findAll();
        } else {
            allTools = toolRepository.findAllByActiveTrue();
        }

        return generateGraphForTools(allTools);
    }

    /**
     * Generates visualization data for a specific tool and its dependencies.
     *
     * @param toolId The ID of the central tool
     * @param depth Maximum depth of dependencies to include (0 for unlimited)
     * @return Graph visualization data
     */
    public GraphVisualizationDTO generateToolDependencyGraph(String toolId, int depth) {
        log.debug("Generating tool dependency graph for tool: {}, depth: {}", toolId, depth);

        Set<String> toolIds = new HashSet<>();
        toolIds.add(toolId);

        // Get dependency closure
        if (depth <= 0) {
            // Unlimited depth - get full closure
            toolIds.addAll(dependencyGraphService.getDependencyClosure(Collections.singletonList(toolId)));
        } else {
            // Limited depth - get dependencies up to specified depth
            Set<String> currentLevel = new HashSet<>(Collections.singleton(toolId));
            DirectedGraph<String> graph = dependencyGraphService.buildDependencyGraph(false);

            for (int i = 0; i < depth; i++) {
                Set<String> nextLevel = new HashSet<>();
                for (String id : currentLevel) {
                    nextLevel.addAll(graph.getIncomingEdges(id));
                }

                toolIds.addAll(nextLevel);
                currentLevel = nextLevel;

                if (currentLevel.isEmpty()) {
                    break;
                }
            }
        }

        // Fetch all the tools
        List<Tool> tools = toolRepository.findAllById(toolIds);

        // Create the visualization
        GraphVisualizationDTO graphData = generateGraphForTools(tools);

        // Add metadata for the central tool
        graphData.getMetadata().put("centralToolId", toolId);
        graphData.getMetadata().put("depth", depth);

        return graphData;
    }

    /**
     * Generates visualization data for a specific category.
     *
     * @param categoryId The ID of the category
     * @return Graph visualization data
     */
    public GraphVisualizationDTO generateCategoryGraph(String categoryId) {
        log.debug("Generating category graph for category: {}", categoryId);

        List<Tool> categoryTools = toolRepository.findByCategoriesId(categoryId);
        GraphVisualizationDTO graphData = generateGraphForTools(categoryTools);

        // Add metadata for the category
        graphData.getMetadata().put("categoryId", categoryId);

        return graphData;
    }

    /**
     * Generates visualization data for a specific execution plan.
     *
     * @param toolIds The ordered list of tool IDs in the execution plan
     * @return Graph visualization data
     */
    public GraphVisualizationDTO generateExecutionPlanGraph(List<String> toolIds) {
        log.debug("Generating execution plan graph for tools: {}", toolIds);

        // Fetch all the tools
        List<Tool> tools = toolRepository.findAllById(toolIds);

        // Create the visualization
        GraphVisualizationDTO graphData = generateGraphForTools(tools);

        // Add execution order as metadata to nodes
        for (int i = 0; i < toolIds.size(); i++) {
            String toolId = toolIds.get(i);
            for (GraphNodeDTO node : graphData.getNodes()) {
                if (node.getId().equals(toolId)) {
                    node.getMetadata().put("executionOrder", i + 1);
                    break;
                }
            }
        }

        // Add metadata for the execution plan
        graphData.getMetadata().put("isExecutionPlan", true);
        graphData.getMetadata().put("executionOrder", toolIds);

        return graphData;
    }

    /**
     * Generates graph visualization data for a list of tools.
     *
     * @param tools The list of tools
     * @return Graph visualization data
     */
    private GraphVisualizationDTO generateGraphForTools(List<Tool> tools) {
        // Create a map of tool ID to tool for quick lookups
        Map<String, Tool> toolMap = tools.stream().collect(Collectors.toMap(Tool::getId, t -> t));

        // Create nodes for all tools
        List<GraphNodeDTO> nodes = new ArrayList<>();
        for (Tool tool : tools) {
            GraphNodeDTO node = GraphNodeDTO.builder()
                    .id(tool.getId())
                    .label(tool.getName())
                    .type("tool")
                    .toolType(tool.getToolType())
                    .title(tool.getDescription())
                    .active(tool.isActive())
                    .build();

            // Add category as group
            if (!tool.getCategories().isEmpty()) {
                ToolCategory firstCategory = tool.getCategories().iterator().next();
                node.setGroup(firstCategory.getName());
            }

            // Add metadata
            node.getMetadata().put("parameterCount", tool.getParameters().size());
            node.getMetadata().put("dependencyCount", tool.getDependencies().size());
            node.getMetadata().put("createdAt", tool.getCreatedAt());
            node.getMetadata().put("updatedAt", tool.getUpdatedAt());

            nodes.add(node);
        }

        // Create edges for dependencies
        List<GraphEdgeDTO> edges = new ArrayList<>();

        for (Tool tool : tools) {
            for (ToolDependency dependency : tool.getDependencies()) {
                // Skip if the dependency is to a tool not in our list
                if (!toolMap.containsKey(dependency.getDependencyTool().getId())) {
                    continue;
                }

                GraphEdgeDTO edge = GraphEdgeDTO.builder()
                        .id(dependency.getId())
                        .from(dependency.getDependencyTool().getId())
                        .to(tool.getId())
                        .type(dependency.getDependencyType())
                        .label(dependency.getDependencyType().name())
                        .bidirectional(false)
                        .width(dependency.getDependencyType() == DependencyType.REQUIRED ? 2 : 1)
                        .build();

                // Add metadata
                edge.getMetadata().put("description", dependency.getDescription());
                edge.getMetadata().put("parameterMappingCount", dependency.getParameterMappings().size());

                edges.add(edge);
            }
        }

        // Create the graph visualization DTO
        GraphVisualizationDTO graphData = new GraphVisualizationDTO();
        graphData.setNodes(nodes);
        graphData.setEdges(edges);

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nodeCount", nodes.size());
        metadata.put("edgeCount", edges.size());
        metadata.put("generatedAt", new Date());

        graphData.setMetadata(metadata);

        return graphData;
    }
}