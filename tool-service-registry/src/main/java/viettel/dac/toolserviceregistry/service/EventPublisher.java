package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.EventPublishingException;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.enums.ToolEventType;
import viettel.dac.toolserviceregistry.model.event.ToolEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced service for publishing tool events to Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ApiToolService apiToolService;

    @Value("${kafka.topic.tool-events}")
    private String toolEventsTopic;

    /**
     * Publishes a tool event to Kafka.
     *
     * @param tool The tool entity
     * @param eventType The type of event
     * @return CompletableFuture that completes when the event is published
     */
    public CompletableFuture<Void> publishToolEvent(Tool tool, ToolEventType eventType) {
        ToolEvent event = createToolEvent(tool, eventType);
        return publishEvent(toolEventsTopic, tool.getId(), event);
    }

    /**
     * Creates a tool event from a tool and event type.
     *
     * @param tool The tool entity
     * @param eventType The type of event
     * @return The created tool event
     */
    private ToolEvent createToolEvent(Tool tool, ToolEventType eventType) {
        ToolEvent event = new ToolEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType.name());
        event.setTimestamp(LocalDateTime.now());
        event.setToolId(tool.getId());
        event.setName(tool.getName());
        event.setDescription(tool.getDescription());
        event.setActive(tool.isActive());
        event.setVersion(tool.getVersion());
        event.setToolType(tool.getToolType());

        // Add API metadata if this is an API tool
        ApiToolMetadataDTO apiMetadata = apiToolService.getApiToolMetadataDTO(tool.getId());
        if (apiMetadata != null) {
            event.setApiMetadata(apiMetadata);
        }

        return event;
    }

    /**
     * Publishes an event to a Kafka topic.
     *
     * @param topic The topic to publish to
     * @param key The key for the message
     * @param event The event to publish
     * @return CompletableFuture that completes when the event is published
     */
    public CompletableFuture<Void> publishEvent(String topic, String key, Object event) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            log.debug("Publishing event to topic: {}, key: {}, event type: {}",
                    topic, key, event.getClass().getSimpleName());

            // Metrics for event publishing
            meterRegistry.counter("events.published",
                    "topic", topic,
                    "eventType", event.getClass().getSimpleName()).increment();

            // For structured logging
            Map<String, Object> logData = new HashMap<>();
            logData.put("topic", topic);
            logData.put("key", key);
            logData.put("eventType", event.getClass().getSimpleName());
            logData.put("timestamp", LocalDateTime.now().toString());

            String jsonLog = objectMapper.writeValueAsString(logData);
            log.info("Publishing Kafka event: {}", jsonLog);

            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            // Success case
                            log.debug("Published event successfully to topic: {}, partition: {}, offset: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            future.complete(null);
                        } else {
                            // Failure case
                            log.error("Failed to publish event to topic: {}", topic, ex);
                            meterRegistry.counter("events.publish.errors",
                                    "topic", topic,
                                    "error", ex.getClass().getSimpleName()).increment();
                            future.completeExceptionally(ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event", e);
            meterRegistry.counter("events.publish.errors",
                    "topic", topic,
                    "error", e.getClass().getSimpleName()).increment();
            future.completeExceptionally(new EventPublishingException("Failed to publish event", e));
        }

        return future;
    }
}