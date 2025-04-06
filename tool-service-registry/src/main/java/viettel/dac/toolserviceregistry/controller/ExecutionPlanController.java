package viettel.dac.toolserviceregistry.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.dto.ExecutionPlanView;
import viettel.dac.toolserviceregistry.model.dto.ParameterRequirement;
import viettel.dac.toolserviceregistry.model.request.ExecutionPlanRequest;
import viettel.dac.toolserviceregistry.service.ExecutionPlanService;
import viettel.dac.toolserviceregistry.service.ParameterValidationService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for advanced execution plan operations.
 * Provides endpoints for creating, retrieving, and managing execution plans.
 */
@RestController
@RequestMapping("/api/v1/execution-plans")
@RequiredArgsConstructor
@Slf4j
public class ExecutionPlanController {
    private final ExecutionPlanService executionPlanService;
    private final ParameterValidationService parameterValidationService;

    /**
     * Generates an execution plan for a set of tools.
     *
     * @param request The execution plan request
     * @return The generated execution plan
     */
    @PostMapping
    public ResponseEntity<ExecutionPlanView> generateExecutionPlan(
            @RequestBody ExecutionPlanRequest request) {
        log.info("Generating execution plan for tools: {}", request.getToolIds());

        ExecutionPlanView plan = executionPlanService.generateExecutionPlan(
                request.getToolIds(), request.getProvidedParameters());

        return ResponseEntity.ok(plan);
    }

    /**
     * Gets a specific version of an execution plan.
     *
     * @param toolIds The tool IDs
     * @param version The version number
     * @return The execution plan, or 404 if not found
     */
    @GetMapping("/version/{version}")
    public ResponseEntity<ExecutionPlanView> getExecutionPlanVersion(
            @RequestParam List<String> toolIds,
            @PathVariable int version) {
        log.info("Retrieving execution plan version {} for tools: {}", version, toolIds);

        ExecutionPlanView plan = executionPlanService.getExecutionPlanVersion(toolIds, version);

        if (plan == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(plan);
    }

    /**
     * Gets all versions of an execution plan.
     *
     * @param toolIds The tool IDs
     * @return Map of version to execution plan
     */
    @GetMapping("/versions")
    public ResponseEntity<Map<Integer, ExecutionPlanView>> getAllExecutionPlanVersions(
            @RequestParam List<String> toolIds) {
        log.info("Retrieving all execution plan versions for tools: {}", toolIds);

        Map<Integer, ExecutionPlanView> versions = executionPlanService.getAllExecutionPlanVersions(toolIds);

        return ResponseEntity.ok(versions);
    }

    /**
     * Gets parameter prompts for missing parameters.
     *
     * @param toolIds The tool IDs
     * @param providedParameters The parameters already provided
     * @return Map of tool ID to list of parameter prompts
     */
    @PostMapping("/parameter-prompts")
    public ResponseEntity<Map<String, List<String>>> getParameterPrompts(
            @RequestParam List<String> toolIds,
            @RequestBody(required = false) Map<String, Object> providedParameters) {
        log.info("Generating parameter prompts for tools: {}", toolIds);

        if (providedParameters == null) {
            providedParameters = Map.of();
        }

        // Generate execution plan to identify missing parameters
        ExecutionPlanView plan = executionPlanService.generateExecutionPlan(
                toolIds, providedParameters);

        // Generate parameter prompts
        Map<String, List<String>> prompts = parameterValidationService.generateParameterPrompts(
                plan.getMissingParameters(), toolIds);

        return ResponseEntity.ok(prompts);
    }

    /**
     * Gets parallel execution groups for a set of tools.
     *
     * @param toolIds The tool IDs
     * @return List of parallel execution groups
     */
    @GetMapping("/parallel-groups")
    public ResponseEntity<List<Set<String>>> getParallelExecutionGroups(
            @RequestParam List<String> toolIds) {
        log.info("Getting parallel execution groups for tools: {}", toolIds);

        // Generate execution plan
        ExecutionPlanView plan = executionPlanService.generateExecutionPlan(
                toolIds, Map.of());

        return ResponseEntity.ok(plan.getParallelExecutionGroups());
    }

    /**
     * Gets missing parameters for a set of tools.
     *
     * @param toolIds The tool IDs
     * @param providedParameters The parameters already provided
     * @return Map of tool ID to set of missing parameters
     */
    @PostMapping("/missing-parameters")
    public ResponseEntity<Map<String, Set<ParameterRequirement>>> getMissingParameters(
            @RequestParam List<String> toolIds,
            @RequestBody(required = false) Map<String, Object> providedParameters) {
        log.info("Getting missing parameters for tools: {}", toolIds);

        if (providedParameters == null) {
            providedParameters = Map.of();
        }

        // Generate execution plan
        ExecutionPlanView plan = executionPlanService.generateExecutionPlan(
                toolIds, providedParameters);

        return ResponseEntity.ok(plan.getMissingParameters());
    }
}