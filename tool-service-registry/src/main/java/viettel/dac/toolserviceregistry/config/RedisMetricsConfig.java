package viettel.dac.toolserviceregistry.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Collections;

/**
 * Configuration for Redis metrics.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class RedisMetricsConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Creates Redis metrics binder.
     *
     * @return The Redis metrics binder
     */
    @Bean
    public MeterBinder redisMetrics() {
        return registry -> {
            // Register Redis connection metrics
            try {
                new RedisMetricsBinder(redisConnectionFactory).bindTo(registry);
                log.info("Redis metrics registered successfully");
            } catch (Exception e) {
                log.error("Failed to register Redis metrics", e);
            }
        };
    }

    /**
     * Inner class for Redis metrics binding.
     */
    private static class RedisMetricsBinder implements MeterBinder {
        private final RedisConnectionFactory connectionFactory;

        public RedisMetricsBinder(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        @Override
        public void bindTo(MeterRegistry registry) {
            // Register connection pool metrics
            registry.gauge("redis.pool.active",
                    Collections.singletonList(Tag.of("type", "active")),
                    connectionFactory, cf -> getActiveConnections());

            registry.gauge("redis.pool.idle",
                    Collections.singletonList(Tag.of("type", "idle")),
                    connectionFactory, cf -> getIdleConnections());

            // Add more metrics as needed
        }

        private double getActiveConnections() {
            try {
                // This is a simplified version - actual implementation may vary depending on connection factory type
                return connectionFactory.getConnection().isClosed() ? 0 : 1;
            } catch (Exception e) {
                return -1;
            }
        }

        private double getIdleConnections() {
            // Simplified version
            return 0;
        }
    }
}