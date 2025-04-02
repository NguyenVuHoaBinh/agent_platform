package viettel.dac.intentanalysisservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import viettel.dac.intentanalysisservice.service.IntentAnalysisHealthService;

/**
 * Configuration for metrics and monitoring.
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry registry;
    private final IntentAnalysisHealthService healthService;

    /**
     * Initialize JVM and system metrics.
     */
    @Bean
    public void initializeMetrics() {
        // JVM metrics
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);

        // System metrics
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
    }

    /**
     * Counter for LLM API calls.
     */
    @Bean
    public Counter llmApiCallCounter() {
        return Counter.builder("llm.api.calls")
                .description("Count of LLM API calls")
                .register(registry);
    }

    /**
     * Counter for successful LLM API calls.
     */
    @Bean
    public Counter llmApiSuccessCounter() {
        return Counter.builder("llm.api.success")
                .description("Count of successful LLM API calls")
                .register(registry);
    }

    /**
     * Counter for failed LLM API calls.
     */
    @Bean
    public Counter llmApiFailureCounter() {
        return Counter.builder("llm.api.failures")
                .description("Count of failed LLM API calls")
                .register(registry);
    }

    /**
     * Counter for cache hits.
     */
    @Bean
    public Counter cacheHitCounter() {
        return Counter.builder("cache.hits")
                .description("Count of cache hits")
                .register(registry);
    }

    /**
     * Counter for cache misses.
     */
    @Bean
    public Counter cacheMissCounter() {
        return Counter.builder("cache.misses")
                .description("Count of cache misses")
                .register(registry);
    }

    /**
     * Timer for LLM API response time.
     */
    @Bean
    public Timer llmApiResponseTimer() {
        return Timer.builder("llm.api.response.time")
                .description("LLM API response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    /**
     * Counter for intent analysis requests.
     */
    @Bean
    public Counter intentAnalysisRequestCounter() {
        return Counter.builder("intent.analysis.requests")
                .description("Count of intent analysis requests")
                .register(registry);
    }

    /**
     * Counter for successful intent analysis requests.
     */
    @Bean
    public Counter intentAnalysisSuccessCounter() {
        return Counter.builder("intent.analysis.success")
                .description("Count of successful intent analysis requests")
                .register(registry);
    }

    /**
     * Counter for failed intent analysis requests.
     */
    @Bean
    public Counter intentAnalysisFailureCounter() {
        return Counter.builder("intent.analysis.failures")
                .description("Count of failed intent analysis requests")
                .register(registry);
    }

    /**
     * Timer for intent analysis processing time.
     */
    @Bean
    public Timer intentAnalysisProcessingTimer() {
        return Timer.builder("intent.analysis.processing.time")
                .description("Intent analysis processing time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    /**
     * Counter for parameter extraction requests.
     */
    @Bean
    public Counter parameterExtractionRequestCounter() {
        return Counter.builder("parameter.extraction.requests")
                .description("Count of parameter extraction requests")
                .register(registry);
    }

    /**
     * Gauge for average confidence of intent analysis.
     */
    @Bean
    public Gauge averageConfidenceGauge() {
        return Gauge.builder("intent.analysis.confidence.avg",
                        healthService::getAverageConfidence)
                .description("Average confidence of intent analysis")
                .register(registry);
    }

    /**
     * Gauge for queue size.
     */
    @Bean
    public Gauge queueSizeGauge() {
        return Gauge.builder("intent.analysis.queue.size",
                        healthService::getQueueSize)
                .description("Current queue size of intent analysis requests")
                .register(registry);
    }

    /**
     * Gauge for circuit breaker state.
     */
    @Bean
    public Gauge circuitBreakerStateGauge() {
        return Gauge.builder("intent.analysis.circuit.breaker.state",
                        healthService::getCircuitBreakerState)
                .description("Circuit breaker state (0=closed, 1=open, 2=half-open)")
                .register(registry);
    }
}