package viettel.dac.intentanalysisservice.query.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.event.IntentAnalysisCompletedEvent;
import viettel.dac.intentanalysisservice.event.IntentAnalysisFailedEvent;
import viettel.dac.intentanalysisservice.event.IntentAnalysisStartedEvent;
import viettel.dac.intentanalysisservice.event.ParametersExtractedEvent;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.model.document.IntentAnalysisDocument;
import viettel.dac.intentanalysisservice.model.document.IntentDocument;
import viettel.dac.intentanalysisservice.query.repository.IntentAnalysisRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Event processor for intent analysis events to update the read model.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisEventProcessor {

    private final IntentAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Process intent analysis events from Kafka.
     *
     * @param eventJson The event message as JSON string
     */
    @KafkaListener(topics = "intent-analysis-events", groupId = "intent-analysis-service")
    public void processEvent(String eventJson) {
        try {
            // Determine event type
            JsonNode root = objectMapper.readTree(eventJson);
            String eventType = root.get("eventType").asText();

            switch (eventType) {
                case "INTENT_ANALYSIS_STARTED":
                    IntentAnalysisStartedEvent startedEvent =
                            objectMapper.readValue(eventJson, IntentAnalysisStartedEvent.class);
                    handleAnalysisStarted(startedEvent);
                    break;

                case "INTENT_ANALYSIS_COMPLETED":
                    IntentAnalysisCompletedEvent completedEvent =
                            objectMapper.readValue(eventJson, IntentAnalysisCompletedEvent.class);
                    handleAnalysisCompleted(completedEvent);
                    break;

                case "PARAMETERS_EXTRACTED":
                    ParametersExtractedEvent extractedEvent =
                            objectMapper.readValue(eventJson, ParametersExtractedEvent.class);
                    handleParametersExtracted(extractedEvent);
                    break;

                case "INTENT_ANALYSIS_FAILED":
                    IntentAnalysisFailedEvent failedEvent =
                            objectMapper.readValue(eventJson, IntentAnalysisFailedEvent.class);
                    handleAnalysisFailed(failedEvent);
                    break;

                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle an intent analysis started event.
     *
     * @param event The started event
     */
    private void handleAnalysisStarted(IntentAnalysisStartedEvent event) {
        log.debug("Processing IntentAnalysisStartedEvent: {}", event.getEventId());

        // Create initial document with status pending
        IntentAnalysisDocument document = IntentAnalysisDocument.builder()
                .analysisId(event.getAnalysisId())
                .userInput(event.getUserInput())
                .sessionId(event.getSessionId())
                .status(0) // Pending
                .timestamp(event.getTimestamp())
                .metadata(event.getMetadata())
                .build();

        repository.save(document);
        log.debug("Created initial analysis document with ID: {}", event.getAnalysisId());
    }

    /**
     * Handle an intent analysis completed event.
     *
     * @param event The completed event
     */
    private void handleAnalysisCompleted(IntentAnalysisCompletedEvent event) {
        log.debug("Processing IntentAnalysisCompletedEvent: {}", event.getEventId());

        // Find existing document or create new one
        Optional<IntentAnalysisDocument> existingDoc = repository.findById(event.getAnalysisId());

        IntentAnalysisDocument document;
        if (existingDoc.isPresent()) {
            document = existingDoc.get();
        } else {
            document = new IntentAnalysisDocument();
            document.setAnalysisId(event.getAnalysisId());
        }

        // Update document with completed analysis information
        document.setUserInput(event.getUserInput());
        document.setSessionId(event.getSessionId());
        document.setConfidence(event.getConfidence());
        document.setMultiIntent(event.getIntents().size() > 1);
        document.setProcessingTimeMs(event.getProcessingTimeMs());
        document.setStatus(1); // Active
        document.setTimestamp(event.getTimestamp());
        document.setMetadata(event.getMetadata());

        // Convert intents
        List<IntentDocument> intentDocuments = mapIntents(event.getIntents());
        document.setIntents(intentDocuments);

        repository.save(document);
        log.debug("Updated analysis document with intents for ID: {}", event.getAnalysisId());
    }

    /**
     * Handle a parameters extracted event.
     *
     * @param event The parameters extracted event
     */
    private void handleParametersExtracted(ParametersExtractedEvent event) {
        log.debug("Processing ParametersExtractedEvent: {}", event.getEventId());

        // Find existing document
        Optional<IntentAnalysisDocument> existingDoc = repository.findById(event.getAnalysisId());

        if (existingDoc.isPresent()) {
            IntentAnalysisDocument document = existingDoc.get();

            // Update document with parameter information
            document.setConfidence(event.getConfidence());
            document.setMultiIntent(event.isMultiIntent());
            document.setStatus(event.getStatus());

            // Convert intents with parameters
            List<IntentDocument> intentDocuments = mapIntentsWithParameters(event.getIntents());
            document.setIntents(intentDocuments);

            repository.save(document);
            log.debug("Updated analysis document with parameters for ID: {}", event.getAnalysisId());
        } else {
            log.warn("Could not find analysis document with ID: {}", event.getAnalysisId());
        }
    }

    /**
     * Handle an intent analysis failed event.
     *
     * @param event The failed event
     */
    private void handleAnalysisFailed(IntentAnalysisFailedEvent event) {
        log.debug("Processing IntentAnalysisFailedEvent: {}", event.getEventId());

        // Find existing document or create new one
        Optional<IntentAnalysisDocument> existingDoc = repository.findById(event.getAnalysisId());

        IntentAnalysisDocument document;
        if (existingDoc.isPresent()) {
            document = existingDoc.get();
        } else {
            document = new IntentAnalysisDocument();
            document.setAnalysisId(event.getAnalysisId());
        }

        // Update document with failure information
        document.setUserInput(event.getUserInput());
        document.setSessionId(event.getSessionId());
        document.setStatus(3); // Failed
        document.setTimestamp(event.getTimestamp());

        // Store error information in metadata
        Map<String, Object> errorMetadata = new HashMap<>();
        if (event.getMetadata() != null) {
            errorMetadata.putAll(event.getMetadata());
        }
        errorMetadata.put("errorMessage", event.getErrorMessage());
        errorMetadata.put("errorType", event.getErrorType());
        errorMetadata.put("failedStep", event.getFailedStep());
        document.setMetadata(errorMetadata);

        repository.save(document);
        log.debug("Updated analysis document with failure info for ID: {}", event.getAnalysisId());
    }

    /**
     * Map intents to intent documents.
     *
     * @param intents List of intents
     * @return List of intent documents
     */
    private List<IntentDocument> mapIntents(List<Intent> intents) {
        if (intents == null) {
            return new ArrayList<>();
        }

        return intents.stream()
                .map(intent -> {
                    IntentDocument doc = new IntentDocument();
                    doc.setIntent(intent.getIntent());
                    doc.setConfidence(intent.getConfidence());
                    doc.setState(0);
                    doc.setParameters(new HashMap<>());
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * Map intents with parameters to intent documents.
     *
     * @param intents List of intents with parameters
     * @return List of intent documents
     */
    private List<IntentDocument> mapIntentsWithParameters(List<IntentWithParameters> intents) {
        if (intents == null) {
            return new ArrayList<>();
        }

        return intents.stream()
                .map(intent -> {
                    IntentDocument doc = new IntentDocument();
                    doc.setIntent(intent.getIntent());
                    doc.setConfidence(intent.getConfidence());
                    doc.setState(intent.getState());
                    doc.setParameters(intent.getParameters() != null ? intent.getParameters() : new HashMap<>());
                    return doc;
                })
                .collect(Collectors.toList());
    }
}
