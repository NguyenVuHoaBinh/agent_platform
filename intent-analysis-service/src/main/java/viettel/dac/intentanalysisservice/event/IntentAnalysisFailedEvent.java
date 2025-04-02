package viettel.dac.intentanalysisservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event published when an intent analysis fails.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class IntentAnalysisFailedEvent extends IntentAnalysisEvent {

    /**
     * The user's input text that was being analyzed.
     */
    private String userInput;

    /**
     * User's session identifier.
     */
    private String sessionId;

    /**
     * Error message describing the failure.
     */
    private String errorMessage;

    /**
     * Type of error that occurred.
     */
    private String errorType;

    /**
     * Step where the failure occurred.
     */
    private String failedStep;
}
