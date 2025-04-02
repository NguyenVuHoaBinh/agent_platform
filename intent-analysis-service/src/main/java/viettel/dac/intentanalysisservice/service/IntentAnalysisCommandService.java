package viettel.dac.intentanalysisservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.dto.AnalysisStatusResponse;
import viettel.dac.intentanalysisservice.dto.AsyncAnalysisResponse;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;
import viettel.dac.intentanalysisservice.exception.NotFoundException;
import viettel.dac.intentanalysisservice.handler.IntentAnalysisCommandHandler;
import viettel.dac.intentanalysisservice.model.AnalysisStatus;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.repository.PromptCacheRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling intent analysis commands and managing analysis state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisCommandService {

    private final IntentAnalysisCommandHandler commandHandler;
    private final PromptCacheRepository promptCacheRepository;

    // In-memory store for async analysis status (would be moved to Redis/DB in production)
    private final Map<String, AnalysisStatus> analysisStatusMap = new ConcurrentHashMap<>();

    /**
     * Analyze user input for intents.
     *
     * @param command The analyze intent command
     * @return The analysis result
     */
    public IntentAnalysisResponse analyzeIntent(AnalyzeIntentCommand command) {
        log.info("Processing synchronous intent analysis for user input: '{}'", command.getUserInput());

        // Execute intent analysis
        String analysisId = commandHandler.handleAnalyzeIntent(command);

        // Extract parameters from the identified intents
        List<IntentWithParameters> processedIntents = extractParameters(analysisId, command);

        // Calculate overall confidence
        double overallConfidence = calculateAverageConfidence(processedIntents);

        // Create response
        IntentAnalysisResponse response = new IntentAnalysisResponse();
        response.setCode("200");
        response.setAnalysisId(analysisId);
        response.setIntents(processedIntents);
        response.setMultiIntent(processedIntents.size() > 1);
        response.setConfidence(overallConfidence);

        return response;
    }

    /**
     * Start asynchronous intent analysis.
     *
     * @param command The analyze intent command
     * @return Response with analysis ID for checking status
     */
    @Async
    public AsyncAnalysisResponse analyzeIntentAsync(AnalyzeIntentCommand command) {
        log.info("Starting asynchronous intent analysis for user input: '{}'", command.getUserInput());

        // Generate analysis ID
        String analysisId = UUID.randomUUID().toString();
        command.setAnalysisId(analysisId);

        // Create initial status entry
        AnalysisStatus status = new AnalysisStatus();
        status.setStatus("PENDING");
        status.setProgress(0);
        analysisStatusMap.put(analysisId, status);

        // Start async processing
        CompletableFuture.runAsync(() -> {
            try {
                // Update status to processing
                AnalysisStatus processingStatus = analysisStatusMap.get(analysisId);
                processingStatus.setStatus("PROCESSING");
                processingStatus.setProgress(10);

                // Execute intent analysis
                String completedAnalysisId = commandHandler.handleAnalyzeIntent(command);

                // Update progress
                processingStatus.setProgress(50);

                // Extract parameters
                List<IntentWithParameters> processedIntents = extractParameters(completedAnalysisId, command);

                // Calculate overall confidence
                double overallConfidence = calculateAverageConfidence(processedIntents);

                // Create result
                IntentAnalysisResponse result = new IntentAnalysisResponse();
                result.setCode("200");
                result.setAnalysisId(completedAnalysisId);
                result.setIntents(processedIntents);
                result.setMultiIntent(processedIntents.size() > 1);
                result.setConfidence(overallConfidence);

                // Update status to completed with result
                processingStatus.setStatus("COMPLETED");
                processingStatus.setProgress(100);
                processingStatus.setResult(result);

                log.info("Completed asynchronous intent analysis for analysis ID: {}", analysisId);
            } catch (Exception e) {
                log.error("Error in async intent analysis: {}", e.getMessage(), e);

                // Update status to failed
                AnalysisStatus failedStatus = analysisStatusMap.get(analysisId);
                failedStatus.setStatus("FAILED");
                failedStatus.setErrorMessage(e.getMessage());
            }
        });

        // Return immediate response with analysis ID
        AsyncAnalysisResponse response = new AsyncAnalysisResponse();
        response.setAnalysisId(analysisId);
        response.setMessage("Analysis started, check status for results");

        return response;
    }

    /**
     * Get the status of an asynchronous analysis.
     *
     * @param analysisId The analysis ID
     * @return The current status
     */
    public AnalysisStatusResponse getAnalysisStatus(String analysisId) {
        log.debug("Checking status for analysis ID: {}", analysisId);

        AnalysisStatus status = analysisStatusMap.get(analysisId);
        if (status == null) {
            throw new NotFoundException("Analysis not found with ID: " + analysisId);
        }

        AnalysisStatusResponse response = new AnalysisStatusResponse();
        response.setAnalysisId(analysisId);
        response.setStatus(status.getStatus());
        response.setProgress(status.getProgress());
        response.setResult(status.getResult());
        response.setErrorMessage(status.getErrorMessage());

        return response;
    }

    /**
     * Execute the analyze intent command directly.
     *
     * @param command The command to execute
     * @return The analysis ID
     */
    public String executeAnalyzeIntentCommand(AnalyzeIntentCommand command) {
        return commandHandler.handleAnalyzeIntent(command);
    }

    /**
     * Execute the extract parameters command directly.
     *
     * @param command The command to execute
     * @return List of intents with parameters
     */
    public List<IntentWithParameters> executeExtractParametersCommand(ExtractParametersCommand command) {
        return commandHandler.handleExtractParameters(command);
    }

    /**
     * Cancel an analysis (for saga compensation).
     *
     * @param analysisId The analysis ID to cancel
     * @param reason The reason for cancellation
     * @return true if cancelled successfully, false if not found
     */
    public boolean cancelAnalysis(String analysisId, String reason) {
        log.info("Cancelling analysis: {}, reason: {}", analysisId, reason);

        // Find the analysis in our status map
        AnalysisStatus status = analysisStatusMap.get(analysisId);
        if (status == null) {
            log.warn("Analysis not found for cancellation: {}", analysisId);
            return false;
        }

        // Update status to cancelled
        status.setStatus("CANCELLED");
        status.setErrorMessage("Cancelled due to saga compensation: " + reason);

        // In a real implementation, we would also publish an event or update a database record

        log.info("Analysis cancelled: {}", analysisId);
        return true;
    }

    /**
     * Extract parameters for the identified intents.
     *
     * @param analysisId The analysis ID
     * @param command The original analyze intent command
     * @return List of intents with extracted parameters
     */
    private List<IntentWithParameters> extractParameters(String analysisId, AnalyzeIntentCommand command) {
        try {
            // Create extract parameters command
            ExtractParametersCommand extractCommand = new ExtractParametersCommand();
            extractCommand.setAnalysisId(analysisId);
            extractCommand.setUserInput(command.getUserInput());
            extractCommand.setLanguage(command.getLanguage());
            extractCommand.setMetadata(command.getMetadata());

            // Get intents from the cache (in a real implementation, would be retrieved from a store)
            // Here we're making a dummy implementation for demonstration
            List<Intent> intents = List.of(
                    new Intent("search_product", 0.95)
            );
            extractCommand.setIntents(intents);

            // Extract parameters
            return commandHandler.handleExtractParameters(extractCommand);
        } catch (Exception e) {
            log.error("Error extracting parameters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract parameters", e);
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
}