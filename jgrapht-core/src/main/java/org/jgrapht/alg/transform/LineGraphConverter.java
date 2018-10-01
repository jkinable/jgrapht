/*
 * (C) Copyright 2018-2018, by Nikhil Sharma and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are transform-licensed under
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
package org.jgrapht.alg.transform;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;

import java.util.Objects;
import java.util.function.*;

/**
 * Generator which produces the
 * <a href="http://mathworld.wolfram.com/LineGraph.html">line graph</a> of a given input
 * graph.
 * The line graph of an undirected graph $G$ is another graph $L(G)$ that represents the
 * adjacencies between edges of $G$.
 * The line graph of a directed graph $G$ is the directed graph $L(G)$ whose vertex set
 * corresponds to the arc set of $G$ and having an arc directed from an edge $e_1$ to an
 * edge $e_2$ if in $G$, the head of $e_1$ meets the tail of $e_2$
 *
 * <p>
 * More formally, let $G = (V, E)$ be a graph then its line graph $L(G)$ is such that
 * <ul>
 * <li>Each vertex of $L(G)$ represents an edge of $G$</li>
 * <li>Two vertices of $L(G)$ are adjacent if and only if their corresponding edges share
 * a common endpoint ("are incident") in $G$ </li>
 * </ul>
 * <p>
 *
 * @author Joris Kinable
 * @author Nikhil Sharma
 *
 *
 * @param <V> vertex type of input graph
 * @param <E> edge type of input graph
 * @param <EE> edge type of target graph
 *
 */
public class LineGraphConverter<V, E, EE>
{

    private final Graph<V, E> graph;

    /**
     * Line Graph Converter
     *
     * @param graph graph to be converted
     */
    public LineGraphConverter(Graph<V, E> graph)
    {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
    }

    /**
     * Constructs a line graph $L(G)$ of the input graph $G(V,E)$. If the input graph is directed, the result is a line digraph.
     * The result is stored in the target graph.
     *
     * @param target target graph
     */
    public void convertToLineGraph(final Graph<E, EE> target)
    {
        this.convertToLineGraph(target, null);
    }

    /**
     * Constructs a line graph of the input graph. If the input graph is directed, the result is a line digraph.
     * The result is stored in the target graph. A weight function is provided to set edge weights of the line graph
     * edges. Notice that the target graph must be a weighted graph for this to work. Recall that in a line graph
     * $L(G)$ of a graph $G(V,E)$ there exists an edge $e$ between $e1\in E$ and $e2\in E$ if the head of $e1$ is incendent
     * to the tail of $e2$. To determine the weight of $e$ in $L(G)$, the weight function takes as input $e1$ and $e2$.
     *
     * @param target target graph
     * @param weightFunction weight function
     */
    public void convertToLineGraph(final Graph<E, EE> target, final BiFunction<E, E, Double> weightFunction)
    {
        Graphs.addAllVertices(target, graph.edgeSet());
        if (graph.getType().isDirected()) {
            for(V vertex : graph.vertexSet()) {
                for (E e1 : graph.incomingEdgesOf(vertex)) {
                    for (E e2 : graph.outgoingEdgesOf(vertex)) {
                        EE edge = target.addEdge(e1, e2);
                        if (weightFunction != null)
                            target.setEdgeWeight(edge, weightFunction.apply(e1, e2));
                    }
                }
            }
        } else{ // undirected graph
            for(V v : graph.vertexSet()) {
                for (E e1 : graph.edgesOf(v)) {
                    for (E e2 : graph.edgesOf(v)) {
                        if (e1 != e2) {
                            EE edge = target.addEdge(e1, e2);
                            if (weightFunction != null)
                                target.setEdgeWeight(edge, weightFunction.apply(e1, e2));
                        }
                    }
                }
            }

        }
    }

//    To implement getRootGraph and isLineGraph, see:
//    -Lehot, P. G. H. "An Optimal Algorithm to Detect a Line Graph and Output Its Root Graph." J. ACM 21, 569-575, 1974.
//    -Sysło, Maciej M. (1982), "A labeling algorithm to recognize a line digraph and output its root graph", Information Processing Letters, 15 (1): 28–30, doi:10.1016/0020-0190(82)90080-1, MR 0678028.
//    -Degiorgi D.G., Simon K. (1995) A dynamic algorithm for line graph recognition. In: Nagl M. (eds) Graph-Theoretic Concepts in Computer Science. WG 1995. Lecture Notes in Computer Science, vol 1017. Springer, Berlin, Heidelberg
//    public void convertToRootGraph(Graph<V, E> target){
//        //Not yet implemented
//    }
//
//    Determines whether the graph is a line graph
//    public boolean isLineGraph(){
//        //Not yet implemented. Add shortcut to GraphTest
//    }
}
