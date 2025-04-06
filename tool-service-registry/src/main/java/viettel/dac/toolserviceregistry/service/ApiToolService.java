package viettel.dac.toolserviceregistry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.ApiMetadataNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolTypeNotCompatibleException;
import viettel.dac.toolserviceregistry.model.dto.ApiHeaderDTO;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.entity.*;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.request.ApiHeaderRequest;
import viettel.dac.toolserviceregistry.model.request.ApiToolMetadataRequest;
import viettel.dac.toolserviceregistry.repository.ApiParameterMappingRepository;
import viettel.dac.toolserviceregistry.repository.ApiToolMetadataRepository;
import viettel.dac.toolserviceregistry.repository.ToolParameterRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced service for API tool operations.
 * Added support for optimized API execution and response caching.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiToolService {
    private final ToolRepository toolRepository;
    private final ApiToolMetadataRepository apiToolMetadataRepository;
    private final ToolParameterRepository toolParameterRepository;
    private final ApiParameterMappingRepository apiParameterMappingRepository;
    private final ApiResponseParser apiResponseParser;

    // Cache for API responses
    private final ConcurrentHashMap<String, ApiResponseCacheEntry> responseCache = new ConcurrentHashMap<>();

    // Maximum cache size
    private static final int MAX_CACHE_ENTRIES = 1000;
    // Default cache TTL in milliseconds
    private static final long DEFAULT_CACHE_TTL_MS = 300000; // 5 minutes

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
    @CacheEvict(cacheNames = "apiMetadata", key = "#toolId")
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

        // Clear response cache for this tool
        clearApiResponseCache(toolId);

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

        // Generate cache key
        String cacheKey = generateApiCacheKey(toolId, parameters);

        // Check cache first
        ApiResponseCacheEntry cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null && !cachedResponse.isExpired()) {
            log.debug("Returning cached API response for tool: {}", toolId);
            return cachedResponse.getResponse();
        }

        // TODO: Implement actual API call logic
        // This is a placeholder for actual implementation
        String responseData = simulateApiCall(metadata, parameters);

        // Cache the response
        cacheApiResponse(cacheKey, responseData, metadata.getRequestTimeoutMs());

        return responseData;
    }

    /**
     * Simulates an API call (placeholder for real implementation).
     *
     * @param metadata The API metadata
     * @param parameters The parameters to use
     * @return Simulated API response
     */
    private String simulateApiCall(ApiToolMetadataDTO metadata, Map<String, Object> parameters) {
        // This is a placeholder for actual API call implementation
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("{\n");
        responseBuilder.append("  \"status\": \"success\",\n");
        responseBuilder.append("  \"data\": {\n");

        if (parameters != null && !parameters.isEmpty()) {
            responseBuilder.append("    \"parameters\": {\n");
            int count = 0;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                responseBuilder.append("      \"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof String) {
                    responseBuilder.append("\"").append(entry.getValue()).append("\"");
                } else {
                    responseBuilder.append(entry.getValue());
                }
                if (count < parameters.size() - 1) {
                    responseBuilder.append(",");
                }
                responseBuilder.append("\n");
                count++;
            }
            responseBuilder.append("    },\n");
        }

        responseBuilder.append("    \"endpoint\": \"").append(metadata.getBaseUrl())
                .append(metadata.getEndpointPath()).append("\",\n");
        responseBuilder.append("    \"method\": \"").append(metadata.getHttpMethod()).append("\",\n");
        responseBuilder.append("    \"timestamp\": ").append(System.currentTimeMillis()).append("\n");
        responseBuilder.append("  }\n");
        responseBuilder.append("}\n");

        return responseBuilder.toString();
    }

    /**
     * Maps an ApiToolMetadata entity to an ApiToolMetadataDTO.
     *
     * @param metadata The API metadata entity
     * @return The API metadata DTO
     */
    @Cacheable(cacheNames = "apiMetadataMapping", key = "#metadata.id")
    public ApiToolMetadataDTO mapToApiToolMetadataDTO(ApiToolMetadata metadata) {
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

    /**
     * Update parameter sources based on API parameter mappings.
     *
     * @param toolId The ID of the tool
     */
    @Transactional
    public void updateParameterSources(String toolId) {
        log.debug("Updating parameter sources for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        ApiToolMetadata metadata = apiToolMetadataRepository.findByToolId(toolId)
                .orElseThrow(() -> new ApiMetadataNotFoundException(toolId));

        // Get all parameter mappings for this API tool
        List<ApiParameterMapping> mappings = apiParameterMappingRepository
                .findByApiToolMetadataId(metadata.getId());

        // Update parameter sources based on mappings
        for (ApiParameterMapping mapping : mappings) {
            ToolParameter parameter = mapping.getToolParameter();

            if (mapping.getApiLocation() == ApiParameterLocation.RESPONSE) {
                parameter.setParameterSource(ParameterSource.API_RESPONSE);
                parameter.setExtractionPath(mapping.getResponseExtractionPath());
            } else {
                // For request parameters, set the source based on the parameter's properties
                if (parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()) {
                    parameter.setParameterSource(ParameterSource.DEFAULT_VALUE);
                } else {
                    parameter.setParameterSource(ParameterSource.USER_INPUT);
                }
            }

            toolParameterRepository.save(parameter);
        }
    }

    /**
     * Gets API metadata for a tool by ID.
     *
     * @param toolId The ID of the tool
     * @return The API metadata DTO or null if not found
     */
    @Cacheable(cacheNames = "apiMetadata", key = "#toolId")
    public ApiToolMetadataDTO getApiToolMetadataDTO(String toolId) {
        log.debug("Getting API metadata DTO for tool: {}", toolId);

        return apiToolMetadataRepository.findByToolId(toolId)
                .map(this::mapToApiToolMetadataDTO)
                .orElse(null);
    }

    /**
     * Caches an API response.
     *
     * @param cacheKey The cache key
     * @param response The API response
     * @param ttlMs The time-to-live in milliseconds, or null for default
     */
    private void cacheApiResponse(String cacheKey, String response, Integer ttlMs) {
        // Use default TTL if not specified
        long expiryTime = System.currentTimeMillis() + (ttlMs != null ? ttlMs : DEFAULT_CACHE_TTL_MS);

        // Create cache entry
        ApiResponseCacheEntry cacheEntry = new ApiResponseCacheEntry(response, expiryTime);

        // Add to cache
        responseCache.put(cacheKey, cacheEntry);

        // Prune cache if needed
        pruneCache();
    }

    /**
     * Clears the API response cache for a specific tool.
     *
     * @param toolId The ID of the tool
     */
    public void clearApiResponseCache(String toolId) {
        log.debug("Clearing API response cache for tool: {}", toolId);

        // Remove all cache entries with matching tool ID
        Set<String> keysToRemove = responseCache.keySet().stream()
                .filter(key -> key.startsWith(toolId + "-"))
                .collect(Collectors.toSet());

        for (String key : keysToRemove) {
            responseCache.remove(key);
        }
    }

    /**
     * Clears all API response caches.
     */
    public void clearAllApiResponseCaches() {
        log.debug("Clearing all API response caches");
        responseCache.clear();
    }

    /**
     * Prunes the cache if it exceeds the maximum size.
     */
    private void pruneCache() {
        if (responseCache.size() <= MAX_CACHE_ENTRIES) {
            return;
        }

        log.debug("Pruning API response cache (size: {})", responseCache.size());

        // Remove expired entries
        List<String> expiredKeys = responseCache.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String key : expiredKeys) {
            responseCache.remove(key);
        }

        // If still too many entries, remove oldest
        if (responseCache.size() > MAX_CACHE_ENTRIES) {
            List<Map.Entry<String, ApiResponseCacheEntry>> entries = new ArrayList<>(responseCache.entrySet());
            entries.sort(Comparator.comparingLong(e -> e.getValue().getExpiryTime()));

            int entriesToRemove = entries.size() - MAX_CACHE_ENTRIES;
            for (int i = 0; i < entriesToRemove; i++) {
                responseCache.remove(entries.get(i).getKey());
            }
        }
    }

    /**
     * Generates a cache key for an API call.
     *
     * @param toolId The ID of the tool
     * @param parameters The parameters for the call
     * @return The cache key
     */
    private String generateApiCacheKey(String toolId, Map<String, Object> parameters) {
        StringBuilder keyBuilder = new StringBuilder(toolId).append("-");

        if (parameters != null && !parameters.isEmpty()) {
            List<String> paramParts = new ArrayList<>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                paramParts.add(entry.getKey() + "=" + (entry.getValue() != null ? entry.getValue().toString() : "null"));
            }
            // Sort for consistent keys
            Collections.sort(paramParts);
            keyBuilder.append(String.join("&", paramParts));
        } else {
            keyBuilder.append("no-params");
        }

        return keyBuilder.toString();
    }

    /**
     * Gets statistics about the API response cache.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", responseCache.size());
        stats.put("maxCacheSize", MAX_CACHE_ENTRIES);

        long expiredCount = responseCache.values().stream()
                .filter(ApiResponseCacheEntry::isExpired)
                .count();
        stats.put("expiredEntries", expiredCount);

        long totalSize = responseCache.values().stream()
                .mapToLong(entry -> entry.getResponse().length())
                .sum();
        stats.put("totalByteSize", totalSize);

        return stats;
    }

    /**
     * Inner class for API response cache entries.
     */
    private static class ApiResponseCacheEntry {
        private final String response;
        private final long expiryTime;

        public ApiResponseCacheEntry(String response, long expiryTime) {
            this.response = response;
            this.expiryTime = expiryTime;
        }

        public String getResponse() {
            return response;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}