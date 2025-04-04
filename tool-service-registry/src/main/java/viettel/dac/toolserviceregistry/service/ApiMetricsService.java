package viettel.dac.toolserviceregistry.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.enums.HttpMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiMetricsService {
    private final MeterRegistry meterRegistry;

    // Cache for timers to avoid creating new ones for each call
    private final Map<String, Timer> timerCache = new HashMap<>();

    /**
     * Records API call metrics.
     *
     * @param toolId The ID of the API tool
     * @param metadata The API tool metadata
     * @param statusCode The HTTP status code
     * @param durationMs The duration in milliseconds
     * @param success Whether the call was successful
     */
    public void recordApiCall(String toolId, ApiToolMetadataDTO metadata,
                              int statusCode, long durationMs, boolean success) {
        // Record API call count
        Counter.builder("api.call.count")
                .tag("tool", toolId)
                .tag("endpoint", metadata.getEndpointPath())
                .tag("method", metadata.getHttpMethod().name())
                .tag("status", String.valueOf(statusCode))
                .tag("success", Boolean.toString(success))
                .register(meterRegistry)
                .increment();

        // Record API call duration
        Timer timer = getOrCreateTimer(toolId, metadata.getEndpointPath(), metadata.getHttpMethod());
        timer.record(durationMs, TimeUnit.MILLISECONDS);

        // Record API call result in histogram
        meterRegistry.summary("api.call.status",
                        "tool", toolId,
                        "endpoint", metadata.getEndpointPath(),
                        "method", metadata.getHttpMethod().name())
                .record(statusCode);
    }

    /**
     * Records API call error.
     *
     * @param toolId The ID of the API tool
     * @param errorType The type of error
     */
    public void recordApiCallError(String toolId, String errorType) {
        Counter.builder("api.call.error")
                .tag("tool", toolId)
                .tag("errorType", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Gets the success rate for an API tool.
     *
     * @param toolId The ID of the API tool
     * @return The success rate (0.0 to 1.0)
     */
    public double getApiCallSuccessRate(String toolId) {
        double totalCalls = meterRegistry.counter("api.call.count", "tool", toolId).count();
        if (totalCalls == 0) {
            return 0.0;
        }

        double successfulCalls = meterRegistry.counter("api.call.count",
                "tool", toolId, "success", "true").count();
        return successfulCalls / totalCalls;
    }

    /**
     * Gets API call statistics for a tool.
     *
     * @param toolId The ID of the API tool
     * @return Map of statistics
     */
    public Map<String, Object> getApiCallStats(String toolId) {
        Map<String, Object> stats = new HashMap<>();

        // Get timers for this tool
        Timer timer = meterRegistry.find("api.call.duration")
                .tag("tool", toolId)
                .timer();

        if (timer != null) {
            stats.put("count", timer.count());
            stats.put("totalTimeMs", timer.totalTime(TimeUnit.MILLISECONDS));
            stats.put("meanMs", timer.mean(TimeUnit.MILLISECONDS));
            stats.put("maxMs", timer.max(TimeUnit.MILLISECONDS));
            stats.put("p95Ms", timer.percentile(0.95, TimeUnit.MILLISECONDS));
            stats.put("p99Ms", timer.percentile(0.99, TimeUnit.MILLISECONDS));
        } else {
            stats.put("count", 0);
        }

        // Get success rate
        stats.put("successRate", getApiCallSuccessRate(toolId));

        // Get error count
        Counter errorCounter = meterRegistry.find("api.call.error")
                .tag("tool", toolId)
                .counter();

        stats.put("errors", errorCounter != null ? errorCounter.count() : 0);

        return stats;
    }

    /**
     * Gets or creates a timer for an API call.
     */
    private Timer getOrCreateTimer(String toolId, String endpoint, HttpMethod method) {
        String key = toolId + ":" + endpoint + ":" + method.name();

        return timerCache.computeIfAbsent(key, k ->
                Timer.builder("api.call.duration")
                        .tag("tool", toolId)
                        .tag("endpoint", endpoint)
                        .tag("method", method.name())
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(meterRegistry));
    }
}