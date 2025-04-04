package viettel.dac.toolserviceregistry.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.graph.DirectedGraph;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;
import viettel.dac.toolserviceregistry.model.event.GraphEdge;
import viettel.dac.toolserviceregistry.model.event.GraphUpdateEvent;
import viettel.dac.toolserviceregistry.repository.ToolDependencyRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing the tool dependency graph.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "toolGraph")
public class ToolDependencyGraphService {
    private final ToolRepository toolRepository;
    private final ToolDependencyRepository dependencyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topic.tool-dependency-events}")
    private String toolDependencyEventsTopic;

    /**
     * Builds a directed graph of tool dependencies.
     *
     * @param requiredOnly Flag to include only required dependencies
     * @return The directed graph
     */
    @Cacheable(key = "'graph-' + #requiredOnly")
    public DirectedGraph<String> buildDependencyGraph(boolean requiredOnly) {
        log.debug("Building dependency graph, requiredOnly: {}", requiredOnly);
        Timer.Sample sample = Timer.start(meterRegistry);

        DirectedGraph<String> graph = new DirectedGraph<>();

        // Get all active tools
        List<Tool> tools = toolRepository.findAllByActiveTrue();

        // Add all tools as nodes
        tools.forEach(tool -> graph.addNode(tool.getId()));

        // Add edges for dependencies
        for (Tool tool : tools) {
            for (ToolDependency dep : tool.getDependencies()) {
                if (!requiredOnly || dep.getDependencyType() == DependencyType.REQUIRED) {
                    // Edge direction: dependency -> tool (means tool depends on dependency)
                    graph.addEdge(dep.getDependencyTool().getId(), tool.getId());
                }
            }
        }

        long elapsedTime = sample.stop(meterRegistry.timer("dependency.graph.build.time"));
        log.debug("Built dependency graph with {} nodes in {}ms", graph.getAllNodes().size(), elapsedTime / 1_000_000);

        // Record metrics
        meterRegistry.gauge("dependency.graph.nodes", graph.getAllNodes().size());

        return graph;
    }

    /**
     * Performs a topological sort of the tools.
     *
     * @param selectedToolIds The IDs of the selected tools
     * @return List of tool IDs in topological order
     */
    public List<String> topologicalSort(List<String> selectedToolIds) {
        log.debug("Performing topological sort for tools: {}", selectedToolIds);
        Timer.Sample sample = Timer.start(meterRegistry);

        // Get dependency closure to include all required dependencies
        Set<String> allToolIds = getDependencyClosure(selectedToolIds);

        // Build subgraph with these tools
        DirectedGraph<String> fullGraph = buildDependencyGraph(true);
        DirectedGraph<String> subgraph = fullGraph.subgraphWithNodes(allToolIds);

        // Perform topological sort
        List<String> result = subgraph.topologicalSort();

        long elapsedTime = sample.stop(meterRegistry.timer("dependency.graph.topological.sort.time"));
        log.debug("Completed topological sort in {}ms", elapsedTime / 1_000_000);

        return result;
    }

    /**
     * Gets the dependency closure for a set of tools.
     *
     * @param toolIds The IDs of the tools
     * @return Set of all tool IDs in the dependency closure
     */
    public Set<String> getDependencyClosure(List<String> toolIds) {
        log.debug("Calculating dependency closure for tools: {}", toolIds);
        Timer.Sample sample = Timer.start(meterRegistry);

        Set<String> closure = new HashSet<>(toolIds);
        DirectedGraph<String> graph = buildDependencyGraph(true);

        // Process each tool
        Queue<String> queue = new LinkedList<>(toolIds);
        while (!queue.isEmpty()) {
            String current = queue.poll();

            // Get direct dependencies (incoming edges in the graph)
            for (String dep : graph.getIncomingEdges(current)) {
                if (closure.add(dep)) {
                    // If this is a new dependency, add it to the queue
                    queue.add(dep);
                }
            }
        }

        long elapsedTime = sample.stop(meterRegistry.timer("dependency.graph.closure.time"));
        log.debug("Calculated dependency closure with {} tools in {}ms", closure.size(), elapsedTime / 1_000_000);

        return closure;
    }

    /**
     * Publishes a graph update event to Kafka.
     *
     * @param toolId The ID of the tool that triggered the update
     */
    @CacheEvict(allEntries = true)
    public void publishGraphUpdate(String toolId) {
        log.debug("Publishing graph update for tool: {}", toolId);
        Timer.Sample sample = Timer.start(meterRegistry);

        DirectedGraph<String> graph = buildDependencyGraph(false);

        GraphUpdateEvent event = new GraphUpdateEvent();
        event.setToolId(toolId);
        event.setTimestamp(LocalDateTime.now());
        event.setNodes(new ArrayList<>(graph.getAllNodes()));
        event.setEdges(convertGraphToEdges(graph));

        kafkaTemplate.send(toolDependencyEventsTopic, toolId, event);

        long elapsedTime = sample.stop(meterRegistry.timer("dependency.graph.publish.time"));
        log.debug("Published graph update in {}ms", elapsedTime / 1_000_000);

        meterRegistry.counter("dependency.graph.publish.count").increment();
    }

    /**
     * Converts the graph to a list of edges.
     *
     * @param graph The directed graph
     * @return List of graph edges
     */
    private List<GraphEdge> convertGraphToEdges(DirectedGraph<String> graph) {
        List<GraphEdge> edges = new ArrayList<>();

        for (String from : graph.getAllNodes()) {
            for (String to : graph.getOutgoingEdges(from)) {
                GraphEdge edge = new GraphEdge();
                edge.setFrom(from);
                edge.setTo(to);
                // Determine dependency type (requires querying the database)
                ToolDependency dependency = dependencyRepository.findByToolIdAndDependencyToolId(to, from);
                edge.setType(dependency != null ? dependency.getDependencyType() : DependencyType.REQUIRED);
                edges.add(edge);
            }
        }

        return edges;
    }
}