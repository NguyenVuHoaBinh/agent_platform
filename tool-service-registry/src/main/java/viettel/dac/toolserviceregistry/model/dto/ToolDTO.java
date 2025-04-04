package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for transferring tool data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDTO {
    private String id;
    private String name;
    private String description;
    private boolean active;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ToolType toolType;
    private ApiToolMetadataDTO apiMetadata;

    @Builder.Default
    private List<ToolParameterDTO> parameters = new ArrayList<>();

    @Builder.Default
    private List<ToolDependencyDTO> dependencies = new ArrayList<>();

    @Builder.Default
    private List<ToolCategoryDTO> categories = new ArrayList<>();

    @Builder.Default
    private List<ToolExampleDTO> examples = new ArrayList<>();
}