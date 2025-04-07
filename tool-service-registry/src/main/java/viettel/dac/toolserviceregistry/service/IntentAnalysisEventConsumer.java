package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumer for intent analysis events from the Intent Analysis Service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IntentAnalysisEventConsumer {

    private final ToolRepository toolRepository;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.intent-analysis-events}")
    private String intentAnalysisEventsTopic;

    /**
     * Consumes events from the intent-analysis-events topic.
     *
     * @param eventJson The event as a JSON string
     * @param ack The acknowledgment object for manual ack
     */
    @KafkaListener(topics = "${kafka.topic.intent-analysis-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeIntentAnalysisEvent(String eventJson, Acknowledgment ack) {
        try {
            // Ensure object mapper can handle Java 8 date/time
            if (!objectMapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
                objectMapper.registerModule(new JavaTimeModule());
            }

            // Parse event JSON
            JsonNode eventNode = objectMapper.readTree(eventJson);

            String eventType = eventNode.path("eventType").asText();
            String analysisId = eventNode.path("analysisId").asText();

            log.info("Received intent analysis event: {} for analysis: {}", eventType, analysisId);

            // Process different event types
            switch (eventType) {
                case "ANALYSIS_COMPLETED":
                    processCompletedAnalysis(eventNode);
                    break;
                case "ANALYSIS_FAILED":
                    processFailedAnalysis(eventNode);
                    break;
                case "ANALYSIS_STARTED":
                    // Could track that an analysis has started
                    log.debug("Analysis started: {}", analysisId);
                    break;
                default:
                    log.debug("Ignoring event type: {}", eventType);
            }

            // Acknowledge message
            ack.acknowledge();
            log.debug("Acknowledged event: {}", analysisId);
        } catch (Exception e) {
            log.error("Error processing intent analysis event: {}", e.getMessage(), e);
            // Acknowledge to prevent redelivery of poison pill messages
            // In production, you might want more sophisticated handling
            ack.acknowledge();
        }
    }

    /**
     * Processes a completed analysis event.
     *
     * @param eventNode The event as a JsonNode
     */
    private void processCompletedAnalysis(JsonNode eventNode) {
        // Extract relevant data
        JsonNode intentsNode = eventNode.path("intents");
        if (intentsNode.isArray() && intentsNode.size() > 0) {
            // Process each intent
            List<String> processedTools = new ArrayList<>();

            for (JsonNode intentNode : intentsNode) {
                String intentName = intentNode.path("intent").asText();
                double confidence = intentNode.path("confidence").asDouble();

                // Skip if we already processed this tool (can happen with multi-intent)
                if (processedTools.contains(intentName)) {
                    continue;
                }

                // Update tool usage metrics
                toolRepository.findByName(intentName).ifPresent(tool -> {
                    metricsService.recordToolUsage(tool.getId(), confidence);
                    processedTools.add(intentName);
                    log.debug("Recorded metrics for tool: {} with confidence: {}", tool.getName(), confidence);
                });
            }

            log.info("Processed {} intents for analysis: {}", processedTools.size(), eventNode.path("analysisId").asText());
        } else {
            log.warn("No intents found in completed analysis: {}", eventNode.path("analysisId").asText());
        }
    }

    /**
     * Processes a failed analysis event.
     *
     * @param eventNode The event as a JsonNode
     */
    private void processFailedAnalysis(JsonNode eventNode) {
        String errorType = eventNode.path("errorType").asText();
        String userInput = eventNode.path("userInput").asText();
        String sessionId = eventNode.path("sessionId").asText();

        log.warn("Analysis failed. SessionId: {}, ErrorType: {}, UserInput: '{}'",
                sessionId, errorType, userInput);

        // Record failure metrics (generic failure without specific tool)
        metricsService.recordToolUsageFailure("system", errorType);
    }
}