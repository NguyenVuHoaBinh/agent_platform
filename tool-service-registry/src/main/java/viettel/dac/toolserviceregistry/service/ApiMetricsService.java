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

    // Constants for metric names
    private static final String METRIC_API_CALL_COUNT = "api.call.count";
    private static final String METRIC_API_CALL_DURATION = "api.call.duration";
    private static final String METRIC_API_CALL_STATUS = "api.call.status";
    private static final String METRIC_API_CALL_ERROR = "api.call.error";

    // Constants for tag names
    private static final String TAG_TOOL = "tool";
    private static final String TAG_ENDPOINT = "endpoint";
    private static final String TAG_METHOD = "method";
    private static final String TAG_STATUS = "status";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_ERROR_TYPE = "errorType";

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
        Counter.builder(METRIC_API_CALL_COUNT)
                .tag(TAG_TOOL, toolId)
                .tag(TAG_ENDPOINT, metadata.getEndpointPath())
                .tag(TAG_METHOD, metadata.getHttpMethod().name())
                .tag(TAG_STATUS, String.valueOf(statusCode))
                .tag(TAG_SUCCESS, Boolean.toString(success))
                .register(meterRegistry)
                .increment();

        // Record API call duration
        Timer timer = getOrCreateTimer(toolId, metadata.getEndpointPath(), metadata.getHttpMethod());
        timer.record(durationMs, TimeUnit.MILLISECONDS);

        // Record API call result in histogram
        meterRegistry.summary(METRIC_API_CALL_STATUS,
                        TAG_TOOL, toolId,
                        TAG_ENDPOINT, metadata.getEndpointPath(),
                        TAG_METHOD, metadata.getHttpMethod().name())
                .record(statusCode);
    }

    /**
     * Records API call error.
     *
     * @param toolId The ID of the API tool
     * @param errorType The type of error
     */
    public void recordApiCallError(String toolId, String errorType) {
        Counter.builder(METRIC_API_CALL_ERROR)
                .tag(TAG_TOOL, toolId)
                .tag(TAG_ERROR_TYPE, errorType)
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
        double totalCalls = meterRegistry.counter(METRIC_API_CALL_COUNT, TAG_TOOL, toolId).count();
        if (totalCalls == 0) {
            return 0.0;
        }

        double successfulCalls = meterRegistry.counter(METRIC_API_CALL_COUNT,
                TAG_TOOL, toolId, TAG_SUCCESS, "true").count();
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
        Timer timer = meterRegistry.find(METRIC_API_CALL_DURATION)
                .tag(TAG_TOOL, toolId)
                .timer();

        if (timer != null) {
            stats.put("count", timer.count());
            stats.put("totalTimeMs", timer.totalTime(TimeUnit.MILLISECONDS));
            stats.put("meanMs", timer.mean(TimeUnit.MILLISECONDS));
            stats.put("maxMs", timer.max(TimeUnit.MILLISECONDS));
            stats.put("p95Ms", timer.percentile(0.95, TimeUnit.MILLISECONDS)); // Changed from percentile to quantile
            stats.put("p99Ms", timer.percentile(0.99, TimeUnit.MILLISECONDS)); // Changed from percentile to quantile
        } else {
            stats.put("count", 0);
        }

        // Get success rate
        stats.put("successRate", getApiCallSuccessRate(toolId));

        // Get error count
        Counter errorCounter = meterRegistry.find(METRIC_API_CALL_ERROR)
                .tag(TAG_TOOL, toolId)
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
                Timer.builder(METRIC_API_CALL_DURATION)
                        .tag(TAG_TOOL, toolId)
                        .tag(TAG_ENDPOINT, endpoint)
                        .tag(TAG_METHOD, method.name())
                        // Remove deprecated publishPercentiles and publishPercentileHistogram
                        // Use one of these alternatives depending on your Micrometer version:
                        .register(meterRegistry));
    }
}
