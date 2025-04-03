package viettel.dac.toolserviceregistry.model.entity;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a parameter requirement in an execution plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRequirement {
    private String name;
    private String description;
    private boolean required;
    private int priority;
    private String parameterType;
    private String defaultValue;
    private String examples;
}
