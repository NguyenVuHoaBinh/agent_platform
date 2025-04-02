package viettel.dac.intentanalysisservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.config.LLMProperties;
import viettel.dac.intentanalysisservice.llm.LLMClient;

/**
 * Health indicator for the LLM service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LLMHealthIndicator implements HealthIndicator {

    private final LLMClient llmClient;
    private final LLMProperties llmProperties;

    @Override
    public Health health() {
        try {
            // Send a simple health check prompt to the LLM
            String healthCheckPrompt = "System check: Respond with only 'OK'";
            String response = llmClient.getCompletion(healthCheckPrompt);

            // Check if response contains "OK"
            if (response != null && response.contains("OK")) {
                return Health.up()
                        .withDetail("status", "available")
                        .withDetail("model", llmProperties.getModel())
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "invalid response")
                        .withDetail("response", response)
                        .build();
            }
        } catch (Exception e) {
            log.error("LLM health check failed: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("status", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}