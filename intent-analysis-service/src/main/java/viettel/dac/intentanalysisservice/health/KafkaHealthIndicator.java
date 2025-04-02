package viettel.dac.intentanalysisservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Health indicator for Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // Check Kafka health by describing the cluster
            DescribeClusterResult clusterResult = adminClient.describeCluster();

            // Wait for the cluster information with a timeout
            int nodeCount = clusterResult.nodes().get(10, TimeUnit.SECONDS).size();
            String clusterId = clusterResult.clusterId().get(10, TimeUnit.SECONDS);

            if (nodeCount > 0 && clusterId != null) {
                return Health.up()
                        .withDetail("status", "available")
                        .withDetail("nodes", nodeCount)
                        .withDetail("clusterId", clusterId)
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "unavailable")
                        .withDetail("nodes", nodeCount)
                        .build();
            }
        } catch (Exception e) {
            log.error("Kafka health check failed: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("status", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}