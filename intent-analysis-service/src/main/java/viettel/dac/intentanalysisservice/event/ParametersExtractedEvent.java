package viettel.dac.intentanalysisservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;

import java.util.List;

/**
 * Event published when parameters have been extracted from intents.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ParametersExtractedEvent extends IntentAnalysisEvent {

    /**
     * User's session identifier.
     */
    private String sessionId;

    /**
     * List of intents with their extracted parameters.
     */
    private List<IntentWithParameters> intents;

    /**
     * Overall confidence score for the parameter extraction.
     */
    private double confidence;

    /**
     * Flag indicating if multiple intents were identified.
     */
    private boolean multiIntent;

    /**
     * Status code (0=pending, 1=active, 2=done).
     */
    private int status;
}