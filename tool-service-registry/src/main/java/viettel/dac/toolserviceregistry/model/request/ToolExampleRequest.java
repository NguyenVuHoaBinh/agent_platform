package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request model for creating or updating a tool example.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExampleRequest {
    /**
     * Optional ID for the example. Required for updates, ignored for creation.
     */
    private String id;

    /**
     * Required input text for the example.
     */
    @NotBlank(message = "Input text is required")
    private String inputText;

    /**
     * Required output parameters for the example.
     */
    @NotNull(message = "Output parameters are required")
    @Builder.Default
    private Map<String, Object> outputParameters = new HashMap<>();
}