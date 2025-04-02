package viettel.dac.intentanalysisservice.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.repository.PromptCacheRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced handler for LLM fallback scenarios with adaptive degradation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LLMFallbackHandler {

    private final PromptCacheRepository promptCacheRepository;

    // Track recent failures for adaptive degradation
    private final Map<String, AtomicInteger> recentFailuresPerType = new ConcurrentHashMap<>();
    private final AtomicInteger totalFailures = new AtomicInteger(0);
    private volatile LocalDateTime degradationStartTime = null;

    // Constants for degradation levels
    private static final int LIGHT_DEGRADATION_THRESHOLD = 5;
    private static final int MODERATE_DEGRADATION_THRESHOLD = 15;
    private static final int SEVERE_DEGRADATION_THRESHOLD = 30;

    /**
     * Handle fallback for a failed LLM request with adaptive degradation.
     *
     * @param prompt The original prompt
     * @param e The exception that caused the fallback
     * @return A fallback response
     */
    public String handleFallback(String prompt, Exception e) {
        // Track failure for degradation management
        trackFailure(e.getClass().getSimpleName());

        // Determine degradation level
        int degradationLevel = getDegradationLevel();

        log.info("Attempting fallback for LLM request with degradation level {}", degradationLevel);

        // First try exact match from cache regardless of degradation level
        String cachedResponse = promptCacheRepository.getCachedResponse(prompt);
        if (cachedResponse != null) {
            log.info("Using exact match cached response for fallback");
            return cachedResponse;
        }

        // For light degradation, try similarity search
        if (degradationLevel < MODERATE_DEGRADATION_THRESHOLD) {
            String similarResponse = promptCacheRepository.findSimilarResponse(prompt);
            if (similarResponse != null) {
                log.info("Using similar cached response for fallback");
                return similarResponse;
            }
        }

        // For all degradation levels, provide appropriate fallback response
        return createDegradedResponse(prompt, degradationLevel);
    }

    /**
     * Create a response for when the system is overloaded.
     *
     * @return Overloaded system response
     */
    public String createOverloadedResponse() {
        // Track this as a failure too
        trackFailure("BULKHEAD_REJECTION");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "overloaded");
        response.put("message", "The service is currently handling too many requests. Please try again later.");
        response.put("timestamp", LocalDateTime.now().toString());

        return toJsonString(response);
    }

    /**
     * Track a failure for adaptive degradation.
     *
     * @param failureType Type of failure
     */
    private void trackFailure(String failureType) {
        // Increment type-specific counter
        recentFailuresPerType.computeIfAbsent(failureType, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Increment total counter
        int total = totalFailures.incrementAndGet();

        // Mark degradation start time if this is the first failure
        if (degradationStartTime == null && total >= LIGHT_DEGRADATION_THRESHOLD) {
            degradationStartTime = LocalDateTime.now();
        }

        // Log degradation level changes
        if (total == LIGHT_DEGRADATION_THRESHOLD) {
            log.warn("Entering light degradation mode due to {} LLM failures", total);
        } else if (total == MODERATE_DEGRADATION_THRESHOLD) {
            log.warn("Entering moderate degradation mode due to {} LLM failures", total);
        } else if (total == SEVERE_DEGRADATION_THRESHOLD) {
            log.warn("Entering severe degradation mode due to {} LLM failures", total);
        }

        // Reset failures after recovery period if degradation has been active
        if (degradationStartTime != null &&
                LocalDateTime.now().isAfter(degradationStartTime.plusMinutes(10))) {
            log.info("Resetting degradation levels after recovery period");
            recentFailuresPerType.clear();
            totalFailures.set(0);
            degradationStartTime = null;
        }
    }

    /**
     * Get current degradation level.
     *
     * @return Degradation level (0-100)
     */
    private int getDegradationLevel() {
        return totalFailures.get();
    }

    /**
     * Create a response for degraded operation.
     *
     * @param prompt The original prompt
     * @param degradationLevel Current degradation level
     * @return Appropriate degraded response
     */
    private String createDegradedResponse(String prompt, int degradationLevel) {
        // For intent analysis, return a safe default response
        if (prompt.contains("intent") || prompt.contains("analyze")) {
            return createDegradedIntentResponse(degradationLevel);
        }
        // For parameter extraction, return empty parameters
        else if (prompt.contains("parameter") || prompt.contains("extract")) {
            return createDegradedParameterResponse(degradationLevel);
        }
        // Generic fallback
        else {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Service degraded");
            response.put("message", "Unable to process request at this time due to system degradation.");
            response.put("degradationLevel", degradationLevel);

            return toJsonString(response);
        }
    }

    /**
     * Create a degraded intent analysis response.
     *
     * @param degradationLevel Current degradation level
     * @return Intent analysis response
     */
    private String createDegradedIntentResponse(int degradationLevel) {
        List<Map<String, Object>> intents = new ArrayList<>();

        if (degradationLevel < MODERATE_DEGRADATION_THRESHOLD) {
            // Light degradation: return default_action with appropriate confidence
            intents.add(Map.of(
                    "intent", "default_action",
                    "confidence", 0.9
            ));
        } else if (degradationLevel < SEVERE_DEGRADATION_THRESHOLD) {
            // Moderate degradation: return generic intents
            intents.add(Map.of(
                    "intent", "default_action",
                    "confidence", 0.7
            ));
            intents.add(Map.of(
                    "intent", "help",
                    "confidence", 0.3
            ));
        } else {
            // Severe degradation: just one intent with low confidence
            intents.add(Map.of(
                    "intent", "service_degraded",
                    "confidence", 0.5
            ));
        }

        return toJsonString(intents);
    }

    /**
     * Create a degraded parameter extraction response.
     *
     * @param degradationLevel Current degradation level
     * @return Parameter extraction response
     */
    private String createDegradedParameterResponse(int degradationLevel) {
        List<Map<String, Object>> intentsWithParams = new ArrayList<>();

        if (degradationLevel < SEVERE_DEGRADATION_THRESHOLD) {
            // Light/moderate degradation: return empty parameters
            intentsWithParams.add(Map.of(
                    "intent", "default_action",
                    "parameters", new HashMap<>(),
                    "confidence", 0.7,
                    "state", 0
            ));
        } else {
            // Severe degradation: indicate service issues
            intentsWithParams.add(Map.of(
                    "intent", "service_degraded",
                    "parameters", Map.of("error", "Service temporarily degraded"),
                    "confidence", 0.5,
                    "state", 0
            ));
        }

        return toJsonString(intentsWithParams);
    }

    /**
     * Convert object to JSON string.
     *
     * @param obj Object to convert
     * @return JSON string
     */
    private String toJsonString(Object obj) {
        try {
            // Basic JSON string creation
            if (obj instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                List<?> list = (List<?>) obj;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(mapToJsonString((Map<?, ?>) list.get(i)));
                }
                sb.append("]");
                return sb.toString();
            } else if (obj instanceof Map) {
                return mapToJsonString((Map<?, ?>) obj);
            }
            return "{}";
        } catch (Exception e) {
            log.error("Error creating JSON string: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Convert map to JSON string.
     *
     * @param map Map to convert
     * @return JSON string
     */
    private String mapToJsonString(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(mapToJsonString((Map<?, ?>) value));
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(value).append("\"");
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}