/*
 * (C) Copyright 2007-2017, by France Telecom and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.connectivity;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;

/**
 * A Block-Cutpoint graph (also known as a block-cut tree).
 * If $G$ is a graph, the block-cutpoint graph of $G$, denoted $BC(G)$ is the simple bipartite graph with
 * bipartition $(A, B)$ where $A$ is the set of <a href="http://mathworld.wolfram.com/ArticulationVertex.html">cut-vertices</a> (also known as articulation points) of $G$,
 * and $B$ is the set of <a href="http://mathworld.wolfram.com/Block.html">blocks</a> of $G$. $BC(G)$ contains an edge $(a,b)$ for $a \in A$ and $b \in B$ if and only if
 * block $b$ contains the cut-vertex $a$.
 * A vertex in $G$ is a cut-vertex if removal of the vertex from $G$ (and all edges incident to this vertex) increases
 * the number of connected components in the graph. A block of $G$ is a maximal connected subgraph $H \subseteq G$ so
 * that $H$ does not have a cut-vertex. Note that if $H$ is a block, then either $H$ is 2-connected, or $|V(H)| \leq 2$.
 * Each pair of blocks of $G$ share at most one vertex, and that vertex is a cut-point in $G$. $BC(G)$ is a tree in which
 * each leaf node corresponds to a block of $G$.
 * <p>
 * This class also computes the <a href="https://en.wikipedia.org/wiki/Bridge_(graph_theory)">bridges</a> (also know as cut-edges) in a graph.
 * A bridge is an edge of a graph whose deletion increases its number of connected components.
 * <p>
 * The algorithm implemented in this class to compute the BLock-Cutpoint graph is described in:
 * Hopcroft, J. Tarjan, R. Algorithm 447: efficient algorithms for graph manipulation, 1973. Communications of the ACM. 16 (6): 372–378.
 * This implementation runs in linear time $O(|V|+|E|)$ and is based on Depth-first search.
 * More information about this subject be be found in this wikipedia <a href="https://en.wikipedia.org/wiki/Biconnected_component">article</a>.
 *
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author France Telecom S.A
 * @author Joris Kinable
 * @since July 5, 2007
 */
