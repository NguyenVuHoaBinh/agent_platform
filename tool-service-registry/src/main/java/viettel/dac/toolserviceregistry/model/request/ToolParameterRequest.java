// File: src/main/java/viettel/dac/toolserviceregistry/model/request/ToolParameterRequest.java
package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ParameterType;

import java.util.List;

/**
 * Enhanced request model for creating or updating a tool parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterRequest {
    private String id;

    @NotBlank(message = "Parameter name is required")
    private String name;

    @NotBlank(message = "Parameter description is required")
    private String description;

    @NotNull(message = "Parameter type is required")
    private ParameterType parameterType;

    private boolean required;

    private String defaultValue;

    private String validationPattern;

    private String validationMessage;

    private String conditionalOn;

    private Integer priority;

    private String examples;

    private String suggestionQuery;

    // New fields
    private ParameterSource parameterSource;

    private String minValue;

    private String maxValue;

    private Integer minLength;

    private Integer maxLength;

    private List<String> allowedValues;

    private String formatHint;

    private boolean sensitive;

    private boolean isArray;

    private String arrayItemType;

    private String objectSchema;

    private String extractionPath;

    // For API tools
    @Valid
    private ApiParameterMappingRequest apiMapping;
}