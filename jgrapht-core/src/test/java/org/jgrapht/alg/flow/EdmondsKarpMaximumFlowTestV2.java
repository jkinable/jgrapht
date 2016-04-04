/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
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
/* -----------------
 * EdmondsKarpMaximumFlowTest.java
 * -----------------
 * (C) Copyright 2008-2008, by Ilya Razenshteyn and Contributors.
 *
 * Original Author:  Ilya Razenshteyn
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 */
package org.jgrapht.alg.flow;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MaximumFlowAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.Map;


public class EdmondsKarpMaximumFlowTestV2 extends MaximumFlowAlgorithmTestBase
{
    @Override
    MaximumFlowAlgorithm<Integer, DefaultWeightedEdge> createSolver(DirectedGraph<Integer, DefaultWeightedEdge> network) {
        return new EdmondsKarpMaximumFlowV2<>(network);
    }

    //~ Methods ----------------------------------------------------------------

    public void testUndirectedGraph(){
        //Undirected graph
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph=new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for(int i=0; i<9; i++)
            graph.addVertex(i);
        Graphs.addEdge(graph, 0, 1, 12);
        Graphs.addEdge(graph, 0, 2, 15);
        Graphs.addEdge(graph, 0, 3, 20);
        Graphs.addEdge(graph, 1, 5, 5);
        Graphs.addEdge(graph, 1, 6, 2);
        Graphs.addEdge(graph, 1, 2, 5);
        Graphs.addEdge(graph, 2, 6, 6);
        Graphs.addEdge(graph, 2, 4, 3);
        Graphs.addEdge(graph, 2, 3, 11);
        Graphs.addEdge(graph, 3, 4, 4);
        Graphs.addEdge(graph, 3, 7, 8);
        Graphs.addEdge(graph, 4, 6, 6);
        Graphs.addEdge(graph, 4, 7, 1);
        Graphs.addEdge(graph, 5, 6, 9);
        Graphs.addEdge(graph, 5, 8, 18);
        Graphs.addEdge(graph, 6, 7, 7);
        Graphs.addEdge(graph, 6, 8, 13);
        Graphs.addEdge(graph, 7, 8, 10);
        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> maxFlowAlgorithm=new EdmondsKarpMaximumFlowV2<>(graph);
        MaximumFlowAlgorithm.MaximumFlow<Integer, DefaultWeightedEdge> maxFlow=maxFlowAlgorithm.buildMaximumFlow(0, 8);
        //Verify that the maximum flow value
        assertEquals(
                28,
                maxFlow.getValue(),
                EdmondsKarpMaximumFlow.DEFAULT_EPSILON);
        //Verify that the flow on every arc is between [-DEFAULT_EPSILON, edge_capacity]
        for (DefaultWeightedEdge e : maxFlow.getFlow().keySet()) {
            assertTrue(graph.containsEdge(e));
            assertTrue(maxFlow.getFlow().get(e) >= -EdmondsKarpMaximumFlow.DEFAULT_EPSILON);
            assertTrue(maxFlow.getFlow().get(e)<= (graph.getEdgeWeight(e)+ EdmondsKarpMaximumFlow.DEFAULT_EPSILON));
        }
    }

    public void testCornerCases()
    {
        DirectedWeightedMultigraph<Integer, DefaultWeightedEdge> simple =
            new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
        simple.addVertex(0);
        simple.addVertex(1);
        DefaultWeightedEdge e = simple.addEdge(0, 1);
        try {
            new EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge>(null);
            fail();
        } catch (NullPointerException ex) {
        }
        try {
            new EdmondsKarpMaximumFlow<>(
                simple,
                -0.1);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            simple.setEdgeWeight(e, -1.0);
            new EdmondsKarpMaximumFlow<>(simple);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            simple.setEdgeWeight(e, 1.0);
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new EdmondsKarpMaximumFlow<>(
                    simple);
            Map<DefaultWeightedEdge, Double> flow = solver.buildMaximumFlow(0, 1).getFlow();
            flow.put(e, 25.0);
            fail();
        } catch (UnsupportedOperationException ex) {
        }
        try {
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new EdmondsKarpMaximumFlow<>(
                    simple);
            solver.buildMaximumFlow(2, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                    new EdmondsKarpMaximumFlow<>(
                            simple);
            solver.buildMaximumFlow(1, 2);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                    new EdmondsKarpMaximumFlow<>(
                            simple);
            solver.buildMaximumFlow(0, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                    new EdmondsKarpMaximumFlow<>(
                            simple);
            solver.buildMaximumFlow(null, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver =
                    new EdmondsKarpMaximumFlow<>(
                            simple);
            solver.buildMaximumFlow(0, null);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }
}

// End EdmondsKarpMaximumFlowTest.java
