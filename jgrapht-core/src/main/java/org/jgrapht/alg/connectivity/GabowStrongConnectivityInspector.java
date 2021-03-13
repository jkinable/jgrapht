/*
 * (C) Copyright 2021-2021, by Joris Kinable and Contributors.
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
package org.jgrapht.alg.connectivity;

import org.jgrapht.*;

import java.util.*;

/**
 * Computes the strongly connected components of a directed graph using Gabow's algorithm. The algorithm is described in:
 * Gabow, Harold N. Path-based depth-first search for strong and biconnected components, Information Processing Letters, 74 (3–4): 107–114, 2000
 * The runtime complexity of this algorithm is $O(|V|+|E|)$.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Joris Kinable
 */
public class GabowStrongConnectivityInspector<V, E>
    extends
    AbstractStrongConnectivityInspector<V, E>
{

    private Set<V> visited;        // Set of vertices which have been visited
    private Set<V> assigned; // Set of vertices which have been assigned to a strongly connected component
    private Map<V,Integer> preorder;          // Maps a vertex to its pre-order traversal number
    private int pre;                 // preorder number counter
    private Stack<V> stackS; // Stack S contains all the vertices that have not yet been assigned to a strongly connected component
    private Stack<V> stackB; // Stack P contains vertices that have not yet been determined to belong to different strongly connected components from each other

    /**
     * Constructor
     *
     * @param graph the graph to inspect
     * @throws NullPointerException in case the graph is null
     */
    public GabowStrongConnectivityInspector(Graph<V, E> graph)
    {
        super(graph);
    }

    @Override
    public List<Set<V>> stronglyConnectedSets()
    {
        if (stronglyConnectedSets == null) {
            stronglyConnectedSets = new LinkedList<>();

            //Initialize data structures
            visited = new HashSet<>();
            assigned = new HashSet<>();
            stackS = new Stack<V>();
            stackB = new Stack<V>();
            preorder = new HashMap<>();
            graph.vertexSet().forEach(v -> preorder.put(v,0));

            // perform DFS
            for (V v : graph.vertexSet()) {
                if (!visited.contains(v)) dfs(v);
            }

            //Return memory
            visited=null;
            assigned=null;
            preorder=null;
        }

        return stronglyConnectedSets;
    }

    private void dfs(V root) {

        //Iterative DFS
        Stack<V> dfsStack=new Stack<>();
        dfsStack.push(root);
        visited.add(root);
        preorder.put(root,pre++);
        stackS.push(root);
        stackB.push(root);

        while(!dfsStack.isEmpty()){
            V v = dfsStack.peek(); // Peek

            boolean visitedAllNeighbors = true;
            // Determine whether there are any unvisited neighbors of v. Seems quite expensive, we might want to track
            // unvisited neighbors instead of looping over all neighbors each time we see v on the stack
            for (E e : graph.outgoingEdgesOf(v)) {
                V w = Graphs.getOppositeVertex(graph, e, v);
                if (!visited.contains(w)) {
                    visitedAllNeighbors = false;         // vertex v is not the tail of our DFS tree since we haven't visited w yet.
                    dfsStack.push(w);
                    visited.add(w);         //Pre-order phase
                    preorder.put(w,pre++);
                    stackS.push(w);
                    stackB.push(w);
                    break;
                } else if (!assigned.contains(w)){
                    while (preorder.get(stackB.peek()) > preorder.get(w))
                        stackB.pop();
                }
            }
            // We visited all neighbors of v. Perform post-order phase.
            if(visitedAllNeighbors){
                dfsStack.pop();
                if (stackB.peek() == v) {
                    Set<V> component = new HashSet<>();
                    stackB.pop();
                    V w;
                    do {
                        w = stackS.pop();
                        component.add(w);
                    } while (w != v);
                    this.stronglyConnectedSets.add(component);
                    assigned.addAll(component);
                }
            }
        }

    }

//    private void dfs(V v) {
//        visited.add(v);
//        preorder.put(v,pre++);
//        stack1.push(v);
//        stack2.push(v);
//        for (E e : graph.outgoingEdgesOf(v)) {
//            V w = Graphs.getOppositeVertex(graph, e, v);
//            if (!visited.contains(w)) dfs(w);
//            else if (!assigned.contains(w)){
//                while (preorder.get(stack2.peek()) > preorder.get(w))
//                    stack2.pop();
//            }
//        }
//
//        // found strong component containing v
//        if (stack2.peek() == v) {
//            Set<V> component = new HashSet<>();
//            stack2.pop();
//            V w;
//            do {
//                w = stack1.pop();
//                component.add(w);
//            } while (w != v);
//            this.stronglyConnectedSets.add(component);
//            assigned.addAll(component);
//        }
//    }
}
