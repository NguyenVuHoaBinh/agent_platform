package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Class representing a parameter requirement in an execution plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRequirement implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private boolean required;
    private int priority;
    private String description;
    private String examples;
    private String defaultValue;
}