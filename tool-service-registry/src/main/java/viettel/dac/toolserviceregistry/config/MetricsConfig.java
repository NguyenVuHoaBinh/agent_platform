package viettel.dac.toolserviceregistry.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MetricsConfig implements ApplicationListener<ApplicationStartedEvent> {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                Tags.of(
                        Tag.of("application", applicationName),
                        Tag.of("environment", activeProfile)
                )
        );
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        MeterRegistry registry = event.getApplicationContext().getBean(MeterRegistry.class);

        // Register JVM memory metrics
        registry.gauge("jvm.memory.used",
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        registry.gauge("jvm.memory.available",
                Runtime.getRuntime().maxMemory());

        log.info("Metrics configured for application: {}, environment: {}",
                applicationName, activeProfile);
    }
}