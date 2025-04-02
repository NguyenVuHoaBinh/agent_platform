package viettel.dac.intentanalysisservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an identified intent with its confidence score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Intent {
    /**
     * The name of the identified intent.
     */
    private String intent;

    /**
     * Confidence score for this intent (0.0 to 1.0).
     */
    private double confidence;
}
