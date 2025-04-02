package viettel.dac.intentanalysisservice.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.event.*;
import viettel.dac.intentanalysisservice.llm.LLMClient;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.service.PromptTemplateService;
import viettel.dac.intentanalysisservice.service.ToolService;
import viettel.dac.intentanalysisservice.util.JsonUtil;
import viettel.dac.intentanalysisservice.validator.IntentCommandValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for intent analysis commands.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisCommandHandler {

    private final LLMClient llmClient;
    private final PromptTemplateService promptService;
    private final ToolService toolService;
    private final EventPublisher eventPublisher;
    private final IntentCommandValidator validator;
    private final JsonUtil jsonUtil;

    /**
     * Handle a command to analyze user input for intents.
     *
     * @param command The analyze intent command
     * @return The analysis ID
     */
    public String handleAnalyzeIntent(AnalyzeIntentCommand command) {
        try {
            // Validate command
            validator.validateAnalyzeIntent(command);

            // Generate ID if not provided
            String analysisId = command.getAnalysisId() != null ?
                    command.getAnalysisId() : UUID.randomUUID().toString();

            // Publish started event
            publishAnalysisStartedEvent(analysisId, command);

            // Fetch tools
            List<ToolDTO> tools = toolService.getTools(command.getToolIds());
            if (tools.isEmpty()) {
                log.warn("No tools available for analysis");
                tools = toolService.getAllTools();
                if (tools.isEmpty()) {
                    throw new RuntimeException("No tools available for analysis");
                }
            }

            // Generate prompt
            String prompt = promptService.createIntentAnalysisPrompt(
                    command.getUserInput(), tools);

            log.debug("Generated intent analysis prompt: {}", prompt);

            // Get LLM completion
            long startTime = System.currentTimeMillis();
            String llmResponse = llmClient.getCompletion(prompt);
            long processingTime = System.currentTimeMillis() - startTime;

            log.debug("Received LLM response: {}", llmResponse);

            // Parse intents
            List<Intent> intents = parseIntents(llmResponse);

            // Calculate overall confidence
            double overallConfidence = calculateAverageConfidence(intents);

            // Publish completed event
            publishAnalysisCompletedEvent(analysisId, command, intents,
                    overallConfidence, processingTime);

            log.info("Completed intent analysis for user input: '{}', found {} intents with confidence {}",
                    command.getUserInput(), intents.size(), overallConfidence);

            return analysisId;
        } catch (Exception e) {
            log.error("Error analyzing intent: {}", e.getMessage(), e);
            if (command.getAnalysisId() != null) {
                publishAnalysisFailedEvent(command.getAnalysisId(), command, e);
            }
            throw new RuntimeException("Failed to analyze intent", e);
        }
    }

    /**
     * Handle a command to extract parameters from identified intents.
     *
     * @param command The extract parameters command
     * @return List of intents with extracted parameters
     */
    public List<IntentWithParameters> handleExtractParameters(ExtractParametersCommand command) {
        try {
            // Validate command
            validator.validateExtractParameters(command);

            // Fetch tools
            List<String> toolNames = command.getIntents().stream()
                    .map(Intent::getIntent)
                    .collect(Collectors.toList());

            List<ToolDTO> tools = toolService.getToolsByNames(toolNames);
            if (tools.isEmpty()) {
                log.warn("No tools found for the specified intents");
                throw new RuntimeException("No tools found for the specified intents");
            }

            // Generate prompt
            String prompt = promptService.createParameterExtractionPrompt(
                    command.getUserInput(), command.getIntents(), tools);

            log.debug("Generated parameter extraction prompt: {}", prompt);

            // Get LLM completion
            String llmResponse = llmClient.getCompletion(prompt);

            log.debug("Received LLM response: {}", llmResponse);

            // Parse intents with parameters
            List<IntentWithParameters> intentsWithParams = parseIntentsWithParameters(llmResponse);

            // Calculate overall confidence
            double overallConfidence = calculateAverageConfidence(intentsWithParams);

            // Publish parameters extracted event
            publishParametersExtractedEvent(command.getAnalysisId(),
                    command.getUserInput(), intentsWithParams, overallConfidence);

            log.info("Completed parameter extraction for analysis ID: {}, found parameters for {} intents",
                    command.getAnalysisId(), intentsWithParams.size());

            return intentsWithParams;
        } catch (Exception e) {
            log.error("Error extracting parameters: {}", e.getMessage(), e);
            publishParameterExtractionFailedEvent(command.getAnalysisId(), command, e);
            throw new RuntimeException("Failed to extract parameters", e);
        }
    }

    /**
     * Parse intents from LLM response.
     *
     * @param llmResponse The LLM response text
     * @return List of parsed intents
     */
    private List<Intent> parseIntents(String llmResponse) {
        try {
            String jsonArray = jsonUtil.extractJsonArray(llmResponse);
            return jsonUtil.fromJsonList(jsonArray, Intent.class);
        } catch (Exception e) {
            log.error("Error parsing intents from LLM response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse intents", e);
        }
    }

    /**
     * Parse intents with parameters from LLM response.
     *
     * @param llmResponse The LLM response text
     * @return List of parsed intents with parameters
     */
    private List<IntentWithParameters> parseIntentsWithParameters(String llmResponse) {
        try {
            String jsonArray = jsonUtil.extractJsonArray(llmResponse);
            return jsonUtil.fromJsonList(jsonArray, IntentWithParameters.class);
        } catch (Exception e) {
            log.error("Error parsing intents with parameters from LLM response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse intents with parameters", e);
        }
    }

    /**
     * Calculate average confidence across all intents.
     *
     * @param intents List of intents
     * @return Average confidence score
     */
    private double calculateAverageConfidence(List<? extends Intent> intents) {
        if (intents.isEmpty()) {
            return 0.0;
        }

        return intents.stream()
                .mapToDouble(Intent::getConfidence)
                .average()
                .orElse(0.0);
    }

    /**
     * Publish an event when intent analysis starts.
     *
     * @param analysisId ID of the analysis
     * @param command The original command
     */
    private void publishAnalysisStartedEvent(String analysisId, AnalyzeIntentCommand command) {
        IntentAnalysisStartedEvent event = new IntentAnalysisStartedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("INTENT_ANALYSIS_STARTED");
        event.setAnalysisId(analysisId);
        event.setTimestamp(LocalDateTime.now());
        event.setUserInput(command.getUserInput());
        event.setSessionId(command.getSessionId());
        event.setToolIds(command.getToolIds());
        event.setLanguage(command.getLanguage());
        event.setMetadata(command.getMetadata());

        eventPublisher.publish("intent-analysis-events", event);
        log.debug("Published IntentAnalysisStartedEvent: {}", event.getEventId());
    }

    /**
     * Publish an event when intent analysis is completed.
     *
     * @param analysisId ID of the analysis
     * @param command The original command
     * @param intents List of identified intents
     * @param confidence Overall confidence score
     * @param processingTimeMs Processing time in milliseconds
     */
    private void publishAnalysisCompletedEvent(String analysisId, AnalyzeIntentCommand command,
                                               List<Intent> intents, double confidence, long processingTimeMs) {

        IntentAnalysisCompletedEvent event = new IntentAnalysisCompletedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("INTENT_ANALYSIS_COMPLETED");
        event.setAnalysisId(analysisId);
        event.setTimestamp(LocalDateTime.now());
        event.setUserInput(command.getUserInput());
        event.setSessionId(command.getSessionId());
        event.setIntents(intents);
        event.setConfidence(confidence);
        event.setProcessingTimeMs(processingTimeMs);
        event.setMetadata(command.getMetadata());

        eventPublisher.publish("intent-analysis-events", event);
        log.debug("Published IntentAnalysisCompletedEvent: {}", event.getEventId());
    }

    /**
     * Publish an event when parameters have been extracted.
     *
     * @param analysisId ID of the analysis
     * @param userInput The original user input
     * @param intents List of intents with extracted parameters
     * @param confidence Overall confidence score
     */
    private void publishParametersExtractedEvent(String analysisId, String userInput,
                                                 List<IntentWithParameters> intents, double confidence) {

        ParametersExtractedEvent event = new ParametersExtractedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PARAMETERS_EXTRACTED");
        event.setAnalysisId(analysisId);
        event.setTimestamp(LocalDateTime.now());
        // In a real implementation, session ID would be carried through the process
        event.setSessionId(extractSessionId(intents, analysisId));
        event.setIntents(intents);
        event.setConfidence(confidence);
        event.setMultiIntent(intents.size() > 1);
        event.setStatus(1); // Active status

        eventPublisher.publish("intent-analysis-events", event);
        log.debug("Published ParametersExtractedEvent: {}", event.getEventId());
    }

    /**
     * Publish an event when intent analysis fails.
     *
     * @param analysisId ID of the analysis
     * @param command The original command
     * @param e The exception that caused the failure
     */
    private void publishAnalysisFailedEvent(String analysisId, AnalyzeIntentCommand command, Exception e) {
        IntentAnalysisFailedEvent event = new IntentAnalysisFailedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("INTENT_ANALYSIS_FAILED");
        event.setAnalysisId(analysisId);
        event.setTimestamp(LocalDateTime.now());
        event.setUserInput(command.getUserInput());
        event.setSessionId(command.getSessionId());
        event.setErrorMessage(e.getMessage());
        event.setErrorType(e.getClass().getSimpleName());
        event.setFailedStep("INTENT_ANALYSIS");
        event.setMetadata(command.getMetadata());

        eventPublisher.publish("intent-analysis-events", event);
        log.debug("Published IntentAnalysisFailedEvent: {}", event.getEventId());
    }

    /**
     * Publish an event when parameter extraction fails.
     *
     * @param analysisId ID of the analysis
     * @param command The original command
     * @param e The exception that caused the failure
     */
    private void publishParameterExtractionFailedEvent(String analysisId,
                                                       ExtractParametersCommand command, Exception e) {

        IntentAnalysisFailedEvent event = new IntentAnalysisFailedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("INTENT_ANALYSIS_FAILED");
        event.setAnalysisId(analysisId);
        event.setTimestamp(LocalDateTime.now());
        event.setUserInput(command.getUserInput());
        // In a real implementation, session ID would be carried through the process
        event.setSessionId("");
        event.setErrorMessage(e.getMessage());
        event.setErrorType(e.getClass().getSimpleName());
        event.setFailedStep("PARAMETER_EXTRACTION");
        event.setMetadata(command.getMetadata());

        eventPublisher.publish("intent-analysis-events", event);
        log.debug("Published IntentAnalysisFailedEvent for parameter extraction: {}", event.getEventId());
    }

    /**
     * Extract session ID from intents or use a default.
     * In a real implementation, this would be properly tracked through the process.
     *
     * @param intents List of intents with parameters
     * @param defaultId Default ID to use if not found
     * @return The session ID
     */
    private String extractSessionId(List<IntentWithParameters> intents, String defaultId) {
        // This is a simplified implementation
        // In a real system, the session ID would be carried through the process
        return defaultId + "-session";
    }
}