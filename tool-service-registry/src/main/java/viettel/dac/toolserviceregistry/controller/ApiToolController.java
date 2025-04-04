package viettel.dac.toolserviceregistry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.model.enums.HttpMethod;
import viettel.dac.toolserviceregistry.model.request.ApiToolMetadataRequest;
import viettel.dac.toolserviceregistry.service.ApiToolService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for API tool operations.
 */
@RestController
@RequestMapping("/tools/api")
@RequiredArgsConstructor
@Slf4j
public class ApiToolController {
    private final ApiToolService apiToolService;

    /**
     * Gets API metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @return The API metadata
     */
    @GetMapping("/{toolId}/metadata")
    public ResponseEntity<ApiToolMetadataDTO> getApiMetadata(@PathVariable String toolId) {
        log.info("Fetching API metadata for tool: {}", toolId);

        ApiToolMetadataDTO metadata = apiToolService.getApiMetadata(toolId);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(metadata);
    }

    /**
     * Updates API metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @param request The API metadata update request
     * @return The updated API metadata
     */
    @PutMapping("/{toolId}/metadata")
    public ResponseEntity<ApiToolMetadataDTO> updateApiMetadata(
            @PathVariable String toolId,
            @Valid @RequestBody ApiToolMetadataRequest request) {
        log.info("Updating API metadata for tool: {}", toolId);

        ApiToolMetadataDTO metadata = apiToolService.updateApiMetadata(toolId, request);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Tests an API call for a tool.
     *
     * @param toolId The ID of the tool
     * @param parameters Optional parameters to override defaults
     * @return The API response
     */
    @PostMapping("/{toolId}/test")
    public ResponseEntity<Map<String, Object>> testApiCall(
            @PathVariable String toolId,
            @RequestBody(required = false) Map<String, Object> parameters) {
        log.info("Testing API call for tool: {}", toolId);

        String response = apiToolService.testApiCall(toolId, parameters);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Gets all available HTTP methods.
     *
     * @return List of available HTTP methods
     */
    @GetMapping("/http-methods")
    public List<String> getHttpMethods() {
        return Arrays.stream(HttpMethod.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    /**
     * Gets all available authentication types.
     *
     * @return List of available authentication types with descriptions
     */
    @GetMapping("/auth-types")
    public List<Map<String, String>> getAuthenticationTypes() {
        return Arrays.stream(AuthenticationType.values())
                .map(type -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("name", type.name());
                    typeInfo.put("description", getAuthTypeDescription(type));
                    return typeInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets a description for an authentication type.
     *
     * @param type The authentication type
     * @return The description of the authentication type
     */
    private String getAuthTypeDescription(AuthenticationType type) {
        switch (type) {
            case NONE:
                return "No authentication required";
            case API_KEY:
                return "Authentication using API key";
            case BASIC:
                return "Basic HTTP authentication (username/password)";
            case BEARER_TOKEN:
                return "Authentication using Bearer token";
            case OAUTH2:
                return "OAuth 2.0 authentication";
            case CUSTOM:
                return "Custom authentication mechanism";
            default:
                return "Unknown authentication type";
        }
    }
}