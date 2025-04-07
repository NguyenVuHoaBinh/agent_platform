package viettel.dac.toolserviceregistry.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for tracking and recording tool usage metrics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Cache to keep track of recent usage - will be periodically cleared
    private final ConcurrentHashMap<String, AtomicInteger> recentToolUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Double>> toolConfidenceStats = new ConcurrentHashMap<>();

    /**
     * Records usage of a tool based on intent analysis results.
     *
     * @param toolId The ID of the tool
     * @param confidence The confidence score of the intent match
     */
    public void recordToolUsage(String toolId, double confidence) {
        // Increment counter for tool usage
        meterRegistry.counter("tool.usage",
                "toolId", toolId,
                "confidenceRange", getConfidenceRange(confidence)).increment();

        // Record confidence as a distribution summary
        meterRegistry.summary("tool.confidence", "toolId", toolId)
                .record(confidence);

        // Update in-memory tracking
        recentToolUsage.computeIfAbsent(toolId, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Track confidence stats
        toolConfidenceStats.computeIfAbsent(toolId, k -> new HashMap<>());
        Map<String, Double> stats = toolConfidenceStats.get(toolId);

        // Update min, max, sum, count for calculating average
        stats.compute("min", (k, v) -> v == null ? confidence : Math.min(v, confidence));
        stats.compute("max", (k, v) -> v == null ? confidence : Math.max(v, confidence));
        stats.compute("sum", (k, v) -> v == null ? confidence : v + confidence);
        stats.compute("count", (k, v) -> v == null ? 1.0 : v + 1.0);

        log.debug("Recorded usage for tool: {} with confidence: {}", toolId, confidence);
    }

    /**
     * Records a failed tool usage.
     *
     * @param toolId The ID of the tool
     * @param reason The reason for the failure
     */
    public void recordToolUsageFailure(String toolId, String reason) {
        meterRegistry.counter("tool.usage.failure",
                "toolId", toolId,
                "reason", reason).increment();

        log.debug("Recorded failed usage for tool: {} with reason: {}", toolId, reason);
    }

    /**
     * Gets the confidence range category for a confidence score.
     */
    private String getConfidenceRange(double confidence) {
        if (confidence >= 0.9) return "high";
        if (confidence >= 0.7) return "medium";
        if (confidence >= 0.5) return "low";
        return "very_low";
    }

    /**
     * Gets metrics for a specific tool.
     *
     * @param toolId The ID of the tool
     * @return Map of metrics
     */
    public Map<String, Object> getToolMetrics(String toolId) {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("id", toolId);
        metrics.put("recentUsageCount", recentToolUsage.getOrDefault(toolId, new AtomicInteger(0)).get());

        // Calculate average confidence if available
        Map<String, Double> stats = toolConfidenceStats.getOrDefault(toolId, new HashMap<>());
        Double count = stats.getOrDefault("count", 0.0);
        if (count > 0) {
            Double sum = stats.getOrDefault("sum", 0.0);
            metrics.put("averageConfidence", sum / count);
            metrics.put("minConfidence", stats.getOrDefault("min", 0.0));
            metrics.put("maxConfidence", stats.getOrDefault("max", 0.0));
        }

        metrics.put("lastUpdated", LocalDateTime.now().toString());

        return metrics;
    }

    /**
     * Gets metrics for all tools.
     *
     * @return Map of tool ID to metrics
     */
    public Map<String, Map<String, Object>> getAllToolMetrics() {
        Map<String, Map<String, Object>> allMetrics = new HashMap<>();

        for (String toolId : recentToolUsage.keySet()) {
            allMetrics.put(toolId, getToolMetrics(toolId));
        }

        return allMetrics;
    }

    /**
     * Clears older metrics data to prevent memory leaks.
     * This should be called periodically, e.g., by a scheduled task.
     */
    public void clearOldMetrics() {
        // In a real implementation, you might keep only data from the last day/week
        // For simplicity, we're not implementing time-based cleanup here
        log.info("Clearing old metrics data");
    }
}