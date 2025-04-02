package viettel.dac.intentanalysisservice.llm;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM service client.
 */
public interface LLMClient {

    /**
     * Get a completion from the LLM for the given prompt.
     *
     * @param prompt The prompt to send to the LLM
     * @return The LLM's response
     */
    String getCompletion(String prompt);

    /**
     * Get a completion from the LLM asynchronously.
     *
     * @param prompt The prompt to send to the LLM
     * @return CompletableFuture with the LLM's response
     */
    CompletableFuture<String> getCompletionAsync(String prompt);
}
