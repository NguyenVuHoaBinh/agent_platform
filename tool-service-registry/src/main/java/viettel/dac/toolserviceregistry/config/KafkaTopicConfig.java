package viettel.dac.toolserviceregistry.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Configuration for Kafka topics and error handling.
 */
@Configuration
@EnableKafka
public class KafkaTopicConfig {

    @Value("${kafka.topic.tool-events}")
    private String toolEventsTopic;

    @Value("${kafka.topic.intent-analysis-events}")
    private String intentAnalysisEventsTopic;

    @Value("${kafka.topic.tool-registry-requests}")
    private String toolRegistryRequestsTopic;

    @Value("${kafka.topic.tool-registry-responses}")
    private String toolRegistryResponsesTopic;

    /**
     * Creates the tool events topic.
     */
    @Bean
    public NewTopic toolEventsTopic() {
        return TopicBuilder.name(toolEventsTopic)
                .partitions(6)
                .replicas(2)
                .compact() // Enable log compaction for events
                .build();
    }

    /**
     * Creates the intent analysis events topic.
     */
    @Bean
    public NewTopic intentAnalysisEventsTopic() {
        return TopicBuilder.name(intentAnalysisEventsTopic)
                .partitions(6)
                .replicas(2)
                .build();
    }

    /**
     * Creates the tool registry requests topic.
     */
    @Bean
    public NewTopic toolRegistryRequestsTopic() {
        return TopicBuilder.name(toolRegistryRequestsTopic)
                .partitions(4)
                .replicas(2)
                .build();
    }

    /**
     * Creates the tool registry responses topic.
     */
    @Bean
    public NewTopic toolRegistryResponsesTopic() {
        return TopicBuilder.name(toolRegistryResponsesTopic)
                .partitions(4)
                .replicas(2)
                .build();
    }

    /**
     * Creates the dead letter queue topic for tool events.
     */
    @Bean
    public NewTopic toolEventsDlqTopic() {
        return TopicBuilder.name(toolEventsTopic + "-dlq")
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * Creates the dead letter queue topic for intent analysis events.
     */
    @Bean
    public NewTopic intentAnalysisEventsDlqTopic() {
        return TopicBuilder.name(intentAnalysisEventsTopic + "-dlq")
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * Creates the dead letter queue topic for tool registry requests.
     */
    @Bean
    public NewTopic toolRegistryRequestsDlqTopic() {
        return TopicBuilder.name(toolRegistryRequestsTopic + "-dlq")
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * Creates error handler for Kafka consumers with DLQ support.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Define a recovery callback that sends failed messages to a DLQ
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "-dlq", record.partition()));

        // Define retry parameters
        ExponentialBackOff backOff = new ExponentialBackOff(1000, 2.0);
        backOff.setMaxInterval(30000);
        backOff.setMaxAttempts(10);

        // Create error handler with custom backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Define which exceptions should not be retried
        errorHandler.addNotRetryableExceptions(
                JsonParseException.class,
                InvalidFormatException.class
        );

        return errorHandler;
    }

}