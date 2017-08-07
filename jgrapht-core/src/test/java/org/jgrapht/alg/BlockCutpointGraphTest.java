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
package org.jgrapht.alg;

import org.jgrapht.*;
import org.jgrapht.alg.connectivity.BlockCutpointGraph;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since July 5, 2007
 */
public class BlockCutpointGraphTest
{
    // ~ Methods ----------------------------------------------------------------

    @Test
    public void testBiconnected()
    {
        BiconnectedGraph graph = new BiconnectedGraph();

        BlockCutpointGraph<String, DefaultEdge> blockCutpointGraph =
            new BlockCutpointGraph<>(graph);
        testGetBlock(blockCutpointGraph);

        assertEquals(0, blockCutpointGraph.getCutpoints().size());
        int nbBiconnectedComponents =
            blockCutpointGraph.vertexSet().size() - blockCutpointGraph.getCutpoints().size();
        assertEquals(1, nbBiconnectedComponents);
    }

    public <V> void testGetBlock(BlockCutpointGraph<V, DefaultEdge> blockCutpointGraph)
    {
        for (Graph<V, DefaultEdge> component : blockCutpointGraph.vertexSet()) {
            if (!component.edgeSet().isEmpty()) {
                for (V vertex : component.vertexSet()) {
                    if (!blockCutpointGraph.getCutpoints().contains(vertex)) {
                        assertEquals(component, blockCutpointGraph.getBlock(vertex));
                    }
                }
            } else {
                assertTrue(
                    blockCutpointGraph
                        .getCutpoints().contains(component.vertexSet().iterator().next()));
            }
        }
    }

    @Test
    public void testLinearGraph()
    {
        testLinearGraph(3);
        testLinearGraph(5);
    }

    public void testLinearGraph(int nbVertices)
    {
        Graph<Object, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        LinearGraphGenerator<Object, DefaultEdge> generator =
            new LinearGraphGenerator<>(nbVertices);
        generator.generateGraph(graph, new ClassBasedVertexFactory<>(Object.class), null);

        BlockCutpointGraph<Object, DefaultEdge> blockCutpointGraph =
            new BlockCutpointGraph<>(graph);
        testGetBlock(blockCutpointGraph);

        assertEquals(nbVertices - 2, blockCutpointGraph.getCutpoints().size());
        int nbBiconnectedComponents =
            blockCutpointGraph.vertexSet().size() - blockCutpointGraph.getCutpoints().size();
        assertEquals(nbVertices - 1, nbBiconnectedComponents);
    }

    @Test
    public void testNotBiconnected()
    {
        Graph<String, DefaultEdge> graph = new NotBiconnectedGraph();

        BlockCutpointGraph<String, DefaultEdge> blockCutpointGraph =
            new BlockCutpointGraph<>(graph);
        testGetBlock(blockCutpointGraph);

        assertEquals(2, blockCutpointGraph.getCutpoints().size());
        int nbBiconnectedComponents =
            blockCutpointGraph.vertexSet().size() - blockCutpointGraph.getCutpoints().size();
        assertEquals(3, nbBiconnectedComponents);
    }

    @Test
    public void testWikiGraph(){
        Graph<Integer, DefaultEdge> g=new SimpleGraph<>(DefaultEdge.class);
        Graphs.addAllVertices(g, Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,13,14));
        int[][] edges={{1,3},{1,2},{2,4},{3,4},{4,5},{5,6},{6,7},{7,8},{7,9},{9,10},{9,11},{11,12},{12,13},{13,14},{12,14},{7,14}};
        for(int[] edge : edges)
            g.addEdge(edge[0],edge[1]);
        BlockCutpointGraph<Integer, DefaultEdge> bcg = new BlockCutpointGraph<>(g);

        Set<Integer> expectedCutpoints=new HashSet<>(Arrays.asList(4,5,6,7,9));
        assertEquals(expectedCutpoints, bcg.getCutpoints());

        Set<DefaultEdge> expectedBridges = new HashSet<>();
        expectedBridges.add(g.getEdge(4,5));
        expectedBridges.add(g.getEdge(5,6));
        expectedBridges.add(g.getEdge(6,7));
        expectedBridges.add(g.getEdge(7,8));
        expectedBridges.add(g.getEdge(9,10));
        assertEquals(expectedBridges, bcg.getBridges());

        for(int v : Arrays.asList(1,2,3))
            assertEquals(bcg.getBlock(v), new AsSubgraph<>(g,new HashSet<>(Arrays.asList(1,2,3,4))));
        assertEquals(new AsSubgraph<>(g,new HashSet<>(Arrays.asList(7,8))), bcg.getBlock(8));
        for(int v : Arrays.asList(11,12,13,14))
            assertEquals(new AsSubgraph<>(g,new HashSet<>(Arrays.asList(6,7,9,11,12,13,14))), bcg.getBlock(v));
        for(int v : bcg.getCutpoints())
            assertEquals(new AsSubgraph<>(g,new HashSet<>(Arrays.asList(v))), bcg.getBlock(v));

//        for(int v : g.vertexSet())
//            System.out.println("block("+v+"): "+bcg.getBlock(v));

//        System.out.println("\n"+bcg);

        //Test block-cut graph structure

    }
}

// End BlockCutpointGraphTest.java
