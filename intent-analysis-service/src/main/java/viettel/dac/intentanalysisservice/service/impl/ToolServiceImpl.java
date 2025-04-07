package viettel.dac.intentanalysisservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.service.ToolService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced implementation of ToolService using Kafka to interact with Tool Registry Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolServiceImpl implements ToolService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.tool-registry-requests}")
    private String requestsTopic;

    @Value("${kafka.topic.tool-registry-responses}")
    private String responsesTopic;

    @Value("${tool.registry.request.timeout:10000}")
    private long requestTimeoutMs;

    // Create a map to store pending requests and their CompletableFutures
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolsFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "#toolIds != null ? #toolIds.toString() : 'all'", unless = "#result.isEmpty()")
    public List<ToolDTO> getTools(List<String> toolIds) {
        log.debug("Fetching tools with IDs: {}", toolIds);

        if (toolIds == null || toolIds.isEmpty()) {
            return getAllTools();
        }

        try {
            // Create a request ID
            String requestId = UUID.randomUUID().toString();

            // Create a request
            Map<String, Object> request = new HashMap<>();
            request.put("requestId", requestId);
            request.put("requestType", "GET_TOOLS_BY_IDS");
            request.put("toolIds", toolIds);
            request.put("timestamp", LocalDateTime.now());

            // Create a CompletableFuture for the response
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            pendingRequests.put(requestId, responseFuture);

            // Send the request
            kafkaTemplate.send(requestsTopic, requestId, request)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            responseFuture.completeExceptionally(ex);
                            log.error("Failed to send tool request: {}", ex.getMessage());
                        } else {
                            log.debug("Tool request sent successfully. Topic: {}, Partition: {}, Offset: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            log.debug("Sent request for tools with IDs: {}", toolIds);

            // Wait for the response with timeout
            String responseJson = responseFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            // Parse the response
            JsonNode responseNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = responseNode.path("data");

            // Convert to list of ToolDTO
            List<ToolDTO> tools = objectMapper.convertValue(
                    dataNode, new TypeReference<List<ToolDTO>>() {});

            log.debug("Received {} tools from Tool Registry Service", tools.size());
            return tools;
        } catch (Exception e) {
            log.error("Error fetching tools by IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolsByNamesFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'byNames:' + #toolNames.toString()", unless = "#result.isEmpty()")
    public List<ToolDTO> getToolsByNames(List<String> toolNames) {
        log.debug("Fetching tools with names: {}", toolNames);

        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolDTO> tools = new ArrayList<>();
        for (String name : toolNames) {
            ToolDTO tool = getToolByName(name);
            if (tool != null) {
                tools.add(tool);
            }
        }

        return tools;
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getAllToolsFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'all'", unless = "#result.isEmpty()")
    public List<ToolDTO> getAllTools() {
        log.debug("Fetching all tools");

        try {
            // Create a request ID
            String requestId = UUID.randomUUID().toString();

            // Create a request
            Map<String, Object> request = new HashMap<>();
            request.put("requestId", requestId);
            request.put("requestType", "GET_ALL_TOOLS");
            request.put("timestamp", LocalDateTime.now());

            // Create a CompletableFuture for the response
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            pendingRequests.put(requestId, responseFuture);

            // Send the request
            kafkaTemplate.send(requestsTopic, requestId, request);
            log.debug("Sent request for all tools");

            // Wait for the response with timeout
            String responseJson = responseFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            // Parse the response
            JsonNode responseNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = responseNode.path("data");

            // Convert to list of ToolDTO
            List<ToolDTO> tools = objectMapper.convertValue(
                    dataNode, new TypeReference<List<ToolDTO>>() {});

            log.debug("Received {} tools from Tool Registry Service", tools.size());
            return tools;
        } catch (Exception e) {
            log.error("Error fetching all tools: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolByIdFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'id:' + #toolId", unless = "#result == null")
    public ToolDTO getToolById(String toolId) {
        log.debug("Fetching tool with ID: {}", toolId);

        try {
            List<ToolDTO> tools = getTools(Collections.singletonList(toolId));
            return tools.isEmpty() ? null : tools.get(0);
        } catch (Exception e) {
            log.error("Error fetching tool by ID {}: {}", toolId, e.getMessage());
            return null;
        }
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolByNameFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'name:' + #name", unless = "#result == null")
    public ToolDTO getToolByName(String name) {
        log.debug("Fetching tool with name: {}", name);

        try {
            // Create a request ID
            String requestId = UUID.randomUUID().toString();

            // Create a request
            Map<String, Object> request = new HashMap<>();
            request.put("requestId", requestId);
            request.put("requestType", "GET_TOOL_BY_NAME");
            request.put("toolName", name);
            request.put("timestamp", LocalDateTime.now());

            // Create a CompletableFuture for the response
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            pendingRequests.put(requestId, responseFuture);

            // Send the request
            kafkaTemplate.send(requestsTopic, requestId, request);
            log.debug("Sent request for tool with name: {}", name);

            // Wait for the response with timeout
            String responseJson = responseFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            // Parse the response
            JsonNode responseNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = responseNode.path("data");

            // Check if the response has an error field
            if (dataNode.has("error")) {
                log.warn("Error response for tool name {}: {}", name, dataNode.path("message").asText());
                return null;
            }

            // Convert to ToolDTO
            ToolDTO tool = objectMapper.convertValue(dataNode, ToolDTO.class);
            log.debug("Received tool from Tool Registry Service: {}", tool.getName());
            return tool;
        } catch (Exception e) {
            log.error("Error fetching tool by name {}: {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Handles responses from the Tool Registry Service
     */
    @KafkaListener(topics = "${kafka.topic.tool-registry-responses}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleToolRegistryResponse(String responseJson, Acknowledgment ack) {
        try {
            JsonNode responseNode = objectMapper.readTree(responseJson);
            String requestId = responseNode.path("requestId").asText();

            log.debug("Received response for request: {}", requestId);

            // Complete the corresponding CompletableFuture
            CompletableFuture<String> future = pendingRequests.remove(requestId);
            if (future != null) {
                future.complete(responseJson);
                log.debug("Completed future for request: {}", requestId);
            } else {
                log.warn("Received response for unknown request: {}", requestId);
            }

            // Acknowledge message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling tool registry response: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    /**
     * Handles tool events from the Tool Registry Service
     */
    @KafkaListener(topics = "${kafka.topic.tool-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleToolEvent(String eventJson, Acknowledgment ack) {
        try {
            JsonNode eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.path("eventType").asText();
            String toolId = eventNode.path("toolId").asText();

            log.info("Received tool event: {} for tool: {}", eventType, toolId);

            // If tool is updated/created/deleted, evict from cache
            switch (eventType) {
                case "TOOL_CREATED":
                case "TOOL_UPDATED":
                case "TOOL_DELETED":
                    clearToolCache(toolId, eventNode.path("name").asText());
                    break;
                default:
                    log.debug("Ignoring event type: {}", eventType);
            }

            // Acknowledge message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling tool event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    /**
     * Clears caches for a tool.
     */
    @CacheEvict(value = "toolCache", allEntries = true)
    public void clearToolCache(String toolId, String toolName) {
        log.info("Clearing cache for tool: {} ({})", toolName, toolId);
    }

    // Fallback methods

    public List<ToolDTO> getToolsFallback(List<String> toolIds, Exception e) {
        log.warn("Using fallback for getTools: {}", e.getMessage());
        return Collections.emptyList();
    }

    public List<ToolDTO> getToolsByNamesFallback(List<String> toolNames, Exception e) {
        log.warn("Using fallback for getToolsByNames: {}", e.getMessage());
        return Collections.emptyList();
    }

    public List<ToolDTO> getAllToolsFallback(Exception e) {
        log.warn("Using fallback for getAllTools: {}", e.getMessage());
        return Collections.emptyList();
    }

    public ToolDTO getToolByIdFallback(String toolId, Exception e) {
        log.warn("Using fallback for getToolById: {}", e.getMessage());
        return null;
    }

    public ToolDTO getToolByNameFallback(String name, Exception e) {
        log.warn("Using fallback for getToolByName: {}", e.getMessage());
        return null;
    }
}