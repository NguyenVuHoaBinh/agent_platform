package viettel.dac.intentanalysisservice.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.repository.PromptCacheRepository;

/**
 * Handler for LLM fallback scenarios when the primary LLM service is unavailable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LLMFallbackHandler {

    private final PromptCacheRepository promptCacheRepository;

    /**
     * Handle fallback for a failed LLM request.
     *
     * @param prompt The original prompt
     * @param e The exception that caused the fallback
     * @return A fallback response
     */
    public String handleFallback(String prompt, Exception e) {
        log.info("Attempting to retrieve cached response for similar prompt");

        // First try exact match
        String cachedResponse = promptCacheRepository.getCachedResponse(prompt);
        if (cachedResponse != null) {
            log.info("Using exact match cached response");
            return cachedResponse;
        }

        // Try semantic similarity (this would use a simplified approach or
        // an external service in a real implementation)
        cachedResponse = promptCacheRepository.findSimilarResponse(prompt);
        if (cachedResponse != null) {
            log.info("Using similar cached response");
            return cachedResponse;
        }

        // Default fallback response
        log.warn("No suitable cached response found, using default fallback");
        return createDefaultFallbackResponse(prompt);
    }

    /**
     * Create a default fallback response based on the prompt content.
     *
     * @param prompt The original prompt
     * @return A default fallback response
     */
    private String createDefaultFallbackResponse(String prompt) {
        // For intent analysis, return a safe default response
        if (prompt.contains("intent")) {
            return "[{\"intent\": \"default_action\", \"confidence\": 1.0}]";
        }
        // For parameter extraction, return empty parameters
        else if (prompt.contains("parameter")) {
            return "[{\"intent\": \"default_action\", \"parameters\": {}, \"confidence\": 1.0, \"state\": 0}]";
        }
        // Generic fallback
        else {
            return "{\"error\": \"Unable to process request at this time.\"}";
        }
    }
}
