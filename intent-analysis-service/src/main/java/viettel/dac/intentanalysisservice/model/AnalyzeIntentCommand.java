package viettel.dac.intentanalysisservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Command to analyze user input for intents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeIntentCommand {

    /**
     * Optional ID for the analysis. If not provided, a new UUID will be generated.
     */
    private String analysisId;

    /**
     * The user's input text to analyze.
     */
    @NotBlank(message = "User input is required")
    private String userInput;

    /**
     * User's session identifier for tracking conversation context.
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /**
     * Optional list of specific tool IDs to consider in the analysis.
     * If null or empty, all available tools will be used.
     */
    private List<String> toolIds;

    /**
     * Language code (e.g., "en" for English). Defaults to "en" if not specified.
     */
    private String language;

    /**
     * Optional metadata for additional context.
     */
    private Map<String, Object> metadata;
}