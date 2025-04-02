package viettel.dac.intentanalysisservice.dto;

import lombok.Data;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;

import java.util.List;

/**
 * Response for intent analysis.
 */
@Data
public class IntentAnalysisResponse {
    private String code;
    private String analysisId;
    private List<IntentWithParameters> intents;
    private boolean multiIntent;
    private double confidence;
}