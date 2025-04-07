package viettel.dac.intentanalysisservice.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for metrics related to Kafka and Intent Analysis.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final ProducerFactory<String, String> producerFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    // Cache for event counters
    private final ConcurrentHashMap<String, AtomicInteger> eventCounter = new ConcurrentHashMap<>();

    /**
     * Customizes the meter registry with common tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", applicationName,
                "component", "intent-analysis"
        );
    }


    /**
     * Enables the @Timed annotation.
     */
    @Bean
    public TimedAspect timedAspect() {
        return new TimedAspect(meterRegistry);
    }

    /**
     * Enables the @Counted annotation.
     */
    @Bean
    public CountedAspect countedAspect() {
        return new CountedAspect(meterRegistry);
    }

    /**
     * Binds Kafka producer metrics.
     */
    @Bean
    public KafkaClientMetrics kafkaProducerMetrics() {
        return new KafkaClientMetrics(kafkaTemplate.getProducerFactory().createProducer());
    }

    /**
     * Creates a gauge for Kafka event publishing rate.
     */
    @Bean
    public Gauge kafkaEventPublishingGauge() {
        return Gauge.builder("kafka.events.publishing.rate", () -> {
                    // In a real implementation, this would track the rate over time
                    return eventCounter.values().stream()
                            .mapToInt(AtomicInteger::get)
                            .sum();
                })
                .description("Rate of Kafka event publishing")
                .tags(Collections.singletonList(Tag.of("component", "kafka")))
                .register(meterRegistry);
    }

    /**
     * Creates a gauge for pending request count.
     */
    @Bean
    public Gauge pendingRequestsGauge() {
        // This would be tied to the actual pending requests map in the ToolService
        AtomicInteger pendingRequestsCount = new AtomicInteger(0);

        return Gauge.builder("kafka.pending.requests", pendingRequestsCount::get)
                .description("Number of pending requests to the Tool Registry Service")
                .tags(Collections.singletonList(Tag.of("component", "kafka")))
                .register(meterRegistry);
    }

    /**
     * Increments the event counter for a specific event type.
     */
    public void incrementEventCounter(String eventType) {
        eventCounter.computeIfAbsent(eventType, k -> new AtomicInteger(0))
                .incrementAndGet();
    }
}