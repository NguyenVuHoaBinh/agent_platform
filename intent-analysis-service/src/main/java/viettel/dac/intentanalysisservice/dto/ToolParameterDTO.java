package viettel.dac.intentanalysisservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Tool Parameter information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterDTO {
    /**
     * Name of the parameter.
     */
    private String name;

    /**
     * Description of the parameter.
     */
    private String description;

    /**
     * Data type of the parameter (e.g., string, number, boolean).
     */
    private String parameterType;

    /**
     * Flag indicating if the parameter is required.
     */
    private boolean required;

    /**
     * Default value for the parameter (optional).
     */
    private String defaultValue;
}
