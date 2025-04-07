package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.model.dto.ToolDTO;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling tool registry requests from other services.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolRegistryRequestHandler {

    private final ToolRepository toolRepository;
    private final ToolQueryService toolQueryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.tool-registry-responses}")
    private String responsesTopic;

    /**
     * Handles requests from the tool-registry-requests topic.
     *
     * @param requestJson The request as a JSON string
     * @param ack The acknowledgment object for manual ack
     */
    @KafkaListener(topics = "${kafka.topic.tool-registry-requests}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleToolRegistryRequest(String requestJson, Acknowledgment ack) {
        try {
            JsonNode requestNode = objectMapper.readTree(requestJson);
            String requestType = requestNode.path("requestType").asText();
            String requestId = requestNode.path("requestId").asText();

            log.info("Received tool registry request: {} with ID: {}", requestType, requestId);

            Object response = null;

            // Process different request types
            switch (requestType) {
                case "GET_TOOL_BY_NAME":
                    String toolName = requestNode.path("toolName").asText();
                    response = getToolByName(toolName);
                    break;
                case "GET_TOOLS_BY_IDS":
                    List<String> toolIds = objectMapper.convertValue(
                            requestNode.path("toolIds"),
                            new TypeReference<List<String>>() {});
                    response = getToolsByIds(toolIds);
                    break;
                case "GET_ALL_TOOLS":
                    response = getAllActiveTools();
                    break;
                default:
                    log.warn("Unknown request type: {}", requestType);
                    response = Map.of(
                            "error", "Unknown request type",
                            "requestType", requestType
                    );
            }

            // Send response
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("requestId", requestId);
            responseMap.put("responseType", requestType + "_RESPONSE");
            responseMap.put("timestamp", LocalDateTime.now().toString());
            responseMap.put("data", response);

            kafkaTemplate.send(responsesTopic, requestId, responseMap)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send response for request {}: {}", requestId, ex.getMessage());
                        } else {
                            log.debug("Response sent for request {} to partition {} at offset {}",
                                    requestId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            log.info("Sent response for request: {}", requestId);

            // Acknowledge message
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling tool registry request: {}", e.getMessage(), e);
            ack.acknowledge(); // Acknowledge to prevent redelivery of malformed requests
        }
    }

    /**
     * Gets a tool by name.
     *
     * @param toolName The name of the tool
     * @return The tool DTO or an error message
     */
    private Object getToolByName(String toolName) {
        try {
            return toolQueryService.getToolByName(toolName);
        } catch (Exception e) {
            log.error("Error getting tool by name {}: {}", toolName, e.getMessage());
            return Map.of(
                    "error", "Tool not found or error occurred",
                    "toolName", toolName,
                    "message", e.getMessage()
            );
        }
    }

    /**
     * Gets tools by their IDs.
     *
     * @param toolIds The list of tool IDs
     * @return List of tool DTOs
     */
    private List<ToolDTO> getToolsByIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Use existing functionality to get tools by IDs
            return toolQueryService.getToolsByIds(toolIds);
        } catch (Exception e) {
            log.error("Error getting tools by IDs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all active tools.
     *
     * @return List of active tool DTOs
     */
    private List<ToolDTO> getAllActiveTools() {
        try {
            return toolQueryService.getAllActiveTools();
        } catch (Exception e) {
            log.error("Error getting all active tools: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}