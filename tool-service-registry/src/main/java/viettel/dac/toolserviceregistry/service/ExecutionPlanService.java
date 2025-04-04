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
import viettel.dac.toolserviceregistry.mapper.ParameterMappingMapper;
import viettel.dac.toolserviceregistry.model.dto.ExecutionPlanView;
import viettel.dac.toolserviceregistry.model.dto.ParameterMappingDTO;
import viettel.dac.toolserviceregistry.model.dto.ParameterRequirement;
import viettel.dac.toolserviceregistry.model.entity.ParameterMapping;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.reponse.ExecutionPlanResponse;
import viettel.dac.toolserviceregistry.model.request.ExecutionPlanRequest;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for generating and managing execution plans for tools.
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

    /**
     * Generates an execution plan for a set of tools.
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

            // Identify missing parameters
            Map<String, Set<ParameterRequirement>> missingParameters =
                    parameterValidationService.identifyMissingParameters(toolsInOrder, providedParameters);

            // Get parameter mappings
            Map<String, List<ParameterMappingDTO>> parameterMappings =
                    getParameterMappings(toolsInOrder);

            // Determine if there are missing required parameters
            boolean hasMissingRequiredParameters =
                    parameterValidationService.hasRequiredParametersMissing(missingParameters);

            ExecutionPlanView plan = ExecutionPlanView.builder()
                    .toolsInOrder(toolsInOrder)
                    .missingParameters(missingParameters)
                    .parameterMappings(parameterMappings)
                    .hasMissingRequiredParameters(hasMissingRequiredParameters)
                    .build();

            long elapsedTime = sample.stop(meterRegistry.timer("execution.plan.generation.time"));
            log.debug("Generated execution plan in {}ms with {} tools in order",
                    elapsedTime / 1_000_000, toolsInOrder.size());

            return plan;
        } catch (Exception e) {
            log.error("Failed to generate execution plan", e);
            meterRegistry.counter("execution.plan.generation.error").increment();
            throw e;
        }
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
        // Generate a new requestId since ExecutionPlanRequest doesn't have one
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

            // Convert the missingParameters map to the expected format
            response.setMissingParameters(convertToResponseParameterRequirements(plan.getMissingParameters()));

            response.setParameterMappings(plan.getParameterMappings());
            response.setHasMissingRequiredParameters(plan.isHasMissingRequiredParameters());

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
     * Maps ParameterRequirement sets to the format used in events.
     *
     * @param requirements Map of tool ID to set of parameter requirements
     * @return Mapped requirements for events
     */
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
            List<ParameterRequirement> mappedReqs = new ArrayList<>();

            for (ParameterRequirement req : entry.getValue()) {
                // Just add the original ParameterRequirement objects to the list
                mappedReqs.add(req);
            }

            result.put(entry.getKey(), mappedReqs);
        }

        return result;
    }
}