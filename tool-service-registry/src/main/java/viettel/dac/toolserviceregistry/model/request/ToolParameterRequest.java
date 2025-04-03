package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for creating or updating a tool parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterRequest {
    /**
     * Optional ID for the parameter. Required for updates, ignored for creation.
     */
    private String id;

    /**
     * Required name of the parameter.
     */
    @NotBlank(message = "Parameter name is required")
    private String name;

    /**
     * Required description of the parameter.
     */
    @NotBlank(message = "Parameter description is required")
    private String description;

    /**
     * Required type of the parameter (e.g., string, number, boolean, array, object).
     */
    @NotBlank(message = "Parameter type is required")
    private String parameterType;

    /**
     * Flag indicating whether the parameter is required.
     */
    private boolean required;

    /**
     * Optional default value for the parameter.
     */
    private String defaultValue;

    /**
     * Optional validation pattern for the parameter (regex).
     */
    private String validationPattern;

    /**
     * Optional validation message for the parameter.
     */
    private String validationMessage;

    /**
     * Optional condition for when this parameter is required or visible.
     */
    private String conditionalOn;

    /**
     * Optional priority for the parameter (used for ordering).
     */
    private Integer priority;

    /**
     * Optional examples for the parameter.
     */
    private String examples;

    /**
     * Optional suggestion query for the parameter.
     */
    private String suggestionQuery;
}