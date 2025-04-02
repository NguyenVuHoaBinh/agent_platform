package viettel.dac.intentanalysisservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementation of EventPublisher using Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                        } else {
                            log.debug("Event published to topic {}: {}", topic, event);
                        }
                    });

            log.info("Published event of type {} to topic {}", event.getClass().getSimpleName(), topic);
        } catch (Exception e) {
            log.error("Error serializing event: {}", e.getMessage());
            throw new RuntimeException("Error publishing event", e);
        }
    }
}

