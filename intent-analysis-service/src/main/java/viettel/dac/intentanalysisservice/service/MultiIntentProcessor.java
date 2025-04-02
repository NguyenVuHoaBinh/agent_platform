package viettel.dac.intentanalysisservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Processor for handling multiple intents and prioritizing them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MultiIntentProcessor {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Process multiple intents, filter and prioritize them.
     *
     * @param intents List of intents to process
     * @param userInput The original user input
     * @param handler Handler to extract parameters
     * @param analysisId The analysis ID
     * @return List of intents with parameters, prioritized and filtered
     */
    public List<IntentWithParameters> processMultipleIntents(
            List<Intent> intents,
            String userInput,
            IntentParameterExtractor handler,
            String analysisId) {

        if (intents == null || intents.isEmpty()) {
            log.debug("No intents to process");
            return Collections.emptyList();
        }

        log.debug("Processing {} intents for user input: '{}'", intents.size(), userInput);

        // Sort intents by confidence (descending)
        List<Intent> sortedIntents = intents.stream()
                .sorted(Comparator.comparing(Intent::getConfidence).reversed())
                .collect(Collectors.toList());

        // Apply threshold filtering
        List<Intent> filteredIntents = applyConfidenceThreshold(sortedIntents);

        if (filteredIntents.isEmpty()) {
            log.debug("No intents passed confidence threshold");
            return Collections.emptyList();
        }

        // Extract parameters in parallel
        List<IntentWithParameters> result = extractParametersInParallel(
                filteredIntents, userInput, handler, analysisId);

        // Apply parameter-based ranking adjustments
        List<IntentWithParameters> rankedIntents = rankIntentsByParameterQuality(result);

        // Limit the number of returned intents
        return limitIntentCount(rankedIntents);
    }

    /**
     * Apply confidence threshold filtering to intents.
     *
     * @param sortedIntents List of intents sorted by confidence (descending)
     * @return Filtered list of intents
     */
    private List<Intent> applyConfidenceThreshold(List<Intent> sortedIntents) {
        if (sortedIntents.isEmpty()) {
            return Collections.emptyList();
        }

        // Get primary intent confidence and calculate dynamic threshold
        double primaryIntentConfidence = sortedIntents.get(0).getConfidence();

        // Dynamic threshold: 70% of primary intent confidence or minimum 0.4, whichever is higher
        double dynamicThreshold = Math.max(0.4, primaryIntentConfidence * 0.7);

        // Absolute minimum threshold
        double absoluteMinThreshold = 0.3;

        // Apply the thresholds
        return sortedIntents.stream()
                .filter(intent -> {
                    // Primary intent always included regardless of thresholds
                    if (intent == sortedIntents.get(0)) {
                        return true;
                    }

                    // Apply dynamic and absolute thresholds
                    return intent.getConfidence() >= dynamicThreshold &&
                            intent.getConfidence() >= absoluteMinThreshold;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract parameters for each intent in parallel.
     *
     * @param intents List of intents
     * @param userInput The original user input
     * @param handler Handler to extract parameters
     * @param analysisId The analysis ID
     * @return List of intents with parameters
     */
    private List<IntentWithParameters> extractParametersInParallel(
            List<Intent> intents,
            String userInput,
            IntentParameterExtractor handler,
            String analysisId) {

        // Create futures for parameter extraction
        List<CompletableFuture<IntentWithParameters>> futures = intents.stream()
                .map(intent -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Create command for single intent
                        ExtractParametersCommand command = ExtractParametersCommand.builder()
                                .analysisId(analysisId)
                                .userInput(userInput)
                                .intents(Collections.singletonList(intent))
                                .build();

                        // Extract parameters
                        List<IntentWithParameters> result = handler.extractParameters(command);

                        // Return result or create empty intent
                        return result.isEmpty() ?
                                createEmptyIntentWithParameters(intent) :
                                result.get(0);
                    } catch (Exception e) {
                        log.error("Error extracting parameters for intent {}: {}",
                                intent.getIntent(), e.getMessage(), e);
                        return createEmptyIntentWithParameters(intent);
                    }
                }, executorService))
                .collect(Collectors.toList());

        // Wait for all futures to complete and collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Rank intents based on parameter quality.
     *
     * @param intents List of intents with parameters
     * @return Ranked list of intents
     */
    private List<IntentWithParameters> rankIntentsByParameterQuality(List<IntentWithParameters> intents) {
        // If single intent or empty, return as-is
        if (intents.size() <= 1) {
            return intents;
        }

        // Calculate parameter completeness score for each intent
        Map<IntentWithParameters, Double> parameterScores = new HashMap<>();

        for (IntentWithParameters intent : intents) {
            Map<String, Object> params = intent.getParameters();
            if (params == null || params.isEmpty()) {
                parameterScores.put(intent, 0.0);
                continue;
            }

            // Count non-null parameters
            long nonNullParamCount = params.values().stream()
                    .filter(Objects::nonNull)
                    .count();

            // Calculate completeness score
            double score = (double) nonNullParamCount / params.size();
            parameterScores.put(intent, score);
        }

        // Sort by confidence and parameter quality
        return intents.stream()
                .sorted((a, b) -> {
                    double aScore = a.getConfidence() * (0.7 + 0.3 * parameterScores.get(a));
                    double bScore = b.getConfidence() * (0.7 + 0.3 * parameterScores.get(b));
                    return Double.compare(bScore, aScore); // Descending order
                })
                .collect(Collectors.toList());
    }

    /**
     * Limit the number of returned intents to a reasonable maximum.
     *
     * @param intents List of intents
     * @return Limited list of intents
     */
    private List<IntentWithParameters> limitIntentCount(List<IntentWithParameters> intents) {
        final int MAX_INTENTS = 3; // Maximum number of intents to return

        if (intents.size() <= MAX_INTENTS) {
            return intents;
        }

        return intents.subList(0, MAX_INTENTS);
    }

    /**
     * Create an empty intent with parameters object.
     *
     * @param intent The original intent
     * @return Intent with empty parameters
     */
    private IntentWithParameters createEmptyIntentWithParameters(Intent intent) {
        IntentWithParameters result = new IntentWithParameters();
        result.setIntent(intent.getIntent());
        result.setConfidence(intent.getConfidence());
        result.setParameters(new HashMap<>());
        result.setState(0);
        return result;
    }

    /**
     * Interface for parameter extraction.
     */
    public interface IntentParameterExtractor {
        List<IntentWithParameters> extractParameters(ExtractParametersCommand command);
    }
}