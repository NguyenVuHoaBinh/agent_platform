package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring tool category data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCategoryDTO {
    private String id;
    private String name;
    private String description;
}