public class BlockCutpointGraph<V, E>
        extends SimpleGraph<Graph<V, E>, DefaultEdge>
{
    private static final long serialVersionUID = -9101341117013163934L;

    private Set<V> cutpoints = new HashSet<>();

    private Set<E> bridges = new HashSet<>();

    private Graph<V, E> graph;

    /* Discovery time of a vertex. */
    private int time;

    /* Stack which keeps track of edges in biconnected components */
    private Deque<E> stack;

    /* Mapping of a vertex to all biconnected components it belongs to */
    private Map<V, Set<Graph<V, E>>> vertex2biconnectedSubgraphs = new HashMap<>();

    /* Mapping of a non cutvertex to the unique block it belongs to */
    private Map<V, Graph<V, E>> vertex2block = new HashMap<>();

    /* Map which tracks when a vertex is discovered in the DFS search */
    private Map<V, Integer> vertex2time = new HashMap<>();

    /**
     * Creates a block-cut point graph for the given input graph.
     * Running time = O(m) where m is the number of edges.
     *
     * @param graph the input graph
     */
    public BlockCutpointGraph(Graph<V, E> graph)
    {
        super(DefaultEdge.class);
        GraphTests.requireDirectedOrUndirected(graph);
        if (graph.getType().isDirected()) {
            this.graph = new AsUndirectedGraph<>(graph);
        } else
            this.graph = graph;

        if(this.graph.getType().isAllowingMultipleEdges())
            throw new IllegalArgumentException("This implementation currently does not support multigraphs");

        this.graph = GraphTests.requireUndirected(graph, "Graph must be undirected");
        stack = new ArrayDeque<>(graph.edgeSet().size());

        V s = graph.vertexSet().iterator().next(); //Pick random start vertex
        dfsVisit(s, null);

        if(!stack.isEmpty())
            biconnectedComponentFinished(s);

        //Construct the Block-cut point graph
        for (V cutpoint : this.cutpoints) {
            Graph<V,E> subgraph=new AsSubgraph<>(graph, Collections.singleton(cutpoint));
            this.vertex2block.put(cutpoint, subgraph);
            this.addVertex(subgraph);
            for (Graph<V, E> biconnectedSubgraph : getBiconnectedSubgraphs(cutpoint))
                addEdge(subgraph, biconnectedSubgraph);
        }
    }

    /**
     * Returns the vertex if vertex is a cutpoint, and otherwise returns the block (biconnected
     * component) containing the vertex.
     *
     * @param vertex vertex in the initial graph.
     * @return the biconnected component containing the vertex
     */
    public Graph<V, E> getBlock(V vertex)
    {
        if (!this.graph.vertexSet().contains(vertex)) {
            throw new IllegalArgumentException("No such vertex in the graph!");
        }

        return this.vertex2block.get(vertex);
    }

    /**
     * Returns the cutpoints of the initial graph.
     *
     * @return the cutpoints of the initial graph
     */
    public Set<V> getCutpoints()
    {
        return this.cutpoints;
    }

    /**
     * Returns the bridges of the initial graph.
     *
     * @return the bridges of the initial graph
     */
    public Set<E> getBridges()
    {
        return this.bridges;
    }

    /**
     * Returns <code>true</code> if the vertex is a cutpoint, <code>false</code> otherwise.
     *
     * @param vertex vertex in the initial graph.
     * @return <code>true</code> if the vertex is a cutpoint, <code>false</code> otherwise.
     */
    public boolean isCutpoint(V vertex)
    {
        return this.cutpoints.contains(vertex);
    }

    private void biconnectedComponentFinished(V n)
    {
        Set<V> vertexComponent = new HashSet<>();

        E edge;
        do{
            edge=this.stack.pop();
            vertexComponent.add(graph.getEdgeSource(edge));
            vertexComponent.add(graph.getEdgeTarget(edge));
        }while (!this.stack.isEmpty() && getTime(graph.getEdgeSource(edge)) >= getTime(n));

        Graph<V,E> biconnectedSubgraph=new AsSubgraph<>(this.graph, vertexComponent);
        for (V vertex : vertexComponent) {
            this.vertex2block.put(vertex, biconnectedSubgraph);
            getBiconnectedSubgraphs(vertex).add(biconnectedSubgraph); //mapping of v to all biconnected subgraphs it is in
        }
        addVertex(biconnectedSubgraph);
    }

    private int dfsVisit(V s, V parent)
    {
        int minS = ++this.time;
        vertex2time.put(s, time);
        int children=0;

        for (E edge : this.graph.edgesOf(s)) {
            V n = Graphs.getOppositeVertex(this.graph, edge, s);
            if (getTime(n) == -1) { //Node hasn't been discovered yet
                children++;

                this.stack.push(edge);

                int minN = dfsVisit(n, s);
                minS = Math.min(minN, minS);

                if (minN > getTime(s))
                    bridges.add(edge);

                if ((parent != null && minN >= getTime(s)) || (parent == null && children > 1)) {
                    this.cutpoints.add(s); //s is a cutpoint
                    biconnectedComponentFinished(n);
                }
            } else if ((getTime(n) < getTime(s)) && !n.equals(parent)) {
                this.stack.push(edge);
                minS = Math.min(getTime(n), minS);
            }
        }
        return minS;
    }

    /**
     * Returns the biconnected components containing the vertex. A vertex which is not a cutpoint is
     * contained in exactly one component. A cutpoint is contained is at least 2 components.
     *
     * @param vertex vertex in the initial graph.
     */
    private Set<Graph<V, E>> getBiconnectedSubgraphs(V vertex)
    {
        return this.vertex2biconnectedSubgraphs.computeIfAbsent(vertex, k -> new HashSet<>());
    }

    /**
     * Returns the traverse order of the vertex in the DFS.
     */
    private int getTime(V vertex)
    {
        assert (vertex != null);
        return this.vertex2time.getOrDefault(vertex, -1);
    }
}

// End BlockCutpointGraph.java