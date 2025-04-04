package viettel.dac.toolserviceregistry.model.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for graph node visualization data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String label;
    private String type;
    private ToolType toolType;
    private int level;
    private String group;
    private String title;
    private boolean active;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}