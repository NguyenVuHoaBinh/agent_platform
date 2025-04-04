package viettel.dac.toolserviceregistry.graph;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A generic directed graph implementation for managing dependencies between nodes.
 * Enhanced with more algorithms and analysis features.
 */
@Slf4j
public class DirectedGraph<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<T, Set<T>> outgoingEdges = new HashMap<>();
    private final Map<T, Set<T>> incomingEdges = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();

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
     * Gets the number of nodes in the graph.
     *
     * @return The number of nodes
     */
    public int getNodeCount() {
        return outgoingEdges.size();
    }

    /**
     * Gets the number of edges in the graph.
     *
     * @return The number of edges
     */
    public int getEdgeCount() {
        return outgoingEdges.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * Gets the metadata for the graph.
     *
     * @return The metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata for the graph.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Calculates the in-degree (number of incoming edges) for each node.
     *
     * @return Map of node to in-degree
     */
    public Map<T, Integer> calculateInDegrees() {
        Map<T, Integer> inDegrees = new HashMap<>();

        for (T node : getAllNodes()) {
            inDegrees.put(node, getIncomingEdges(node).size());
        }

        return inDegrees;
    }

    /**
     * Calculates the out-degree (number of outgoing edges) for each node.
     *
     * @return Map of node to out-degree
     */
    public Map<T, Integer> calculateOutDegrees() {
        Map<T, Integer> outDegrees = new HashMap<>();

        for (T node : getAllNodes()) {
            outDegrees.put(node, getOutgoingEdges(node).size());
        }

        return outDegrees;
    }

    /**
     * Finds source nodes (nodes with no incoming edges).
     *
     * @return Set of source nodes
     */
    public Set<T> findSourceNodes() {
        return getAllNodes().stream()
                .filter(node -> getIncomingEdges(node).isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Finds sink nodes (nodes with no outgoing edges).
     *
     * @return Set of sink nodes
     */
    public Set<T> findSinkNodes() {
        return getAllNodes().stream()
                .filter(node -> getOutgoingEdges(node).isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Finds isolated nodes (nodes with no incoming or outgoing edges).
     *
     * @return Set of isolated nodes
     */
    public Set<T> findIsolatedNodes() {
        return getAllNodes().stream()
                .filter(node -> getIncomingEdges(node).isEmpty() && getOutgoingEdges(node).isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Finds articulation points (nodes whose removal would increase the number of connected components).
     *
     * @return Set of articulation points
     */
    public Set<T> findArticulationPoints() {
        Set<T> articulationPoints = new HashSet<>();
        Map<T, Integer> discoveryTime = new HashMap<>();
        Map<T, Integer> lowTime = new HashMap<>();
        Map<T, T> parent = new HashMap<>();
        Set<T> visited = new HashSet<>();

        // Initialize discovery time, low time, and parent
        for (T node : getAllNodes()) {
            discoveryTime.put(node, -1);
            lowTime.put(node, -1);
            parent.put(node, null);
        }

        // Process each connected component
        int time = 0;
        for (T node : getAllNodes()) {
            if (discoveryTime.get(node) == -1) {
                // Find articulation points in DFS tree rooted at node
                time = findArticulationPointsDFS(node, time, discoveryTime, lowTime, parent, visited, articulationPoints);
            }
        }

        return articulationPoints;
    }

    /**
     * DFS helper method for finding articulation points.
     */
    private int findArticulationPointsDFS(T node, int time, Map<T, Integer> discoveryTime, Map<T, Integer> lowTime,
                                          Map<T, T> parent, Set<T> visited, Set<T> articulationPoints) {
        // Count of children in DFS tree
        int children = 0;

        // Mark the current node as visited
        visited.add(node);

        // Initialize discovery time and low value
        time++;
        discoveryTime.put(node, time);
        lowTime.put(node, time);

        // Iterate through all adjacent vertices
        for (T adjacent : getOutgoingEdges(node)) {
            // If adjacent is not visited yet, then make it a child of node in DFS tree and recur for it
            if (!visited.contains(adjacent)) {
                children++;
                parent.put(adjacent, node);

                time = findArticulationPointsDFS(adjacent, time, discoveryTime, lowTime, parent, visited, articulationPoints);

                // Check if the subtree rooted at adjacent has a connection to one of the ancestors of node
                lowTime.put(node, Math.min(lowTime.get(node), lowTime.get(adjacent)));

                // If node is not root and low value of one of its children is more than or equal to discovery value of node
                if (parent.get(node) != null && lowTime.get(adjacent) >= discoveryTime.get(node)) {
                    articulationPoints.add(node);
                }
            }
            // Update low value of node for parent function calls
            else if (!adjacent.equals(parent.get(node))) {
                lowTime.put(node, Math.min(lowTime.get(node), discoveryTime.get(adjacent)));
            }
        }

        // If node is root of DFS tree and has more than one child
        if (parent.get(node) == null && children > 1) {
            articulationPoints.add(node);
        }

        return time;
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
     * Performs a topological sort using Kahn's algorithm (non-recursive).
     *
     * @return List of nodes in topological order
     * @throws IllegalStateException if the graph contains a cycle
     */
    public List<T> topologicalSortKahn() {
        // Copy the graph to avoid modifying the original
        Map<T, Set<T>> inEdges = new HashMap<>();
        for (T node : getAllNodes()) {
            inEdges.put(node, new HashSet<>(getIncomingEdges(node)));
        }

        // Find all nodes with no incoming edges
        Queue<T> queue = new LinkedList<>();
        for (T node : getAllNodes()) {
            if (inEdges.get(node).isEmpty()) {
                queue.add(node);
            }
        }

        // Process nodes
        List<T> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            T node = queue.poll();
            result.add(node);

            // For each node that depends on this node
            for (T outNode : new ArrayList<>(getOutgoingEdges(node))) {
                // Remove the edge
                inEdges.get(outNode).remove(node);

                // If the node has no more incoming edges, add it to the queue
                if (inEdges.get(outNode).isEmpty()) {
                    queue.add(outNode);
                }
            }
        }

        // Check if all edges were processed (i.e., no cycles)
        if (result.size() != getAllNodes().size()) {
            throw new IllegalStateException("Cycle detected in graph, cannot perform topological sort");
        }

        return result;
    }

    /**
     * Finds strongly connected components using Kosaraju's algorithm.
     *
     * @return List of sets of nodes, where each set is a strongly connected component
     */
    public List<Set<T>> findStronglyConnectedComponents() {
        // First pass: fill the stack based on finishing times
        Stack<T> stack = new Stack<>();
        Set<T> visited = new HashSet<>();

        for (T node : getAllNodes()) {
            if (!visited.contains(node)) {
                fillStack(node, visited, stack);
            }
        }

        // Create the transpose of the graph
        DirectedGraph<T> transposed = getTranspose();

        // Reset visited
        visited.clear();

        // Second pass: process nodes in order of finishing times
        List<Set<T>> components = new ArrayList<>();
        while (!stack.isEmpty()) {
            T node = stack.pop();

            if (!visited.contains(node)) {
                Set<T> component = new HashSet<>();
                transposed.collectSCC(node, visited, component);
                components.add(component);
            }
        }

        return components;
    }

    /**
     * Helper method for Kosaraju's algorithm to fill the stack.
     */
    private void fillStack(T node, Set<T> visited, Stack<T> stack) {
        visited.add(node);

        for (T adjacent : getOutgoingEdges(node)) {
            if (!visited.contains(adjacent)) {
                fillStack(adjacent, visited, stack);
            }
        }

        stack.push(node);
    }

    /**
     * Helper method for Kosaraju's algorithm to collect strongly connected components.
     */
    private void collectSCC(T node, Set<T> visited, Set<T> component) {
        visited.add(node);
        component.add(node);

        for (T adjacent : getOutgoingEdges(node)) {
            if (!visited.contains(adjacent)) {
                collectSCC(adjacent, visited, component);
            }
        }
    }

    /**
     * Creates the transpose of the graph (all edges reversed).
     *
     * @return The transposed graph
     */
    public DirectedGraph<T> getTranspose() {
        DirectedGraph<T> transposed = new DirectedGraph<>();

        for (T node : getAllNodes()) {
            transposed.addNode(node);
        }

        for (T node : getAllNodes()) {
            for (T adjacent : getOutgoingEdges(node)) {
                transposed.addEdge(adjacent, node);
            }
        }

        return transposed;
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
     * Finds all cycles in the graph using Johnson's algorithm.
     *
     * @return List of cycles, where each cycle is a list of nodes
     */
    public List<List<T>> findAllCycles() {
        List<List<T>> cycles = new ArrayList<>();
        Set<T> blocked = new HashSet<>();
        Map<T, Set<T>> blockedMap = new HashMap<>();

        // Initialize blocked map
        for (T node : getAllNodes()) {
            blockedMap.put(node, new HashSet<>());
        }

        // Find strongly connected components
        List<Set<T>> sccs = findStronglyConnectedComponents();

        // Process each strongly connected component
        for (Set<T> scc : sccs) {
            if (scc.size() > 1) {
                DirectedGraph<T> subgraph = subgraphWithNodes(scc);

                // For each node in the SCC
                for (T startNode : scc) {
                    // Reset blocked and blockedMap for each start node
                    blocked.clear();
                    for (T node : scc) {
                        blockedMap.put(node, new HashSet<>());
                    }

                    // Find cycles starting from this node
                    findCycles(startNode, startNode, new ArrayList<>(), cycles, blocked, blockedMap, subgraph);
                }
            }
        }

        return cycles;
    }

    /**
     * Helper method for Johnson's algorithm to find cycles.
     */
    private boolean findCycles(T startNode, T currentNode, List<T> path, List<List<T>> cycles,
                               Set<T> blocked, Map<T, Set<T>> blockedMap, DirectedGraph<T> subgraph) {
        boolean foundCycle = false;

        // Add current node to path
        path.add(currentNode);
        blocked.add(currentNode);

        // Check outgoing edges
        for (T neighbor : subgraph.getOutgoingEdges(currentNode)) {
            if (neighbor.equals(startNode)) {
                // Found a cycle
                List<T> cycle = new ArrayList<>(path);
                cycles.add(cycle);
                foundCycle = true;
            } else if (!blocked.contains(neighbor)) {
                if (findCycles(startNode, neighbor, path, cycles, blocked, blockedMap, subgraph)) {
                    foundCycle = true;
                }
            }
        }

        if (foundCycle) {
            unblock(currentNode, blocked, blockedMap);
        } else {
            for (T neighbor : subgraph.getOutgoingEdges(currentNode)) {
                blockedMap.get(neighbor).add(currentNode);
            }
        }

        // Remove current node from path
        path.remove(path.size() - 1);

        return foundCycle;
    }

    /**
     * Helper method for Johnson's algorithm to unblock nodes.
     */
    private void unblock(T node, Set<T> blocked, Map<T, Set<T>> blockedMap) {
        blocked.remove(node);

        Set<T> blockedNodes = blockedMap.get(node);
        while (!blockedNodes.isEmpty()) {
            T blockedNode = blockedNodes.iterator().next();
            blockedNodes.remove(blockedNode);

            if (blocked.contains(blockedNode)) {
                unblock(blockedNode, blocked, blockedMap);
            }
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
     * Gets the transitive closure of multiple nodes.
     *
     * @param nodes The starting nodes
     * @return Set of all nodes reachable from any of the given nodes
     */
    public Set<T> getTransitiveClosure(Collection<T> nodes) {
        Set<T> result = new HashSet<>();

        for (T node : nodes) {
            result.addAll(getTransitiveClosure(node));
        }

        return result;
    }

    /**
     * Gets the reverse transitive closure of a node (all nodes that can reach the given node).
     *
     * @param node The target node
     * @return Set of all nodes that can reach the given node
     */
    public Set<T> getReverseTransitiveClosure(T node) {
        Set<T> result = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(node);

        while (!queue.isEmpty()) {
            T current = queue.poll();
            for (T adj : getIncomingEdges(current)) {
                if (result.add(adj)) {
                    queue.add(adj);
                }
            }
        }

        return result;
    }

    /**
     * Gets the reverse transitive closure of multiple nodes.
     *
     * @param nodes The target nodes
     * @return Set of all nodes that can reach any of the given nodes
     */
    public Set<T> getReverseTransitiveClosure(Collection<T> nodes) {
        Set<T> result = new HashSet<>();

        for (T node : nodes) {
            result.addAll(getReverseTransitiveClosure(node));
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
     * Gets the shortest path from one node to another.
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
     * Finds a minimum spanning tree of the graph using Prim's algorithm.
     *
     * @param weightFunction A function that returns the weight of an edge
     * @return A new graph representing the minimum spanning tree
     */
    public DirectedGraph<T> minimumSpanningTree(EdgeWeightFunction<T> weightFunction) {
        if (getAllNodes().isEmpty()) {
            return new DirectedGraph<>();
        }

        DirectedGraph<T> mst = new DirectedGraph<>();
        Set<T> visited = new HashSet<>();

        // Start with any node
        T start = getAllNodes().iterator().next();
        visited.add(start);
        mst.addNode(start);

        // Create a priority queue of edges
        PriorityQueue<Edge<T>> edgeQueue = new PriorityQueue<>(Comparator.comparingDouble(Edge::getWeight));

        // Add all edges from the start node
        for (T to : getOutgoingEdges(start)) {
            edgeQueue.add(new Edge<>(start, to, weightFunction.getWeight(start, to)));
        }

        // Process edges until all nodes are visited or no more edges
        while (!edgeQueue.isEmpty() && visited.size() < getAllNodes().size()) {
            Edge<T> edge = edgeQueue.poll();

            if (visited.contains(edge.getTo())) {
                continue;
            }

            // Add the edge to MST
            mst.addNode(edge.getFrom());
            mst.addNode(edge.getTo());
            mst.addEdge(edge.getFrom(), edge.getTo());

            // Mark the node as visited
            visited.add(edge.getTo());

            // Add all edges from the new node
            for (T to : getOutgoingEdges(edge.getTo())) {
                if (!visited.contains(to)) {
                    edgeQueue.add(new Edge<>(edge.getTo(), to, weightFunction.getWeight(edge.getTo(), to)));
                }
            }
        }

        return mst;
    }

    /**
     * Finds the shortest paths from a source node to all other nodes using Dijkstra's algorithm.
     *
     * @param source The source node
     * @param weightFunction A function that returns the weight of an edge
     * @return A map of node to the distance from the source node
     */
    public Map<T, Double> shortestPathsFromSource(T source, EdgeWeightFunction<T> weightFunction) {
        Map<T, Double> distances = new HashMap<>();
        Set<T> visited = new HashSet<>();
        PriorityQueue<NodeDistance<T>> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::getDistance));

        // Initialize distances
        for (T node : getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }

        // Distance to source is 0
        distances.put(source, 0.0);
        queue.add(new NodeDistance<>(source, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance<T> current = queue.poll();

            // Skip if already visited
            if (visited.contains(current.getNode())) {
                continue;
            }

            visited.add(current.getNode());

            // Explore neighbors
            for (T neighbor : getOutgoingEdges(current.getNode())) {
                if (visited.contains(neighbor)) {
                    continue;
                }

                double weight = weightFunction.getWeight(current.getNode(), neighbor);
                double distance = current.getDistance() + weight;

                if (distance < distances.get(neighbor)) {
                    distances.put(neighbor, distance);
                    queue.add(new NodeDistance<>(neighbor, distance));
                }
            }
        }

        return distances;
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

    /**
     * Class representing an edge in the graph.
     */
    private static class Edge<T> {
        private final T from;
        private final T to;
        private final double weight;

        public Edge(T from, T to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public T getFrom() {
            return from;
        }

        public T getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }
    }

    /**
     * Class representing a node with its distance from the source.
     */
    private static class NodeDistance<T> {
        private final T node;
        private final double distance;

        public NodeDistance(T node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public T getNode() {
            return node;
        }

        public double getDistance() {
            return distance;
        }
    }

    /**
     * Interface for providing edge weights.
     */
    public interface EdgeWeightFunction<T> {
        double getWeight(T from, T to);
    }
}