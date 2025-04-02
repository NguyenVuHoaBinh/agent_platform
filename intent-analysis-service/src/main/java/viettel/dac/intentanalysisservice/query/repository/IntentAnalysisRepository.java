package viettel.dac.intentanalysisservice.query.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.intentanalysisservice.model.document.IntentAnalysisDocument;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing intent analysis documents in Elasticsearch.
 */
@Repository
public interface IntentAnalysisRepository extends ElasticsearchRepository<IntentAnalysisDocument, String> {

    /**
     * Find analyses by session ID, ordered by timestamp.
     *
     * @param sessionId Session identifier
     * @param pageable Pagination parameters
     * @return List of analyses for the session
     */
    List<IntentAnalysisDocument> findBySessionIdOrderByTimestampDesc(String sessionId, Pageable pageable);

    /**
     * Find analyses by user input containing the search term, ordered by timestamp.
     *
     * @param searchTerm Search term to match in userInput
     * @param pageable Pagination parameters
     * @return List of matching analyses
     */
    List<IntentAnalysisDocument> findByUserInputContainingOrderByTimestampDesc(String searchTerm, Pageable pageable);

    /**
     * Find analyses by status and timestamp within a range.
     *
     * @param status Status code
     * @param from Start of timestamp range
     * @param to End of timestamp range
     * @param pageable Pagination parameters
     * @return Page of matching analyses
     */
    Page<IntentAnalysisDocument> findByStatusAndTimestampBetween(int status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Find the latest 10 analyses for a session.
     *
     * @param sessionId Session identifier
     * @return List of the 10 most recent analyses
     */
    List<IntentAnalysisDocument> findTop10BySessionIdOrderByTimestampDesc(String sessionId);

    /**
     * Count analyses by status.
     *
     * @param status Status code
     * @return Count of analyses with the status
     */
    long countByStatus(int status);

    /**
     * Count analyses by session ID.
     *
     * @param sessionId Session identifier
     * @return Count of analyses for the session
     */
    long countBySessionId(String sessionId);

    /**
     * Find analyses by specific intent.
     *
     * @param intent Intent name
     * @param pageable Pagination parameters
     * @return List of analyses with the intent
     */
    List<IntentAnalysisDocument> findByIntentsIntentOrderByTimestampDesc(String intent, Pageable pageable);

    /**
     * Find analyses by confidence greater than the threshold.
     *
     * @param threshold Confidence threshold
     * @param pageable Pagination parameters
     * @return List of analyses with high confidence
     */
    List<IntentAnalysisDocument> findByConfidenceGreaterThanOrderByTimestampDesc(double threshold, Pageable pageable);
    // Add this method to the IntentAnalysisRepository interface:

    /**
     * Calculate average confidence for analyses within a time period.
     *
     * @param from Start of time period
     * @param to End of time period
     * @return Average confidence
     */
    double getAverageConfidenceForPeriod(LocalDateTime from, LocalDateTime to);
}