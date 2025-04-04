// File: src/main/java/viettel/dac/toolserviceregistry/service/ApiToolService.java
package viettel.dac.toolserviceregistry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.ApiMetadataNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolTypeNotCompatibleException;
import viettel.dac.toolserviceregistry.model.dto.ApiHeaderDTO;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.entity.ApiHeader;
import viettel.dac.toolserviceregistry.model.entity.ApiToolMetadata;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.request.ApiHeaderRequest;
import viettel.dac.toolserviceregistry.model.request.ApiToolMetadataRequest;
import viettel.dac.toolserviceregistry.repository.ApiToolMetadataRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for API tool operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiToolService {
    private final ToolRepository toolRepository;
    private final ApiToolMetadataRepository apiToolMetadataRepository;

    /**
     * Gets API metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @return The API metadata DTO
     */
    public ApiToolMetadataDTO getApiMetadata(String toolId) {
        log.info("Getting API metadata for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        return apiToolMetadataRepository.findByToolId(toolId)
                .map(this::mapToApiToolMetadataDTO)
                .orElse(null);
    }

    /**
     * Updates API metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @param request The API metadata update request
     * @return The updated API metadata DTO
     */
    @Transactional
    public ApiToolMetadataDTO updateApiMetadata(String toolId, ApiToolMetadataRequest request) {
        log.info("Updating API metadata for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        // Find existing API metadata or create new
        ApiToolMetadata metadata = apiToolMetadataRepository.findByToolId(toolId)
                .orElse(new ApiToolMetadata());

        if (metadata.getId() == null) {
            metadata.setId(UUID.randomUUID().toString());
            metadata.setTool(tool);
        }

        // Update metadata fields
        metadata.setBaseUrl(request.getBaseUrl());
        metadata.setEndpointPath(request.getEndpointPath());
        metadata.setHttpMethod(request.getHttpMethod());
        metadata.setContentType(request.getContentType());
        metadata.setAuthenticationType(request.getAuthenticationType());
        metadata.setRequestTimeoutMs(request.getRequestTimeoutMs());
        metadata.setResponseFormat(request.getResponseFormat());
        metadata.setRateLimitRequests(request.getRateLimitRequests());
        metadata.setRateLimitPeriodSeconds(request.getRateLimitPeriodSeconds());
        metadata.setRetryCount(request.getRetryCount());
        metadata.setRetryDelayMs(request.getRetryDelayMs());

        // Update headers (remove all and re-add)
        metadata.getHeaders().clear();

        if (request.getHeaders() != null) {
            for (ApiHeaderRequest headerReq : request.getHeaders()) {
                ApiHeader header = mapToApiHeader(headerReq);
                header.setId(headerReq.getId() != null ?
                        headerReq.getId() : UUID.randomUUID().toString());
                metadata.addHeader(header);
            }
        }

        // Save metadata
        ApiToolMetadata savedMetadata = apiToolMetadataRepository.save(metadata);

        return mapToApiToolMetadataDTO(savedMetadata);
    }

    /**
     * Tests an API call using the tool's metadata.
     *
     * @param toolId The ID of the tool
     * @param parameters Optional parameters to override defaults
     * @return The API response data
     */
    public String testApiCall(String toolId, Map<String, Object> parameters) {
        log.info("Testing API call for tool: {}", toolId);

        // Get API metadata
        ApiToolMetadataDTO metadata = getApiMetadata(toolId);
        if (metadata == null) {
            throw new ApiMetadataNotFoundException(toolId);
        }

        // TODO: Implement actual API call logic
        // This is a placeholder for actual implementation

        return "API call successful. Response data would be shown here.";
    }

    /**
     * Maps an ApiToolMetadata entity to an ApiToolMetadataDTO.
     *
     * @param metadata The API metadata entity
     * @return The API metadata DTO
     */
    ApiToolMetadataDTO mapToApiToolMetadataDTO(ApiToolMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return ApiToolMetadataDTO.builder()
                .id(metadata.getId())
                .baseUrl(metadata.getBaseUrl())
                .endpointPath(metadata.getEndpointPath())
                .httpMethod(metadata.getHttpMethod())
                .contentType(metadata.getContentType())
                .authenticationType(metadata.getAuthenticationType())
                .requestTimeoutMs(metadata.getRequestTimeoutMs())
                .responseFormat(metadata.getResponseFormat())
                .headers(metadata.getHeaders().stream()
                        .map(this::mapToApiHeaderDTO)
                        .collect(Collectors.toList()))
                .rateLimitRequests(metadata.getRateLimitRequests())
                .rateLimitPeriodSeconds(metadata.getRateLimitPeriodSeconds())
                .retryCount(metadata.getRetryCount())
                .retryDelayMs(metadata.getRetryDelayMs())
                .build();
    }

    /**
     * Maps an ApiHeader entity to an ApiHeaderDTO.
     *
     * @param header The API header entity
     * @return The API header DTO
     */
    private ApiHeaderDTO mapToApiHeaderDTO(ApiHeader header) {
        if (header == null) {
            return null;
        }

        ApiHeaderDTO dto = new ApiHeaderDTO();
        dto.setId(header.getId());
        dto.setName(header.getName());
        dto.setValue(header.isSensitive() ? "********" : header.getValue());
        dto.setRequired(header.isRequired());
        dto.setSensitive(header.isSensitive());
        return dto;
    }

    /**
     * Maps an ApiHeaderRequest to an ApiHeader entity.
     *
     * @param request The API header request
     * @return The API header entity
     */
    private ApiHeader mapToApiHeader(ApiHeaderRequest request) {
        ApiHeader header = new ApiHeader();
        header.setName(request.getName());
        header.setValue(request.getValue());
        header.setRequired(request.isRequired());
        header.setSensitive(request.isSensitive());
        return header;
    }
}