package viettel.dac.intentanalysisservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.query.repository.IntentAnalysisRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for tracking health metrics of the intent analysis service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisHealthService {

    private final IntentAnalysisRepository intentAnalysisRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ConcurrentLinkedQueue<Double> recentConfidences = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Double> averageConfidence = new AtomicReference<>(0.0);
    private final AtomicReference<Integer> queueSize = new AtomicReference<>(0);

    private static final int MAX_CONFIDENCE_SAMPLES = 100;

    /**
     * Record a confidence score for tracking.
     *
     * @param confidence The confidence score
     */
    public void recordConfidence(double confidence) {
        recentConfidences.add(confidence);

        // Trim the queue if it gets too large
        while (recentConfidences.size() > MAX_CONFIDENCE_SAMPLES) {
            recentConfidences.poll();
        }
    }

    /**
     * Update the queue size metric.
     *
     * @param size Current queue size
     */
    public void updateQueueSize(int size) {
        queueSize.set(size);
    }

    /**
     * Get the average confidence of recent analyses.
     *
     * @return The average confidence
     */
    public double getAverageConfidence() {
        return averageConfidence.get();
    }

    /**
     * Get the current queue size.
     *
     * @return The queue size
     */
    public int getQueueSize() {
        return queueSize.get();
    }

    /**
     * Get the circuit breaker state.
     *
     * @return 0=closed, 1=open, 2=half-open
     */
    public int getCircuitBreakerState() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("llmClient");

        switch (breaker.getState()) {
            case CLOSED:
                return 0;
            case OPEN:
                return 1;
            case HALF_OPEN:
                return 2;
            default:
                return -1;
        }
    }

    /**
     * Update average confidence every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void updateAverageConfidence() {
        try {
            if (recentConfidences.isEmpty()) {
                // If no recent samples, try to get from database
                LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
                double dbAverage = intentAnalysisRepository.getAverageConfidenceForPeriod(oneHourAgo, LocalDateTime.now());
                averageConfidence.set(dbAverage);
            } else {
                // Calculate from in-memory samples
                double sum = recentConfidences.stream().mapToDouble(Double::doubleValue).sum();
                averageConfidence.set(sum / recentConfidences.size());
            }

            log.debug("Updated average confidence: {}", averageConfidence.get());
        } catch (Exception e) {
            log.error("Error updating average confidence: {}", e.getMessage(), e);
        }
    }
}