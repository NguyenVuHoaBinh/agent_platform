package viettel.dac.intentanalysisservice.model;

import lombok.Data;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;

/**
 * Status of an asynchronous analysis.
 */
@Data
public class AnalysisStatus {
    private String status;
    private int progress;
    private IntentAnalysisResponse result;
    private String errorMessage;
}