package viettel.dac.intentanalysisservice.dto;

import lombok.Data;

/**
 * Response for analysis status request.
 */
@Data
public class AnalysisStatusResponse {
    private String analysisId;
    private String status;
    private int progress;
    private IntentAnalysisResponse result;
    private String errorMessage;
}