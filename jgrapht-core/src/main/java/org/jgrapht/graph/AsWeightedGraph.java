/*
 * (C) Copyright 2018-2018, by Lukas Harzenetter and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.graph;

import java.io.Serializable;
import java.util.*;
import java.util.function.*;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.GraphType;

/**
 * Provides a weighted view on a graph.
 *
 * Algorithms designed for weighted graphs should also work on unweighted graphs. This class
 * emulates a weighted graph based on a backing one by handling the storage of edge weights
 * internally and passing all other operations on the underlying graph. As a consequence, the edges
 * returned are the edges of the original graph.
 *
 * Additionally, if the underlying graph is weighted, weight changes can be
 * propagated to it. By default, this happens automatically when the backing
 * graph is weighted; this behavior can be disabled via an optional constructor
 * parameter.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class AsWeightedGraph<V, E>
    extends GraphDelegator<V, E>
    implements Serializable, Graph<V, E>
{

    private static final long serialVersionUID = -6838132233557L;
    private final Function<E, Double> weightFunction;
    private final Map<E, Double> weights;
    public final boolean writeWeightsThrough;
    public final boolean cacheWeights;

    /**
     * Constructor for AsWeightedGraph which enables weight write propagation
     * automatically if the backing graph is weighted (otherwise, weight changes
     * only affect the weighted view).
     *
     * @param graph   the backing graph over which an weighted view is to be created.
     * @param weights the map containing the edge weights.
     * @throws NullPointerException if the graph or the weights are null.
     */
    public AsWeightedGraph(Graph<V, E> graph, Map<E, Double> weights)
    {
        this(graph, weights, graph.getType().isWeighted());
    }

    /**
     * Constructor for AsWeightedGraph which allows weight write propagation
     * to be requested explicitly.
     *
     * @param graph               the backing graph over which an weighted view is to be created
     * @param weights             the map containing the edge weights
     * @param writeWeightsThrough if set to true, the weights will get propagated to the backing
     *                            graph in the <code>setEdgeWeight()</code> method.
     * @throws NullPointerException     if the graph or the weights are null
     * @throws IllegalArgumentException if <code>writeWeightsThrough</code> is set to true and
     *                                  <code>graph</code> is not a weighted graph
     */
    public AsWeightedGraph(Graph<V, E> graph, Map<E, Double> weights, boolean writeWeightsThrough)
    {
        super(graph);
        this.weights = Objects.requireNonNull(weights);
        this.weightFunction=null;
        this.cacheWeights=false;
        this.writeWeightsThrough = writeWeightsThrough;

        if (this.writeWeightsThrough)
            GraphTests.requireWeighted(graph);
    }

    /**
     * Constructor for AsWeightedGraph which uses a weight function to compute edge weights. When the weight of an edge
     * is queried, the weight function is invoked. If <code>cacheWeights</code> is set to <code>true</code>, the weight
     * of an edge returned by the <code>weightFunction</code> after its first invocation is stored in a map. The weight of
     * an edge returned by subsequent calls to @link{getEdgeWeight} for the same edge will then be
     * retrieved directly from the map, instead of re-invoking the weight function. If <code>cacheWeights</code> is set
     * to <code>false</code>, each invocation of the @link{getEdgeWeight} method will invoke the weight function.
     * Caching the edge weights is particularly useful when pre-computing all edge weights is expensive and it is
     * expected that the weights of only a subset of all edges will be queried.
     *
     * @param graph the backing graph over which an weighted view is to be created
     * @param weightFunction function which maps an edge to a weight
     * @param cacheWeights if set to <code>true</code>, weights are cached once computed by the weight function
     * @param writeWeightsThrough if set to <code>true</code>, the weight set directly by the @link{setEdgeWeight} method will be propagated to the backing graph.
     * @throws NullPointerException     if the graph or the weight function is null
     * @throws IllegalArgumentException if <code>writeWeightsThrough</code> is set to true and <code>graph</code> is not a weighted graph
     */
    public AsWeightedGraph(Graph<V,E> graph, Function<E, Double> weightFunction, boolean cacheWeights, boolean writeWeightsThrough){
        super(graph);
        this.weightFunction=Objects.requireNonNull(weightFunction);
        this.cacheWeights=cacheWeights;
        this.writeWeightsThrough=writeWeightsThrough;
        this.weights=new HashMap<>();

        if (this.writeWeightsThrough)
            GraphTests.requireWeighted(graph);
    }

    /**
     * Returns the weight assigned to a given edge.
     * If there is no edge weight set for the given edge, the value of the backing graph's
     * getEdgeWeight method is returned.
     *
     * @param e edge of interest
     * @return the edge weight
     * @throws NullPointerException if the edge is null
     */
    @Override public double getEdgeWeight(E e)
    {
        Double weight;
        if(weightFunction != null) {
            if(cacheWeights) //If weights are cached, check map first before invoking the weight function
                weight = weights.computeIfAbsent(e, weightFunction);
            else
                weight = weightFunction.apply(e);
        }else{
            weight=weights.get(e);
        }

        if (Objects.isNull(weight))
            weight = super.getEdgeWeight(e);

        return weight;
    }

    /**
     * Assigns a weight to an edge. If <code>writeWeightsThrough</code> is set to <code>true</code>, the same weight is
     * set in the backing graph. If this class was constructed using a weight function, it only makes sense to invoke this
     * method when <code>cacheWeights</code> is set to true. This method can than be used to preset weights in the cache, or
     * to overwrite existing values.
     *
     * @param e      edge on which to set weight
     * @param weight new weight for edge
     * @throws NullPointerException if the edge is null
     */
    @Override public void setEdgeWeight(E e, double weight)
    {
        this.weights.put(Objects.requireNonNull(e), weight);

        if (this.writeWeightsThrough) {
            this.getDelegate().setEdgeWeight(e, weight);
        }
    }

    @Override public GraphType getType()
    {
        return super.getType().asWeighted();
    }

}
