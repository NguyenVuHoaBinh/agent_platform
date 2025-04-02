package viettel.dac.intentanalysisservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Command to extract parameters for identified intents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractParametersCommand {

    /**
     * ID of the analysis that identified the intents.
     */
    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    /**
     * The original user input.
     */
    @NotBlank(message = "User input is required")
    private String userInput;

    /**
     * List of intents identified in the analysis step.
     */
    @NotEmpty(message = "At least one intent is required")
    private List<Intent> intents;

    /**
     * Language code (e.g., "en" for English). Defaults to "en" if not specified.
     */
    private String language;

    /**
     * Optional metadata for additional context.
     */
    private Map<String, Object> metadata;
}
