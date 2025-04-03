package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.EventPublishingException;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events to Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes an event to Kafka.
     *
     * @param topic The topic to publish to
     * @param key The key for the message
     * @param event The event to publish
     */
    public void publish(String topic, String key, Object event) {
        try {
            log.debug("Publishing event to topic: {}, key: {}, event type: {}",
                    topic, key, event.getClass().getSimpleName());

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // Success case
                    log.debug("Published event successfully to topic: {}, partition: {}, offset: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    // Failure case
                    log.error("Failed to publish event to topic: {}", topic, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing event", e);
            throw new EventPublishingException("Failed to publish event", e);
        }
    }
}
