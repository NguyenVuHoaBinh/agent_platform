package viettel.dac.intentanalysisservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents an identified intent with extracted parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentWithParameters extends Intent {
    /**
     * The name of the identified intent.
     */
    private String intent;

    /**
     * Map of parameter names to their extracted values.
     */
    private Map<String, Object> parameters;

    /**
     * State of the intent execution (0=not executed, 1=executed).
     */
    private int state;

    /**
     * Confidence score for this intent (0.0 to 1.0).
     */
    private double confidence;
}
