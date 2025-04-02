package viettel.dac.intentanalysisservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import viettel.dac.intentanalysisservice.model.Intent;

import java.util.List;

/**
 * Event published when an intent analysis is completed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class IntentAnalysisCompletedEvent extends IntentAnalysisEvent {

    /**
     * The user's input text that was analyzed.
     */
    private String userInput;

    /**
     * User's session identifier.
     */
    private String sessionId;

    /**
     * List of intents identified in the analysis.
     */
    private List<Intent> intents;

    /**
     * Overall confidence score for the analysis.
     */
    private double confidence;

    /**
     * Time taken to process in milliseconds.
     */
    private long processingTimeMs;
}
