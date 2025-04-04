package viettel.dac.toolserviceregistry.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.dto.*;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model for detailed tool information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDetailResponse {
    private String id;
    private String name;
    private String description;
    @Builder.Default
    private List<ToolParameterDTO> parameters = new ArrayList<>();
    @Builder.Default
    private List<ToolDependencyDTO> dependencies = new ArrayList<>();
    @Builder.Default
    private List<ToolCategoryDTO> categories = new ArrayList<>();
    @Builder.Default
    private List<ToolExampleDTO> examples = new ArrayList<>();
    private boolean active;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ToolType toolType;
    private ApiToolMetadataDTO apiMetadata;
}
