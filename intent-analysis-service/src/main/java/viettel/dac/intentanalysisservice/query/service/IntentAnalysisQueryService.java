package viettel.dac.intentanalysisservice.query.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;
import viettel.dac.intentanalysisservice.exception.NotFoundException;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.model.document.IntentAnalysisDocument;
import viettel.dac.intentanalysisservice.model.document.IntentDocument;
import viettel.dac.intentanalysisservice.query.repository.IntentAnalysisRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying intent analysis data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisQueryService {

    private final IntentAnalysisRepository repository;

    /**
     * Get analysis history for a session.
     *
     * @param sessionId Session identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return Analysis history response
     */
    public AnalysisHistoryResponse getAnalysisHistory(String sessionId, int page, int size) {
        log.debug("Getting analysis history for session: {}, page: {}, size: {}", sessionId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        List<IntentAnalysisDocument> documents = repository.findBySessionIdOrderByTimestampDesc(sessionId, pageable);

        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setSessionId(sessionId);
        response.setPage(page);
        response.setSize(size);
        response.setTotalCount(repository.countBySessionId(sessionId));
        response.setItems(documents.stream()
                .map(this::mapToHistoryItem)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Get analysis details by ID.
     *
     * @param analysisId Analysis identifier
     * @return Analysis details response
     * @throws NotFoundException if analysis not found
     */
    public IntentAnalysisResponse getAnalysisDetails(String analysisId) {
        log.debug("Getting analysis details for ID: {}", analysisId);

        IntentAnalysisDocument document = repository.findById(analysisId)
                .orElseThrow(() -> new NotFoundException("Analysis not found with ID: " + analysisId));

        return mapToAnalysisResponse(document);
    }

    /**
     * Search for analyses by user input.
     *
     * @param searchTerm Search term
     * @param page Page number (0-based)
     * @param size Page size
     * @return Analysis history response
     */
    public AnalysisHistoryResponse searchAnalyses(String searchTerm, int page, int size) {
        log.debug("Searching analyses with term: {}, page: {}, size: {}", searchTerm, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        List<IntentAnalysisDocument> documents = repository.findByUserInputContainingOrderByTimestampDesc(searchTerm, pageable);

        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setSearchTerm(searchTerm);
        response.setPage(page);
        response.setSize(size);
        response.setItems(documents.stream()
                .map(this::mapToHistoryItem)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Find analyses by specific intent.
     *
     * @param intent Intent name
     * @param page Page number (0-based)
     * @param size Page size
     * @return Analysis history response
     */
    public AnalysisHistoryResponse findAnalysesByIntent(String intent, int page, int size) {
        log.debug("Finding analyses with intent: {}, page: {}, size: {}", intent, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        List<IntentAnalysisDocument> documents = repository.findByIntentsIntentOrderByTimestampDesc(intent, pageable);

        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setIntent(intent);
        response.setPage(page);
        response.setSize(size);
        response.setItems(documents.stream()
                .map(this::mapToHistoryItem)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Map a document to a history item.
     *
     * @param document Analysis document
     * @return History item
     */
    private AnalysisHistoryItem mapToHistoryItem(IntentAnalysisDocument document) {
        AnalysisHistoryItem item = new AnalysisHistoryItem();
        item.setAnalysisId(document.getAnalysisId());
        item.setUserInput(document.getUserInput());
        item.setTimestamp(document.getTimestamp());
        item.setStatus(document.getStatus());
        item.setConfidence(document.getConfidence());

        // Extract primary intent
        if (document.getIntents() != null && !document.getIntents().isEmpty()) {
            IntentDocument primaryIntent = document.getIntents().get(0);
            item.setPrimaryIntent(primaryIntent.getIntent());
            item.setMultiIntent(document.isMultiIntent());
            item.setIntentCount(document.getIntents().size());
        }

        return item;
    }

    /**
     * Map a document to an analysis response.
     *
     * @param document Analysis document
     * @return Analysis response
     */
    private IntentAnalysisResponse mapToAnalysisResponse(IntentAnalysisDocument document) {
        IntentAnalysisResponse response = new IntentAnalysisResponse();
        response.setCode(document.getStatus() == 3 ? "ERROR" : "200");
        response.setAnalysisId(document.getAnalysisId());
        response.setMultiIntent(document.isMultiIntent());
        response.setConfidence(document.getConfidence());

        // Map intents
        if (document.getIntents() != null) {
            List<IntentWithParameters> intents = document.getIntents().stream()
                    .map(this::mapToIntentWithParameters)
                    .collect(Collectors.toList());
            response.setIntents(intents);
        }

        return response;
    }

    /**
     * Map an intent document to an intent with parameters.
     *
     * @param intentDoc Intent document
     * @return Intent with parameters
     */
    private IntentWithParameters mapToIntentWithParameters(IntentDocument intentDoc) {
        IntentWithParameters intent = new IntentWithParameters();
        intent.setIntent(intentDoc.getIntent());
        intent.setConfidence(intentDoc.getConfidence());
        intent.setState(intentDoc.getState());
        intent.setParameters(intentDoc.getParameters());
        return intent;
    }

    /**
     * Analysis history response.
     */
    public static class AnalysisHistoryResponse {
        private String sessionId;
        private String searchTerm;
        private String intent;
        private int page;
        private int size;
        private long totalCount;
        private List<AnalysisHistoryItem> items;

        // Getters and setters
        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public List<AnalysisHistoryItem> getItems() {
            return items;
        }

        public void setItems(List<AnalysisHistoryItem> items) {
            this.items = items;
        }
    }

    /**
     * Analysis history item.
     */
    public static class AnalysisHistoryItem {
        private String analysisId;
        private String userInput;
        private String primaryIntent;
        private boolean multiIntent;
        private int intentCount;
        private int status;
        private double confidence;
        private java.time.LocalDateTime timestamp;

        // Getters and setters
        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }

        public String getPrimaryIntent() {
            return primaryIntent;
        }

        public void setPrimaryIntent(String primaryIntent) {
            this.primaryIntent = primaryIntent;
        }

        public boolean isMultiIntent() {
            return multiIntent;
        }

        public void setMultiIntent(boolean multiIntent) {
            this.multiIntent = multiIntent;
        }

        public int getIntentCount() {
            return intentCount;
        }

        public void setIntentCount(int intentCount) {
            this.intentCount = intentCount;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public java.time.LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(java.time.LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}