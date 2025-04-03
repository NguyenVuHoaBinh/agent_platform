package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring tool parameter data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterDTO {
    private String id;
    private String name;
    private String description;
    private String parameterType;
    private boolean required;
    private String defaultValue;
    private String validationPattern;
    private String validationMessage;
    private String conditionalOn;
    private int priority;
    private String examples;
    private String suggestionQuery;
}