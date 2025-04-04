package viettel.dac.toolserviceregistry.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.mapper.ToolMapper;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.enums.ToolEventType;
import viettel.dac.toolserviceregistry.model.reponse.ToolCreatedResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolDeletedResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolUpdatedResponse;
import viettel.dac.toolserviceregistry.model.request.CreateToolRequest;
import viettel.dac.toolserviceregistry.model.request.ToolDependencyRequest;
import viettel.dac.toolserviceregistry.model.request.ToolParameterRequest;
import viettel.dac.toolserviceregistry.model.request.UpdateToolRequest;
import viettel.dac.toolserviceregistry.repository.ToolRepository;
import viettel.dac.toolserviceregistry.service.ToolCommandService;
import viettel.dac.toolserviceregistry.service.ToolDependencyGraphService;

import java.time.LocalDateTime;


/**
 * REST controller for tool management commands.
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Slf4j
public class ToolCommandController {
    private final ToolCommandService commandService;
    private final ToolDependencyGraphService graphService;
    private final ToolMapper toolMapper;
    private final ToolRepository toolRepository;

    /**
     * Creates a new tool.
     *
     * @param request The tool creation request
     * @return The created tool response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToolCreatedResponse createTool(@Valid @RequestBody CreateToolRequest request) {
        log.info("Received request to create tool: {}", request.getName());
        String toolId = commandService.createTool(request);
        return ToolCreatedResponse.builder()
                .id(toolId)
                .name(request.getName())
                .message("Tool created successfully")
                .build();
    }

    /**
     * Updates an existing tool.
     *
     * @param id The ID of the tool to update
     * @param request The tool update request
     * @return The updated tool response
     */
    @PutMapping("/{id}")
    public ToolUpdatedResponse updateTool(
            @PathVariable String id,
            @Valid @RequestBody UpdateToolRequest request) {
        log.info("Received request to update tool: {}", id);
        int version = commandService.updateTool(id, request);
        return ToolUpdatedResponse.builder()
                .id(id)
                .name(request.getName())
                .version(version)
                .message("Tool updated successfully")
                .build();
    }

    /**
     * Deletes a tool.
     *
     * @param id The ID of the tool to delete
     * @return The deleted tool response
     */
    @DeleteMapping("/{id}")
    public ToolDeletedResponse deleteTool(@PathVariable String id) {
        log.info("Received request to delete tool: {}", id);
        commandService.deleteTool(id);
        return ToolDeletedResponse.builder()
                .id(id)
                .message("Tool deleted successfully")
                .build();
    }

    /**
     * Activates or deactivates a tool.
     *
     * @param id The ID of the tool
     * @param active Whether the tool should be active
     * @return The updated tool version
     */
    @Transactional
    public int setToolActive(String id, boolean active) {
        log.info("Setting tool {} active state to: {}", id, active);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // If already in the desired state, just return
        if (tool.isActive() == active) {
            return tool.getVersion();
        }

        // Update active status
        tool.setActive(active);
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Publish tool updated event
        commandService.publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        // Publish graph update event
        graphService.publishGraphUpdate(id);

        return savedTool.getVersion();
    }

    /**
     * Activates a tool.
     *
     * @param id The ID of the tool to activate
     * @return The updated tool response
     */
    @PatchMapping("/{id}/activate")
    public ToolUpdatedResponse activateTool(@PathVariable String id) {
        log.info("Received request to activate tool: {}", id);
        int version = commandService.setToolActive(id, true);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Tool activated successfully")
                .build();
    }

    /**
     * Deactivates a tool.
     *
     * @param id The ID of the tool to deactivate
     * @return The updated tool response
     */
    @PatchMapping("/{id}/deactivate")
    public ToolUpdatedResponse deactivateTool(@PathVariable String id) {
        log.info("Received request to deactivate tool: {}", id);
        int version = commandService.setToolActive(id, false);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Tool deactivated successfully")
                .build();
    }

    /**
     * Adds a parameter to a tool.
     *
     * @param id The ID of the tool
     * @param request The parameter to add
     * @return The updated tool response
     */
    @PostMapping("/{id}/parameters")
    public ToolUpdatedResponse addToolParameter(
            @PathVariable String id,
            @Valid @RequestBody ToolParameterRequest request) {
        log.info("Received request to add parameter to tool: {}", id);
        int version = commandService.addToolParameter(id, request);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Parameter added successfully")
                .build();
    }

    /**
     * Removes a parameter from a tool.
     *
     * @param id The ID of the tool
     * @param parameterId The ID of the parameter to remove
     * @return The updated tool response
     */
    @DeleteMapping("/{id}/parameters/{parameterId}")
    public ToolUpdatedResponse removeToolParameter(
            @PathVariable String id,
            @PathVariable String parameterId) {
        log.info("Received request to remove parameter from tool: {}", id);
        int version = commandService.removeToolParameter(id, parameterId);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Parameter removed successfully")
                .build();
    }

    /**
     * Adds a dependency to a tool.
     *
     * @param id The ID of the tool
     * @param request The dependency to add
     * @return The updated tool response
     */
    @PostMapping("/{id}/dependencies")
    public ToolUpdatedResponse addToolDependency(
            @PathVariable String id,
            @Valid @RequestBody ToolDependencyRequest request) {
        log.info("Received request to add dependency to tool: {}", id);
        int version = commandService.addToolDependency(id, request);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Dependency added successfully")
                .build();
    }

    /**
     * Removes a dependency from a tool.
     *
     * @param id The ID of the tool
     * @param dependencyId The ID of the dependency to remove
     * @return The updated tool response
     */
    @DeleteMapping("/{id}/dependencies/{dependencyId}")
    public ToolUpdatedResponse removeToolDependency(
            @PathVariable String id,
            @PathVariable String dependencyId) {
        log.info("Received request to remove dependency from tool: {}", id);
        int version = commandService.removeToolDependency(id, dependencyId);
        return ToolUpdatedResponse.builder()
                .id(id)
                .version(version)
                .message("Dependency removed successfully")
                .build();
    }
}