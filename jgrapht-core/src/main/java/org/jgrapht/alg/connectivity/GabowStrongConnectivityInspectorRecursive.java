package org.jgrapht.alg.connectivity;

import org.jgrapht.*;
import org.jgrapht.util.*;

import java.util.*;

public class GabowStrongConnectivityInspectorRecursive<V,E> extends AbstractStrongConnectivityInspector<V, E>{
    // stores the vertices
    private Deque<VertexNumber<V>> stack = new ArrayDeque<>();

    // maps vertices to their VertexNumber object
    private Map<V, VertexNumber<V>> vertexToVertexNumber;

    // store the numbers
    private Deque<Integer> B = new ArrayDeque<>();

    // number of vertices
    private int c;

    /**
     * Constructor
     *
     * @param graph the graph to inspect
     * @throws NullPointerException in case the graph is null
     */
    public GabowStrongConnectivityInspectorRecursive(Graph<V, E> graph)
    {
        super(graph);
    }

    @Override
    public List<Set<V>> stronglyConnectedSets()
    {
        if (stronglyConnectedSets == null) {
            stronglyConnectedSets = new Vector<>();

            // create VertexData objects for all vertices, store them
            createVertexNumber();

            // perform DFS
            for (VertexNumber<V> data : vertexToVertexNumber.values()) {
                if (data.getNumber() == 0) {
                    dfsVisit(graph, data);
                }
            }

            vertexToVertexNumber = null;
            stack = null;
            B = null;
        }

        return stronglyConnectedSets;
    }

    /*
     * Creates a VertexNumber object for every vertex in the graph and stores them in a HashMap.
     */

    private void createVertexNumber()
    {
        c = graph.vertexSet().size();
        vertexToVertexNumber = CollectionUtil.newHashMapWithExpectedSize(c);

        for (V vertex : graph.vertexSet()) {
            vertexToVertexNumber.put(vertex, new VertexNumber<>(vertex, 0));
        }

        stack = new ArrayDeque<>(c);
        B = new ArrayDeque<>(c);
    }

    /*
     * The subroutine of DFS.
     */
    private void dfsVisit(Graph<V, E> visitedGraph, VertexNumber<V> v)
    {
        VertexNumber<V> w;
        stack.add(v);
        B.add(v.setNumber(stack.size() - 1));

        // follow all edges

        for (E edge : visitedGraph.outgoingEdgesOf(v.getVertex())) {
            w = vertexToVertexNumber.get(visitedGraph.getEdgeTarget(edge));

            if (w.getNumber() == 0) {
                dfsVisit(graph, w);
            } else { /* contract if necessary */
                while (w.getNumber() < B.getLast()) {
                    B.removeLast();
                }
            }
        }
        Set<V> L = new HashSet<>();
        if (v.getNumber() == (B.getLast())) {
            /*
             * number vertices of the next strong component
             */
            B.removeLast();

            c++;
            while (v.getNumber() <= (stack.size() - 1)) {
                VertexNumber<V> r = stack.removeLast();
                L.add(r.getVertex());
                r.setNumber(c);
            }
            stronglyConnectedSets.add(L);
        }
    }

    private static final class VertexNumber<V>
    {
        V vertex;
        int number;

        private VertexNumber(V vertex, int number)
        {
            this.vertex = vertex;
            this.number = number;
        }

        int getNumber()
        {
            return number;
        }

        V getVertex()
        {
            return vertex;
        }

        Integer setNumber(int n)
        {
            return number = n;
        }
    }
}
