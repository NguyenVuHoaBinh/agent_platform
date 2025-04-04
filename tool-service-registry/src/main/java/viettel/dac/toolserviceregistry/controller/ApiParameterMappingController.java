// File: src/main/java/viettel/dac/toolserviceregistry/controller/ApiParameterMappingController.java
package viettel.dac.toolserviceregistry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.dto.ApiParameterMappingDTO;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;
import viettel.dac.toolserviceregistry.model.request.ApiParameterMappingRequest;
import viettel.dac.toolserviceregistry.service.ApiParameterMappingService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for API parameter mapping operations.
 */
@RestController
@RequestMapping("/tools/api/mappings")
@RequiredArgsConstructor
@Slf4j
public class ApiParameterMappingController {
    private final ApiParameterMappingService mappingService;

    /**
     * Gets parameter mappings for an API tool.
     *
     * @param toolId The ID of the API tool
     * @return List of parameter mappings
     */
    @GetMapping
    public ResponseEntity<List<ApiParameterMappingDTO>> getParameterMappings(
            @RequestParam String toolId) {
        log.info("Getting API parameter mappings for tool: {}", toolId);

        List<ApiParameterMappingDTO> mappings = mappingService.getParameterMappings(toolId);
        return ResponseEntity.ok(mappings);
    }

    /**
     * Creates a new parameter mapping.
     *
     * @param toolId The ID of the API tool
     * @param parameterId The ID of the tool parameter
     * @param request The mapping request
     * @return The created mapping
     */
    @PostMapping
    public ResponseEntity<ApiParameterMappingDTO> createParameterMapping(
            @RequestParam String toolId,
            @RequestParam String parameterId,
            @Valid @RequestBody ApiParameterMappingRequest request) {
        log.info("Creating API parameter mapping for tool: {} and parameter: {}", toolId, parameterId);

        ApiParameterMappingDTO mapping = mappingService.createParameterMapping(toolId, parameterId, request);
        return ResponseEntity.ok(mapping);
    }

    /**
     * Updates an existing parameter mapping.
     *
     * @param mappingId The ID of the mapping to update
     * @param request The mapping request
     * @return The updated mapping
     */
    @PutMapping("/{mappingId}")
    public ResponseEntity<ApiParameterMappingDTO> updateParameterMapping(
            @PathVariable String mappingId,
            @Valid @RequestBody ApiParameterMappingRequest request) {
        log.info("Updating API parameter mapping: {}", mappingId);

        ApiParameterMappingDTO mapping = mappingService.updateParameterMapping(mappingId, request);
        return ResponseEntity.ok(mapping);
    }

    /**
     * Deletes a parameter mapping.
     *
     * @param mappingId The ID of the mapping to delete
     * @return No content
     */
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> deleteParameterMapping(@PathVariable String mappingId) {
        log.info("Deleting API parameter mapping: {}", mappingId);

        mappingService.deleteParameterMapping(mappingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Generates parameter mappings automatically.
     *
     * @param toolId The ID of the API tool
     * @return List of created mappings
     */
    @PostMapping("/generate")
    public ResponseEntity<List<ApiParameterMappingDTO>> generateParameterMappings(
            @RequestParam String toolId) {
        log.info("Generating API parameter mappings for tool: {}", toolId);

        List<ApiParameterMappingDTO> mappings = mappingService.generateParameterMappings(toolId);
        return ResponseEntity.ok(mappings);
    }

    /**
     * Gets all API parameter locations.
     *
     * @return List of API parameter locations with descriptions
     */
    @GetMapping("/locations")
    public List<Map<String, String>> getApiParameterLocations() {
        return Arrays.stream(ApiParameterLocation.values())
                .map(location -> {
                    Map<String, String> locationInfo = new HashMap<>();
                    locationInfo.put("name", location.name());
                    locationInfo.put("description", getApiParameterLocationDescription(location));
                    return locationInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets a description for an API parameter location.
     *
     * @param location The API parameter location
     * @return The description of the API parameter location
     */
    private String getApiParameterLocationDescription(ApiParameterLocation location) {
        switch (location) {
            case QUERY:
                return "Parameter appears in the query string";
            case PATH:
                return "Parameter is part of the URL path";
            case HEADER:
                return "Parameter appears as an HTTP header";
            case BODY:
                return "Parameter appears in the request body";
            case FORM:
                return "Parameter is sent as form data";
            case RESPONSE:
                return "Parameter is extracted from the response";
            default:
                return "Unknown API parameter location";
        }
    }
}