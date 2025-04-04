package viettel.dac.toolserviceregistry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.dto.ExecutionPlanView;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.reponse.ToolDependencyView;
import viettel.dac.toolserviceregistry.model.reponse.ToolDetailResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolQueryResponse;
import viettel.dac.toolserviceregistry.model.request.ExecutionPlanRequest;
import viettel.dac.toolserviceregistry.service.ToolQueryService;

import java.util.List;

/**
 * REST controller for tool query operations.
 */
@RestController
@RequestMapping("/tools/query")
@RequiredArgsConstructor
@Slf4j
public class ToolQueryController {
    private final ToolQueryService queryService;

    /**
     * Queries tools with filtering and pagination.
     *
     * @param active Flag to filter by active status
     * @param category Category to filter by
     * @param search Search term for name/description
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort field and direction (e.g., "name,asc")
     * @return Paginated list of tools
     */
    @GetMapping
    public ToolQueryResponse queryTools(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ToolType toolType, // New parameter
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        log.info("Querying tools with filters: active={}, category={}, toolType={}, search={}",
                active, category, toolType, search);

        return queryService.queryTools(active, category, toolType, search, page, size, sort);
    }

    /**
     * Gets a tool by its ID.
     *
     * @param id The ID of the tool
     * @return The tool details
     */
    @GetMapping("/{id}")
    public ToolDetailResponse getToolById(@PathVariable String id) {
        log.info("Fetching tool details for id: {}", id);
        return queryService.getToolById(id);
    }

    /**
     * Gets a tool by its name.
     *
     * @param name The name of the tool
     * @return The tool details
     */
    @GetMapping("/name/{name}")
    public ToolDetailResponse getToolByName(@PathVariable String name) {
        log.info("Fetching tool details for name: {}", name);
        return queryService.getToolByName(name);
    }

    /**
     * Gets the dependencies of a tool.
     *
     * @param id The ID of the tool
     * @return List of dependencies
     */
    @GetMapping("/{id}/dependencies")
    public List<ToolDependencyView> getToolDependencies(@PathVariable String id) {
        log.info("Fetching dependencies for tool: {}", id);
        return queryService.getToolDependencies(id);
    }

    /**
     * Gets the tools that depend on a tool.
     *
     * @param id The ID of the dependency tool
     * @return List of dependents
     */
    @GetMapping("/{id}/dependents")
    public List<ToolDependencyView> getToolDependents(@PathVariable String id) {
        log.info("Fetching dependents for tool: {}", id);
        return queryService.getToolDependents(id);
    }

    /**
     * Generates an execution plan for a set of tools.
     *
     * @param request The execution plan request
     * @return The execution plan
     */
    @PostMapping("/execution-plan")
    public ExecutionPlanView generateExecutionPlan(
            @Valid @RequestBody ExecutionPlanRequest request) {
        log.info("Generating execution plan for tools: {}", request.getToolIds());
        return queryService.generateExecutionPlan(
                request.getToolIds(),
                request.getProvidedParameters());
    }
}