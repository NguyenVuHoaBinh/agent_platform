package viettel.dac.toolserviceregistry.model.event;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;

/**
 * Represents an edge in the dependency graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {
    private String from;
    private String to;
    private DependencyType type;
}
