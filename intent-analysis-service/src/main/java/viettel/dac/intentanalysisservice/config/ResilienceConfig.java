package viettel.dac.intentanalysisservice.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import viettel.dac.intentanalysisservice.exception.LLMException;

import java.time.Duration;

/**
 * Configuration for resilience patterns.
 */
@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

    private final LLMProperties llmProperties;

    /**
     * Circuit breaker configuration for LLM client.
     */
    @Bean
    public CircuitBreakerConfig llmCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)  // Open when 50% of calls fail
                .slowCallRateThreshold(50) // Consider 50% of slow calls as failure
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Call is slow if it takes > 5 seconds
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Stay open for 30 seconds
                .permittedNumberOfCallsInHalfOpenState(10) // Allow 10 calls in half-open state
                .minimumNumberOfCalls(10) // Minimum calls before calculating failure rate
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100) // Consider last 100 calls
                .build();
    }

    /**
     * Retry configuration for LLM client.
     */
    @Bean
    public RetryConfig llmRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(llmProperties.getRetryMaxAttempts())
                .waitDuration(Duration.ofMillis(llmProperties.getRetryBackoffMs()))
                .retryExceptions(
                        LLMException.class,
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class
                )
                .failAfterMaxAttempts(true)
                .build();
    }

    /**
     * Bulkhead configuration for LLM client.
     */
    @Bean
    public BulkheadConfig llmBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(20) // Maximum concurrent calls
                .maxWaitDuration(Duration.ofSeconds(1)) // Max wait time when bulkhead is full
                .build();
    }

    /**
     * Time limiter configuration for LLM client.
     */
    @Bean
    public TimeLimiterConfig llmTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(15)) // Maximum time for call
                .cancelRunningFuture(true)
                .build();
    }
}