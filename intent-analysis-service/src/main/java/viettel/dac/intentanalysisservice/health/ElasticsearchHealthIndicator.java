package viettel.dac.intentanalysisservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Health health() {
        try {
            // Get index operations and check if Elasticsearch is responsive
            IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of("_all"));
            boolean exists = indexOps.exists();

            return Health.up()
                    .withDetail("status", "available")
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch health check failed: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("status", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
