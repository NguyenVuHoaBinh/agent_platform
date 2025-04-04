package viettel.dac.toolserviceregistry.model.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for graph edge visualization data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdgeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String from;
    private String to;
    private DependencyType type;
    private String label;
    private boolean bidirectional;
    private int width;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}