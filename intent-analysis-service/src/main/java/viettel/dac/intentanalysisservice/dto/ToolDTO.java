package viettel.dac.intentanalysisservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for Tool information from the Tool Registry Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDTO {
    /**
     * Unique identifier for the tool.
     */
    private String id;

    /**
     * Name of the tool (also serves as the intent name).
     */
    private String name;

    /**
     * Description of the tool's functionality.
     */
    private String description;

    /**
     * List of parameters that the tool accepts.
     */
    private List<ToolParameterDTO> parameters;

    /**
     * Flag indicating if the tool is active.
     */
    private boolean active;
}
