package viettel.dac.intentanalysisservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published when an intent analysis is started.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class IntentAnalysisStartedEvent extends IntentAnalysisEvent {

    /**
     * The user's input text being analyzed.
     */
    private String userInput;

    /**
     * User's session identifier.
     */
    private String sessionId;

    /**
     * List of tool IDs to consider in the analysis.
     */
    private List<String> toolIds;

    /**
     * Language code of the input.
     */
    private String language;
}
