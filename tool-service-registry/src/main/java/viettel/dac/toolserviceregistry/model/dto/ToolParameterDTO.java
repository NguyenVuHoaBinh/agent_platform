package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ParameterType;

import java.io.Serializable;
import java.util.List;

/**
 * Enhanced DTO for transferring tool parameter data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    private ParameterType parameterType;
    private boolean required;
    private String defaultValue;
    private String validationPattern;
    private String validationMessage;
    private String conditionalOn;
    private int priority;
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
    private ApiParameterMappingDTO apiMapping;
}