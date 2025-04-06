package viettel.dac.toolserviceregistry.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.graph.DirectedGraph;
import viettel.dac.toolserviceregistry.mapper.ParameterMappingMapper;
import viettel.dac.toolserviceregistry.model.dto.*;
import viettel.dac.toolserviceregistry.model.entity.ParameterMapping;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.reponse.ExecutionPlanResponse;
import viettel.dac.toolserviceregistry.model.request.ExecutionPlanRequest;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced service for generating and managing execution plans for tools.
 * Added support for parallel execution, plan optimization, and versioning.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "executionPlans")
public class ExecutionPlanService {
    private final ToolDependencyGraphService graphService;
    private final ParameterValidationService parameterValidationService;
    private final ToolRepository toolRepository;
    private final ParameterMappingMapper parameterMappingMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final ApiToolService apiToolService;

    // Cache for storing versioned execution plans
    private final ConcurrentHashMap<String, Map<Integer, ExecutionPlanView>> planVersionCache = new ConcurrentHashMap<>();

    private static final int MAX_PLAN_VERSIONS = 10;

    /**
     * Generates an execution plan for a set of tools with support for parallelization and optimization.
     *
     * @param toolIds The IDs of the tools to include in the plan
     * @param providedParameters The parameters that are already provided
     * @return The execution plan
     */
    @Cacheable(key = "#toolIds.hashCode() + '-' + #providedParameters.keySet().hashCode()")
    public ExecutionPlanView generateExecutionPlan(
            List<String> toolIds,
            Map<String, Object> providedParameters) {
        log.debug("Generating execution plan for tools: {} with parameters: {}",
                toolIds, providedParameters.keySet());
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Get dependency closure
            Set<String> allToolIds = graphService.getDependencyClosure(toolIds);

            // Generate execution order using topological sort
            List<String> toolsInOrder = graphService.topologicalSort(new ArrayList<>(allToolIds));

            // Generate parallel execution groups
            List<Set<String>> parallelExecutionGroups = identifyParallelExecutionGroups(toolsInOrder);

            // Apply API context-aware optimization
            optimizeExecutionPlanForApi(parallelExecutionGroups);

            // Identify missing parameters
            Map<String, Set<ParameterRequirement>> missingParameters =
                    parameterValidationService.identifyMissingParameters(toolsInOrder, providedParameters);

            // Get parameter mappings
            Map<String, List<ParameterMappingDTO>> parameterMappings =
                    getParameterMappings(toolsInOrder);

            // Determine if there are missing required parameters
            boolean hasMissingRequiredParameters =
                    parameterValidationService.hasRequiredParametersMissing(missingParameters);

            // Create plan with version
            ExecutionPlanView plan = ExecutionPlanView.builder()
                    .toolsInOrder(toolsInOrder)
                    .missingParameters(missingParameters)
                    .parameterMappings(parameterMappings)
                    .hasMissingRequiredParameters(hasMissingRequiredParameters)
                    .parallelExecutionGroups(parallelExecutionGroups)
                    .version(1)
                    .generatedAt(LocalDateTime.now())
                    .optimized(true)
                    .build();

            // Store versioned plan
            storePlanVersion(generatePlanKey(toolIds), plan);

            long elapsedTime = sample.stop(meterRegistry.timer("execution.plan.generation.time"));
            log.debug("Generated execution plan in {}ms with {} tools in order and {} parallel groups",
                    elapsedTime / 1_000_000, toolsInOrder.size(), parallelExecutionGroups.size());

            return plan;
        } catch (Exception e) {
            log.error("Failed to generate execution plan", e);
            meterRegistry.counter("execution.plan.generation.error").increment();
            throw e;
        }
    }

    /**
     * Identifies groups of tools that can be executed in parallel.
     *
     * @param toolsInOrder The tools in topological order
     * @return List of sets, where each set contains tools that can be executed in parallel
     */
    List<Set<String>> identifyParallelExecutionGroups(List<String> toolsInOrder) {
        DirectedGraph<String> graph = graphService.buildDependencyGraph(true);
        List<Set<String>> parallelGroups = new ArrayList<>();

        // Group 1: Tools with no dependencies (source nodes)
        Set<String> sourceNodes = new HashSet<>();
        for (String tool : toolsInOrder) {
            if (graph.getIncomingEdges(tool).isEmpty()) {
                sourceNodes.add(tool);
            }
        }

        if (!sourceNodes.isEmpty()) {
            parallelGroups.add(sourceNodes);
        }

        // Process remaining tools level by level
        Set<String> processedTools = new HashSet<>(sourceNodes);
        while (processedTools.size() < toolsInOrder.size()) {
            Set<String> nextLevelTools = new HashSet<>();

            for (String tool : toolsInOrder) {
                if (processedTools.contains(tool)) {
                    continue;
                }

                // Check if all dependencies are processed
                Set<String> dependencies = graph.getIncomingEdges(tool);
                if (processedTools.containsAll(dependencies)) {
                    nextLevelTools.add(tool);
                }
            }

            if (!nextLevelTools.isEmpty()) {
                parallelGroups.add(nextLevelTools);
                processedTools.addAll(nextLevelTools);
            } else {
                // Avoid infinite loop if there's a cycle
                break;
            }
        }

        return parallelGroups;
    }

    /**
     * Optimizes execution plan for API tools - grouping API calls to the same endpoints.
     *
     * @param parallelExecutionGroups The groups of tools that can be executed in parallel
     */
    private void optimizeExecutionPlanForApi(List<Set<String>> parallelExecutionGroups) {
        List<Set<String>> optimizedGroups = new ArrayList<>();

        for (Set<String> group : parallelExecutionGroups) {
            // Group API tools by their base URL for optimization
            Map<String, Set<String>> toolsByBaseUrl = new HashMap<>();
            Set<String> nonApiTools = new HashSet<>();

            for (String toolId : group) {
                Tool tool = toolRepository.findById(toolId).orElse(null);
                if (tool == null) continue;

                if (tool.getToolType() == ToolType.API_TOOL) {
                    ApiToolMetadataDTO apiMetadata = apiToolService.getApiToolMetadataDTO(toolId);
                    if (apiMetadata != null) {
                        String baseUrl = apiMetadata.getBaseUrl();
                        toolsByBaseUrl.computeIfAbsent(baseUrl, k -> new HashSet<>()).add(toolId);
                    } else {
                        nonApiTools.add(toolId);
                    }
                } else {
                    nonApiTools.add(toolId);
                }
            }

            // Add optimized API tool groups and non-API tools
            for (Set<String> apiToolGroup : toolsByBaseUrl.values()) {
                optimizedGroups.add(apiToolGroup);
            }

            if (!nonApiTools.isEmpty()) {
                optimizedGroups.add(nonApiTools);
            }
        }

        // Replace original groups with optimized ones
        parallelExecutionGroups.clear();
        parallelExecutionGroups.addAll(optimizedGroups);
    }

    /**
     * Handles execution plan requests from Kafka.
     *
     * @param request The execution plan request
     * @param ack The acknowledgment
     */
    @KafkaListener(topics = "${kafka.topic.execution-plan-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleExecutionPlanRequest(ExecutionPlanRequest request, Acknowledgment ack) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Received execution plan request for tools: {}", request.getToolIds());

            // Generate execution plan
            ExecutionPlanView plan = generateExecutionPlan(
                    request.getToolIds(), request.getProvidedParameters());

            // Create response
            ExecutionPlanResponse response = new ExecutionPlanResponse();
            response.setRequestId(requestId);
            response.setTimestamp(LocalDateTime.now());
            response.setToolsInOrder(plan.getToolsInOrder());
            response.setMissingParameters(convertToResponseParameterRequirements(plan.getMissingParameters()));
            response.setParameterMappings(plan.getParameterMappings());
            response.setHasMissingRequiredParameters(plan.isHasMissingRequiredParameters());
            response.setParallelExecutionGroups(plan.getParallelExecutionGroups());
            response.setVersion(plan.getVersion());
            response.setOptimized(plan.isOptimized());

            // Send response
            kafkaTemplate.send("${kafka.topic.execution-plan-responses}", requestId, response);

            // Acknowledge message
            ack.acknowledge();

            long elapsedTime = sample.stop(meterRegistry.timer("execution.plan.request.processing.time"));
            log.info("Processed execution plan request {} in {}ms", requestId, elapsedTime / 1_000_000);
        } catch (Exception e) {
            log.error("Failed to process execution plan request", e);
            meterRegistry.counter("execution.plan.request.error").increment();
            ack.acknowledge(); // Still acknowledge to avoid reprocessing
        }
    }

    /**
     * Gets parameter mappings for tools in the execution order.
     *
     * @param toolsInOrder The tools in execution order
     * @return Map of tool ID to list of parameter mappings
     */
    private Map<String, List<ParameterMappingDTO>> getParameterMappings(List<String> toolsInOrder) {
        Map<String, List<ParameterMappingDTO>> result = new HashMap<>();

        for (String toolId : toolsInOrder) {
            List<ParameterMappingDTO> mappings = new ArrayList<>();

            // Get tool's dependencies
            Tool tool = toolRepository.findById(toolId).orElse(null);
            if (tool == null) continue;

            for (ToolDependency dependency : tool.getDependencies()) {
                // Only consider dependencies that are in our execution order
                if (toolsInOrder.contains(dependency.getDependencyTool().getId())) {
                    for (ParameterMapping mapping : dependency.getParameterMappings()) {
                        mappings.add(parameterMappingMapper.toDto(mapping));
                    }
                }
            }

            if (!mappings.isEmpty()) {
                result.put(toolId, mappings);
            }
        }

        return result;
    }

    /**
     * Converts the parameter requirements from the view format to the response format.
     *
     * @param viewRequirements Map of tool ID to set of parameter requirements in view format
     * @return Map of tool ID to list of parameter requirements in response format
     */
    private Map<String, List<ParameterRequirement>> convertToResponseParameterRequirements(
            Map<String, Set<ParameterRequirement>> viewRequirements) {
        Map<String, List<ParameterRequirement>> result = new HashMap<>();

        for (Map.Entry<String, Set<ParameterRequirement>> entry : viewRequirements.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return result;
    }

    /**
     * Stores a versioned execution plan.
     *
     * @param planKey The key for the plan
     * @param plan The execution plan
     */
    private void storePlanVersion(String planKey, ExecutionPlanView plan) {
        Map<Integer, ExecutionPlanView> versions = planVersionCache.computeIfAbsent(planKey, k -> new HashMap<>());

        // Determine next version number
        int nextVersion = versions.keySet().stream().max(Integer::compare).orElse(0) + 1;
        plan.setVersion(nextVersion);

        // Store the plan
        versions.put(nextVersion, plan);

        // Prune old versions if needed
        if (versions.size() > MAX_PLAN_VERSIONS) {
            Integer oldestVersion = versions.keySet().stream().min(Integer::compare).orElse(1);
            versions.remove(oldestVersion);
        }
    }

    /**
     * Gets a specific version of an execution plan.
     *
     * @param toolIds The tools IDs in the plan
     * @param version The version to retrieve
     * @return The execution plan, or null if not found
     */
    public ExecutionPlanView getExecutionPlanVersion(List<String> toolIds, int version) {
        String planKey = generatePlanKey(toolIds);
        Map<Integer, ExecutionPlanView> versions = planVersionCache.get(planKey);

        if (versions == null) {
            return null;
        }

        return versions.get(version);
    }

    /**
     * Gets all versions of an execution plan.
     *
     * @param toolIds The tools IDs in the plan
     * @return Map of version to execution plan
     */
    public Map<Integer, ExecutionPlanView> getAllExecutionPlanVersions(List<String> toolIds) {
        String planKey = generatePlanKey(toolIds);
        return planVersionCache.getOrDefault(planKey, Collections.emptyMap());
    }

    /**
     * Generates a unique key for an execution plan based on tool IDs.
     *
     * @param toolIds The tool IDs
     * @return The plan key
     */
    private String generatePlanKey(List<String> toolIds) {
        return toolIds.stream().sorted().collect(Collectors.joining("-"));
    }
}