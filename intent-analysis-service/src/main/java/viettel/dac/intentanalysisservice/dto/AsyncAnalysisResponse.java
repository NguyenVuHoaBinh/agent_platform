package viettel.dac.intentanalysisservice.dto;

import lombok.Data;

/**
 * Response for asynchronous analysis request.
 */
@Data
public class AsyncAnalysisResponse {
    private String analysisId;
    private String message;
}