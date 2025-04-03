package viettel.dac.toolserviceregistry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.reponse.ToolCreatedResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolDeletedResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolUpdatedResponse;
import viettel.dac.toolserviceregistry.model.request.CreateToolRequest;
import viettel.dac.toolserviceregistry.model.request.UpdateToolRequest;


/**
 * REST controller for tool management commands.
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Slf4j
public class ToolCommandController {
    private final ToolCommandService commandService;

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
}