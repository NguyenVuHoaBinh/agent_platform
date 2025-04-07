package viettel.dac.intentanalysisservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced implementation of EventPublisher using Kafka with metrics and structured logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // Declare these as fields without autowiring
    private Counter eventCounter;
    private Counter eventErrorCounter;
    private Timer eventPublishTimer;

    @Value("${kafka.topic.intent-analysis-events}")
    private String intentAnalysisEventsTopic;

    /**
     * Initialize counters and timers after bean construction.
     */
    @PostConstruct
    public void initMetrics() {
        // Initialize metrics
        this.eventCounter = Counter.builder("intent.analysis.events.published")
                .description("Count of published intent analysis events")
                .register(meterRegistry);

        this.eventErrorCounter = Counter.builder("intent.analysis.events.errors")
                .description("Count of errors while publishing intent analysis events")
                .register(meterRegistry);

        this.eventPublishTimer = Timer.builder("intent.analysis.events.publish.time")
                .description("Time taken to publish intent analysis events")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        log.info("Initialized Kafka event metrics");
    }

    @Override
    public void publish(String topic, Object event) {
        Timer.Sample sample = Timer.start();

        try {
            // Get key from event for partitioning
            String key = getKeyFromEvent(event);

            // Convert event to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Structured logging
            Map<String, Object> logData = new HashMap<>();
            logData.put("topic", topic);
            logData.put("eventType", event.getClass().getSimpleName());
            logData.put("eventId", getEventId(event));
            logData.put("timestamp", LocalDateTime.now().toString());

            // Log in JSON format for better parsing
            String logJson = objectMapper.writeValueAsString(logData);
            log.info("Publishing event: {}", logJson);

            // Send to Kafka with completion handling
            kafkaTemplate.send(topic, key, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                            eventErrorCounter.increment();
                        } else {
                            log.debug("Event published to topic {} at offset {}",
                                    topic, result.getRecordMetadata().offset());
                            eventCounter.increment();
                        }
                    });

            log.info("Published event of type {} to topic {}", event.getClass().getSimpleName(), topic);

            // Record metrics
            sample.stop(eventPublishTimer);
        } catch (Exception e) {
            log.error("Error serializing event: {}", e.getMessage());
            eventErrorCounter.increment();
            // Record exception timing
            sample.stop(eventPublishTimer);
            throw new RuntimeException("Error publishing event", e);
        }
    }

    /**
     * Extract a key from the event for Kafka partitioning.
     *
     * @param event The event object
     * @return A key for Kafka partitioning
     */
    private String getKeyFromEvent(Object event) {
        try {
            // Try to get an ID field from the event for partitioning
            if (event instanceof IntentAnalysisEvent) {
                return ((IntentAnalysisEvent) event).getAnalysisId();
            } else if (event instanceof Map) {
                Object id = ((Map<?, ?>) event).get("id");
                if (id != null) {
                    return id.toString();
                }
            }
            // Default to random UUID if no ID found
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Extract event ID for logging purposes.
     *
     * @param event The event object
     * @return The event ID as a string
     */
    private String getEventId(Object event) {
        try {
            if (event instanceof IntentAnalysisEvent) {
                return ((IntentAnalysisEvent) event).getEventId();
            } else if (event instanceof Map) {
                Object id = ((Map<?, ?>) event).get("eventId");
                if (id != null) {
                    return id.toString();
                }
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}