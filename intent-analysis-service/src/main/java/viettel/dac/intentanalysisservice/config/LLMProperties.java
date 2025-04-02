package viettel.dac.intentanalysisservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the LLM service.
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMProperties {
    /**
     * LLM API endpoint URL.
     */
    private String apiUrl;

    /**
     * API key for authentication.
     */
    private String apiKey;

    /**
     * Model identifier to use.
     */
    private String model;

    /**
     * System prompt to use in requests.
     */
    private String systemPrompt;

    /**
     * Temperature setting (controls randomness).
     */
    private double temperature = 0.3;

    /**
     * Maximum number of tokens in the response.
     */
    private int maxTokens = 1024;

    /**
     * Maximum number of retry attempts.
     */
    private int retryMaxAttempts = 3;

    /**
     * Backoff delay in milliseconds between retries.
     */
    private int retryBackoffMs = 1000;
}
