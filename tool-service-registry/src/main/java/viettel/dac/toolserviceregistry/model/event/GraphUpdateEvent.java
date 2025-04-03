package viettel.dac.toolserviceregistry.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Event representing a dependency graph update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphUpdateEvent extends BaseEvent {
    private String toolId;

    @Builder.Default
    private List<String> nodes = new ArrayList<>();

    @Builder.Default
    private List<GraphEdge> edges = new ArrayList<>();
}
