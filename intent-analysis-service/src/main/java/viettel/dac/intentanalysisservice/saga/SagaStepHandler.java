package viettel.dac.intentanalysisservice.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.service.IntentAnalysisCommandService;
import viettel.dac.intentanalysisservice.service.ToolService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for processing saga commands from other services.
 * Implements the saga pattern for coordinating distributed transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaStepHandler {

    private final IntentAnalysisCommandService commandService;
    private final ToolService toolService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Listen for saga commands on the saga-commands topic.
     *
     * @param commandJson The saga command as JSON
     */
    @KafkaListener(topics = "saga-commands", groupId = "intent-analysis-service-saga")
    public void handleSagaCommand(String commandJson) {
        try {
            JsonNode root = objectMapper.readTree(commandJson);
            String commandType = root.get("commandType").asText();
            String sagaId = root.get("sagaId").asText();

            log.info("Received saga command: {}, sagaId: {}", commandType, sagaId);

            switch (commandType) {
                case "ANALYZE_INTENT":
                    handleAnalyzeIntentCommand(root, sagaId);
                    break;
                case "EXTRACT_PARAMETERS":
                    handleExtractParametersCommand(root, sagaId);
                    break;
                case "COMPENSATE_ANALYSIS":
                    handleCompensationCommand(root, sagaId);
                    break;
                default:
                    log.warn("Unknown saga command type: {}", commandType);
                    publishErrorEvent(sagaId, "Unknown command type: " + commandType);
            }
        } catch (Exception e) {
            log.error("Error handling saga command: {}", e.getMessage(), e);
            try {
                String sagaId = objectMapper.readTree(commandJson).get("sagaId").asText();
                publishErrorEvent(sagaId, e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to publish error event: {}", ex.getMessage());
            }
        }
    }

    /**
     * Handle an analyze intent command.
     *
     * @param command The command data
     * @param sagaId The saga ID
     */
    private void handleAnalyzeIntentCommand(JsonNode command, String sagaId) {
        log.debug("Processing ANALYZE_INTENT saga command for sagaId: {}", sagaId);

        try {
            // Extract fields from command
            String userInput = command.get("userInput").asText();
            String sessionId = command.get("sessionId").asText();
            String language = command.has("language") ? command.get("language").asText() : "en";

            // Extract toolIds if present
            List<String> toolIds = null;
            if (command.has("toolIds") && command.get("toolIds").isArray()) {
                toolIds = new ArrayList<>();
                for (JsonNode toolId : command.get("toolIds")) {
                    toolIds.add(toolId.asText());
                }
            }

            // Extract metadata if present
            Map<String, Object> metadata = null;
            if (command.has("metadata") && command.get("metadata").isObject()) {
                metadata = objectMapper.convertValue(command.get("metadata"), Map.class);
            }

            // Create command object
            AnalyzeIntentCommand analyzeCommand = AnalyzeIntentCommand.builder()
                    .analysisId(sagaId)
                    .userInput(userInput)
                    .sessionId(sessionId)
                    .toolIds(toolIds)
                    .language(language)
                    .metadata(metadata)
                    .build();

            // Execute command and get result
            String analysisId = commandService.executeAnalyzeIntentCommand(analyzeCommand);

            // Publish success event
            publishSuccessEvent(sagaId, "ANALYZE_INTENT", Map.of("analysisId", analysisId));

        } catch (Exception e) {
            log.error("Error executing analyze intent command: {}", e.getMessage(), e);
            publishErrorEvent(sagaId, "Failed to analyze intent: " + e.getMessage());
        }
    }

    /**
     * Handle an extract parameters command.
     *
     * @param command The command data
     * @param sagaId The saga ID
     */
    private void handleExtractParametersCommand(JsonNode command, String sagaId) {
        log.debug("Processing EXTRACT_PARAMETERS saga command for sagaId: {}", sagaId);

        try {
            // Extract fields from command
            String userInput = command.get("userInput").asText();
            String analysisId = command.has("analysisId") ? command.get("analysisId").asText() : sagaId;
            String language = command.has("language") ? command.get("language").asText() : "en";

            // Extract intents
            List<Intent> intents = new ArrayList<>();
            if (command.has("intents") && command.get("intents").isArray()) {
                for (JsonNode intentNode : command.get("intents")) {
                    String intentName = intentNode.get("intent").asText();
                    double confidence = intentNode.has("confidence") ?
                            intentNode.get("confidence").asDouble() : 0.9;

                    intents.add(new Intent(intentName, confidence));
                }
            } else {
                // If no intents provided, fetch tools and use as intents
                List<String> toolNames = new ArrayList<>();
                if (command.has("toolNames") && command.get("toolNames").isArray()) {
                    for (JsonNode toolName : command.get("toolNames")) {
                        toolNames.add(toolName.asText());
                    }
                }

                List<ToolDTO> tools;
                if (!toolNames.isEmpty()) {
                    tools = toolService.getToolsByNames(toolNames);
                } else {
                    tools = toolService.getAllTools();
                }

                // Convert tools to intents
                intents = tools.stream()
                        .map(tool -> new Intent(tool.getName(), 0.9))
                        .collect(Collectors.toList());
            }

            // Extract metadata if present
            Map<String, Object> metadata = null;
            if (command.has("metadata") && command.get("metadata").isObject()) {
                metadata = objectMapper.convertValue(command.get("metadata"), Map.class);
            }

            // Create command object
            ExtractParametersCommand extractCommand = ExtractParametersCommand.builder()
                    .analysisId(analysisId)
                    .userInput(userInput)
                    .intents(intents)
                    .language(language)
                    .metadata(metadata)
                    .build();

            // Execute command and get result
            List<IntentWithParameters> result = commandService.executeExtractParametersCommand(extractCommand);

            // Convert result to a map for the event
            List<Map<String, Object>> resultMaps = result.stream()
                    .map(iwp -> Map.of(
                            "intent", iwp.getIntent(),
                            "parameters", iwp.getParameters(),
                            "confidence", iwp.getConfidence(),
                            "state", iwp.getState()))
                    .collect(Collectors.toList());

            // Publish success event
            publishSuccessEvent(sagaId, "EXTRACT_PARAMETERS", Map.of(
                    "analysisId", analysisId,
                    "intentsWithParameters", resultMaps
            ));

        } catch (Exception e) {
            log.error("Error executing extract parameters command: {}", e.getMessage(), e);
            publishErrorEvent(sagaId, "Failed to extract parameters: " + e.getMessage());
        }
    }

    /**
     * Handle a compensation command for rollback.
     *
     * @param command The command data
     * @param sagaId The saga ID
     */
    private void handleCompensationCommand(JsonNode command, String sagaId) {
        log.debug("Processing COMPENSATE_ANALYSIS saga command for sagaId: {}", sagaId);

        try {
            // Extract fields from command
            String analysisId = command.has("analysisId") ? command.get("analysisId").asText() : sagaId;
            String reason = command.has("reason") ? command.get("reason").asText() : "Saga compensation";

            // Perform compensation logic
            // In this case, we mark the analysis as failed in our system
            boolean success = commandService.cancelAnalysis(analysisId, reason);

            // Publish compensation result
            if (success) {
                publishSuccessEvent(sagaId, "COMPENSATE_ANALYSIS", Map.of(
                        "analysisId", analysisId,
                        "status", "compensated",
                        "reason", reason
                ));
            } else {
                publishErrorEvent(sagaId, "Failed to compensate analysis: Analysis not found");
            }

        } catch (Exception e) {
            log.error("Error executing compensation command: {}", e.getMessage(), e);
            publishErrorEvent(sagaId, "Failed to compensate: " + e.getMessage());
        }
    }

    /**
     * Publish a success event for a saga step.
     *
     * @param sagaId The saga ID
     * @param step The step that completed successfully
     * @param result The result data
     */
    private void publishSuccessEvent(String sagaId, String step, Map<String, Object> result) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("eventType", "SAGA_STEP_COMPLETED");
            event.put("sagaId", sagaId);
            event.put("step", step);
            event.put("service", "intent-analysis-service");
            event.put("timestamp", LocalDateTime.now().toString());

            // Add result data
            ObjectNode resultNode = event.putObject("result");
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                resultNode.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
            }

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("saga-events", eventJson);
            log.debug("Published saga step completed event for step: {}, sagaId: {}", step, sagaId);

        } catch (Exception e) {
            log.error("Error publishing saga success event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish an error event for a saga step.
     *
     * @param sagaId The saga ID
     * @param errorMessage The error message
     */
    private void publishErrorEvent(String sagaId, String errorMessage) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("eventType", "SAGA_STEP_FAILED");
            event.put("sagaId", sagaId);
            event.put("service", "intent-analysis-service");
            event.put("timestamp", LocalDateTime.now().toString());
            event.put("errorMessage", errorMessage);

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("saga-events", eventJson);
            log.debug("Published saga step failed event for sagaId: {}, error: {}", sagaId, errorMessage);

        } catch (Exception e) {
            log.error("Error publishing saga error event: {}", e.getMessage(), e);
        }
    }
}