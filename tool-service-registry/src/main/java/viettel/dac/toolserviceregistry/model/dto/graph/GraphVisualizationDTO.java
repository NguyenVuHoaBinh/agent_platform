package viettel.dac.toolserviceregistry.model.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for graph visualization data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphVisualizationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<GraphNodeDTO> nodes = new ArrayList<>();

    @Builder.Default
    private List<GraphEdgeDTO> edges = new ArrayList<>();

    private Map<String, Object> metadata;
}