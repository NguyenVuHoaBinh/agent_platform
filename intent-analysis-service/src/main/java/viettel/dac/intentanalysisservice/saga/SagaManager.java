package viettel.dac.intentanalysisservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manager for initiating sagas and publishing saga commands.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaManager {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Start a new intent processing saga.
     *
     * @param userInput The user input text
     * @param sessionId The session ID
     * @param toolIds Optional list of tool IDs to consider
     * @return The saga ID
     */
    public String startIntentProcessingSaga(String userInput, String sessionId, List<String> toolIds) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Starting intent processing saga with ID: {}", sagaId);

        try {
            // Create saga start event
            Map<String, Object> sagaData = Map.of(
                    "sagaId", sagaId,
                    "sagaType", "INTENT_PROCESSING",
                    "initiatingService", "intent-analysis-service",
                    "userInput", userInput,
                    "sessionId", sessionId,
                    "toolIds", toolIds != null ? toolIds : List.of(),
                    "timestamp", System.currentTimeMillis()
            );

            String sagaJson = objectMapper.writeValueAsString(sagaData);
            kafkaTemplate.send("saga-start", sagaJson);
            log.debug("Published saga start event for ID: {}", sagaId);

            return sagaId;

        } catch (Exception e) {
            log.error("Error starting saga: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start saga", e);
        }
    }

    /**
     * Publish a command to the next step in the saga.
     *
     * @param sagaId The saga ID
     * @param commandType The command type
     * @param commandData The command data
     */
    public void publishNextCommand(String sagaId, String commandType, Map<String, Object> commandData) {
        try {
            Map<String, Object> command = Map.of(
                    "sagaId", sagaId,
                    "commandType", commandType,
                    "sourceService", "intent-analysis-service",
                    "timestamp", System.currentTimeMillis(),
                    "data", commandData
            );

            String commandJson = objectMapper.writeValueAsString(command);
            kafkaTemplate.send("saga-commands", commandJson);
            log.debug("Published next command for saga ID: {}, type: {}", sagaId, commandType);

        } catch (Exception e) {
            log.error("Error publishing next command: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish next command", e);
        }
    }

    /**
     * Publish a completion event for the saga.
     *
     * @param sagaId The saga ID
     * @param intents The identified intents with parameters
     */
    public void completeSaga(String sagaId, List<IntentWithParameters> intents) {
        try {
            List<Map<String, Object>> intentMaps = intents.stream()
                    .map(iwp -> Map.of(
                            "intent", iwp.getIntent(),
                            "parameters", iwp.getParameters(),
                            "confidence", iwp.getConfidence()
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> completionData = Map.of(
                    "sagaId", sagaId,
                    "status", "COMPLETED",
                    "service", "intent-analysis-service",
                    "timestamp", System.currentTimeMillis(),
                    "result", Map.of(
                            "intents", intentMaps,
                            "multiIntent", intents.size() > 1
                    )
            );

            String completionJson = objectMapper.writeValueAsString(completionData);
            kafkaTemplate.send("saga-completion", completionJson);
            log.debug("Published saga completion event for ID: {}", sagaId);

        } catch (Exception e) {
            log.error("Error completing saga: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete saga", e);
        }
    }

    /**
     * Publish a failure event for the saga.
     *
     * @param sagaId The saga ID
     * @param errorMessage The error message
     * @param errorType The error type
     */
    public void failSaga(String sagaId, String errorMessage, String errorType) {
        try {
            Map<String, Object> failureData = Map.of(
                    "sagaId", sagaId,
                    "status", "FAILED",
                    "service", "intent-analysis-service",
                    "timestamp", System.currentTimeMillis(),
                    "error", Map.of(
                            "message", errorMessage,
                            "type", errorType
                    )
            );

            String failureJson = objectMapper.writeValueAsString(failureData);
            kafkaTemplate.send("saga-completion", failureJson);
            log.debug("Published saga failure event for ID: {}", sagaId);

        } catch (Exception e) {
            log.error("Error publishing saga failure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish saga failure", e);
        }
    }
}
