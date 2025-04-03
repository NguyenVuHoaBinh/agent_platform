package viettel.dac.toolserviceregistry.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Kafka Topics.
 */
@Configuration
public class KafkaTopicConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.tool-events}")
    private String toolEventsTopic;

    @Value("${kafka.topic.tool-dependency-events}")
    private String toolDependencyEventsTopic;

    @Value("${kafka.topic.execution-plan-requests}")
    private String executionPlanRequestsTopic;

    @Value("${kafka.topic.execution-plan-responses}")
    private String executionPlanResponsesTopic;

    /**
     * Creates the Kafka admin client.
     *
     * @return The configured Kafka admin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Creates the tool events topic.
     *
     * @return The configured topic
     */
    @Bean
    public NewTopic toolEventsTopic() {
        return TopicBuilder.name(toolEventsTopic)
                .partitions(6)
                .replicas(1)
                .compact()
                .build();
    }

    /**
     * Creates the tool dependency events topic.
     *
     * @return The configured topic
     */
    @Bean
    public NewTopic toolDependencyEventsTopic() {
        return TopicBuilder.name(toolDependencyEventsTopic)
                .partitions(6)
                .replicas(1)
                .compact()
                .build();
    }

    /**
     * Creates the execution plan requests topic.
     *
     * @return The configured topic
     */
    @Bean
    public NewTopic executionPlanRequestsTopic() {
        return TopicBuilder.name(executionPlanRequestsTopic)
                .partitions(12)
                .replicas(1)
                .build();
    }

    /**
     * Creates the execution plan responses topic.
     *
     * @return The configured topic
     */
    @Bean
    public NewTopic executionPlanResponsesTopic() {
        return TopicBuilder.name(executionPlanResponsesTopic)
                .partitions(12)
                .replicas(1)
                .build();
    }

    /**
     * Creates the dead letter queue topic for tool events.
     *
     * @return The configured topic
     */
    @Bean
    public NewTopic deadLetterQueueTopic() {
        return TopicBuilder.name(toolEventsTopic + "-dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }
}