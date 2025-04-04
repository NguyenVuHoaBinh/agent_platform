package viettel.dac.toolserviceregistry.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for metrics and monitoring.
 */
@Configuration
@Slf4j
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * Customizes the meter registry with common tags.
     *
     * @return The meter registry customizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                Tags.of(
                        Tag.of("application", applicationName),
                        Tag.of("environment", activeProfile)
                )
        );
    }

    /**
     * Creates a timed aspect for @Timed annotation support.
     *
     * @param registry The meter registry
     * @return The timed aspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Creates a counted aspect for @Counted annotation support.
     *
     * @param registry The meter registry
     * @return The counted aspect
     */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    /**
     * Creates cache metrics for the tool graph cache.
     *
     * @param registry The meter registry
     * @param toolGraphCache The tool graph cache
     * @return The cache metrics
     */
    @Bean
    public CaffeineCacheMetrics toolGraphCacheMetrics(
            MeterRegistry registry,
            @Qualifier("toolGraphCache") com.github.benmanes.caffeine.cache.Cache<Object, Object> toolGraphCache) {
        return CaffeineCacheMetrics
                .monitor(registry, toolGraphCache, "tool_graph_cache");
    }

    /**
     * Creates cache metrics for the execution plans cache.
     *
     * @param registry The meter registry
     * @param executionPlansCache The execution plans cache
     * @return The cache metrics
     */
    @Bean
    public CaffeineCacheMetrics executionPlansCacheMetrics(
            MeterRegistry registry,
            @Qualifier("executionPlansCache") com.github.benmanes.caffeine.cache.Cache<Object, Object> executionPlansCache) {
        return CaffeineCacheMetrics
                .monitor(registry, executionPlansCache, "execution_plans_cache");
    }

    /**
     * Initializes the registry with system metrics.
     *
     * @param registry The meter registry
     */
    @PostConstruct
    public void setUp(MeterRegistry registry) {
        // Register JVM memory metrics
        registry.gauge("jvm.memory.used",
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        registry.gauge("jvm.memory.available",
                Runtime.getRuntime().maxMemory());

        log.info("Metrics configured for application: {}, environment: {}",
                applicationName, activeProfile);
    }
}