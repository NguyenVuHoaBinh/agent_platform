package viettel.dac.toolserviceregistry.graph;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;

/**
 * A generic directed graph implementation for managing dependencies between nodes.
 *
 * @param <T> The type of nodes in the graph
 */
@Slf4j
public class DirectedGraph<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<T, Set<T>> outgoingEdges = new HashMap<>();
    private final Map<T, Set<T>> incomingEdges = new HashMap<>();

    /**
     * Adds a node to the graph if it doesn't already exist.
     *
     * @param node The node to add
     */
    public void addNode(T node) {
        outgoingEdges.putIfAbsent(node, new HashSet<>());
        incomingEdges.putIfAbsent(node, new HashSet<>());
    }

    /**
     * Adds a directed edge from one node to another.
     * Both nodes will be added if they don't already exist.
     *
     * @param from The source node
     * @param to The target node
     */
    public void addEdge(T from, T to) {
        addNode(from);
        addNode(to);
        outgoingEdges.get(from).add(to);
        incomingEdges.get(to).add(from);
    }

    /**
     * Gets the outgoing edges (successors) of a node.
     *
     * @param node The node
     * @return Set of nodes that have an incoming edge from the given node
     */
    public Set<T> getOutgoingEdges(T node) {
        return outgoingEdges.getOrDefault(node, Collections.emptySet());
    }

    /**
     * Gets the incoming edges (predecessors) of a node.
     *
     * @param node The node
     * @return Set of nodes that have an outgoing edge to the given node
     */
    public Set<T> getIncomingEdges(T node) {
        return incomingEdges.getOrDefault(node, Collections.emptySet());
    }

    /**
     * Gets all nodes in the graph.
     *
     * @return Set of all nodes in the graph
     */
    public Set<T> getAllNodes() {
        return new HashSet<>(outgoingEdges.keySet());
    }

    /**
     * Performs a topological sort of the graph.
     *
     * @return List of nodes in topological order
     * @throws IllegalStateException if the graph contains a cycle
     */
    public List<T> topologicalSort() {
        Set<T> visited = new HashSet<>();
        Set<T> temporaryMark = new HashSet<>();
        List<T> result = new ArrayList<>();

        for (T node : outgoingEdges.keySet()) {
            if (!visited.contains(node)) {
                topologicalSortVisit(node, visited, temporaryMark, result);
            }
        }

        Collections.reverse(result);
        return result;
    }

    /**
     * Creates a subgraph containing only the specified nodes and the edges between them.
     *
     * @param nodes The nodes to include in the subgraph
     * @return A new DirectedGraph containing only the specified nodes
     */
    public DirectedGraph<T> subgraphWithNodes(Set<T> nodes) {
        DirectedGraph<T> subgraph = new DirectedGraph<>();

        for (T node : nodes) {
            subgraph.addNode(node);
        }

        for (T from : nodes) {
            for (T to : getOutgoingEdges(from)) {
                if (nodes.contains(to)) {
                    subgraph.addEdge(from, to);
                }
            }
        }

        return subgraph;
    }

    /**
     * Checks if the graph contains any cycles.
     *
     * @return true if the graph contains a cycle, false otherwise
     */
    public boolean hasCycles() {
        try {
            topologicalSort();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /**
     * Gets the transitive closure of a node (all nodes reachable from the given node).
     *
     * @param node The starting node
     * @return Set of all nodes reachable from the given node
     */
    public Set<T> getTransitiveClosure(T node) {
        Set<T> result = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(node);

        while (!queue.isEmpty()) {
            T current = queue.poll();
            for (T adj : getOutgoingEdges(current)) {
                if (result.add(adj)) {
                    queue.add(adj);
                }
            }
        }

        return result;
    }

    /**
     * Gets all paths from one node to another.
     *
     * @param from The starting node
     * @param to The destination node
     * @return List of all paths from the starting node to the destination node
     */
    public List<List<T>> getAllPaths(T from, T to) {
        List<List<T>> result = new ArrayList<>();
        List<T> currentPath = new ArrayList<>();
        currentPath.add(from);

        findAllPaths(from, to, new HashSet<>(), currentPath, result);

        return result;
    }

    /**
     * Finds the shortest path from one node to another.
     *
     * @param from The starting node
     * @param to The destination node
     * @return The shortest path, or an empty list if no path exists
     */
    public List<T> getShortestPath(T from, T to) {
        if (from.equals(to)) {
            return Collections.singletonList(from);
        }

        Map<T, T> predecessors = new HashMap<>();
        Queue<T> queue = new LinkedList<>();
        Set<T> visited = new HashSet<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            T current = queue.poll();

            for (T neighbor : getOutgoingEdges(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    predecessors.put(neighbor, current);
                    queue.add(neighbor);

                    if (neighbor.equals(to)) {
                        // Found the destination, reconstruct the path
                        return reconstructPath(from, to, predecessors);
                    }
                }
            }
        }

        // No path found
        return Collections.emptyList();
    }

    /**
     * Removes a node and all its edges from the graph.
     *
     * @param node The node to remove
     */
    public void removeNode(T node) {
        Set<T> incoming = new HashSet<>(getIncomingEdges(node));
        Set<T> outgoing = new HashSet<>(getOutgoingEdges(node));

        for (T in : incoming) {
            outgoingEdges.get(in).remove(node);
        }

        for (T out : outgoing) {
            incomingEdges.get(out).remove(node);
        }

        outgoingEdges.remove(node);
        incomingEdges.remove(node);
    }

    /**
     * Removes an edge from the graph.
     *
     * @param from The source node
     * @param to The target node
     */
    public void removeEdge(T from, T to) {
        if (outgoingEdges.containsKey(from)) {
            outgoingEdges.get(from).remove(to);
        }

        if (incomingEdges.containsKey(to)) {
            incomingEdges.get(to).remove(from);
        }
    }

    /**
     * Helper method for the topological sort that visits a node and its dependencies recursively.
     */
    private void topologicalSortVisit(T node, Set<T> visited, Set<T> temporaryMark, List<T> result) {
        if (temporaryMark.contains(node)) {
            throw new IllegalStateException("Cycle detected in graph, cannot perform topological sort");
        }

        if (!visited.contains(node)) {
            temporaryMark.add(node);

            for (T dependency : getIncomingEdges(node)) {
                topologicalSortVisit(dependency, visited, temporaryMark, result);
            }

            visited.add(node);
            temporaryMark.remove(node);
            result.add(node);
        }
    }

    /**
     * Helper method to find all paths between two nodes recursively.
     */
    private void findAllPaths(T current, T destination, Set<T> visited, List<T> currentPath, List<List<T>> result) {
        if (current.equals(destination)) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        visited.add(current);

        for (T neighbor : getOutgoingEdges(current)) {
            if (!visited.contains(neighbor)) {
                currentPath.add(neighbor);
                findAllPaths(neighbor, destination, visited, currentPath, result);
                currentPath.remove(currentPath.size() - 1);
            }
        }

        visited.remove(current);
    }

    /**
     * Helper method to reconstruct a path from the predecessors map.
     */
    private List<T> reconstructPath(T from, T to, Map<T, T> predecessors) {
        List<T> path = new ArrayList<>();
        T current = to;

        while (!current.equals(from)) {
            path.add(current);
            current = predecessors.get(current);
        }

        path.add(from);
        Collections.reverse(path);

        return path;
    }
}