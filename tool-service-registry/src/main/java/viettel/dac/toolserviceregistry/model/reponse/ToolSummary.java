package viettel.dac.toolserviceregistry.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary view of a tool for list responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolSummary {
    private String id;
    private String name;
    private String description;
    private boolean active;
    private int version;
    private LocalDateTime updatedAt;
    private int parameterCount;
    private int dependencyCount;
    @Builder.Default
    private List<String> categories = new ArrayList<>();
    private ToolType toolType;
}
