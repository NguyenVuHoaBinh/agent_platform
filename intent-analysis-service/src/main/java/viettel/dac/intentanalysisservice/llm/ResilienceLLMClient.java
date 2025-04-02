package viettel.dac.intentanalysisservice.llm;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import viettel.dac.intentanalysisservice.config.LLMProperties;
import viettel.dac.intentanalysisservice.exception.LLMException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LLM client implementation with resilience patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceLLMClient implements LLMClient {

    private final RestTemplate restTemplate;
    private final LLMProperties llmProperties;
    private final LLMFallbackHandler fallbackHandler;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Get a completion from the LLM for the given prompt.
     * This method is protected by circuit breaker, retry, and bulkhead patterns.
     *
     * @param prompt The prompt to send to the LLM
     * @return The LLM's response
     */
    @Override
    @CircuitBreaker(name = "llmClient", fallbackMethod = "getCompletionFallback")
    @Retry(name = "llmClient")
    @Bulkhead(name = "llmClient")
    public String getCompletion(String prompt) {
        try {
            log.debug("Sending request to LLM: {}", prompt);

            Map<String, Object> request = createRequestBody(prompt);
            Map<String, Object> response = restTemplate.postForObject(
                    llmProperties.getApiUrl(),
                    request,
                    Map.class
            );

            return extractCompletionFromResponse(response);
        } catch (Exception e) {
            log.error("Error calling LLM: {}", e.getMessage());
            throw new LLMException("Failed to get completion from LLM", e);
        }
    }

    /**
     * Get a completion from the LLM asynchronously.
     *
     * @param prompt The prompt to send to the LLM
     * @return CompletableFuture with the LLM's response
     */
    @Override
    public CompletableFuture<String> getCompletionAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> getCompletion(prompt), executorService);
    }

    /**
     * Fallback method for circuit breaker.
     *
     * @param prompt The original prompt
     * @param e The exception that triggered the fallback
     * @return A fallback response
     */
    private String getCompletionFallback(String prompt, Exception e) {
        log.warn("Circuit breaker triggered for LLM call: {}", e.getMessage());
        return fallbackHandler.handleFallback(prompt, e);
    }

    /**
     * Create the request body for the LLM API.
     *
     * @param prompt The user prompt
     * @return The request body as a Map
     */
    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", llmProperties.getModel());
        request.put("messages", List.of(
                Map.of("role", "system", "content", llmProperties.getSystemPrompt()),
                Map.of("role", "user", "content", prompt)
        ));
        request.put("temperature", llmProperties.getTemperature());
        request.put("max_tokens", llmProperties.getMaxTokens());
        return request;
    }

    /**
     * Extract the completion from the LLM API response.
     *
     * @param response The API response
     * @return The extracted completion text
     */
    private String extractCompletionFromResponse(Map<String, Object> response) {
        if (response == null) {
            throw new LLMException("Null response from LLM");
        }

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LLMException("No choices in LLM response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new LLMException("No message in LLM response");
            }

            return (String) message.get("content");
        } catch (Exception e) {
            throw new LLMException("Failed to parse LLM response", e);
        }
    }
}
