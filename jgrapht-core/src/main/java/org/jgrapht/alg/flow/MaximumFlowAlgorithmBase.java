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
 * MaximumFlowAlgorithmBase.java
 * -----------------
 * (C) Copyright 2015-2015, by Alexey Kudinkin and Contributors.
 *
 * Original Author:  Alexey Kudinkin
 * Contributor(s):
 *
 * $Id$
 *
 * Changes
 * -------
 */
package org.jgrapht.alg.flow;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.alg.util.*;
import org.jgrapht.alg.util.Extension.*;


/**
 * Base class backing algorithms allowing to derive <a
 * href="https://en.wikipedia.org/wiki/Maximum_flow_problem">maximum-flow</a>
 * from the supplied <a href="https://en.wikipedia.org/wiki/Flow_network">flow
 * network</a>
 *
 * @param <V> vertex concept type
 * @param <E> edge concept type
 *
 * @author Alexey Kudinkin
 */
public abstract class MaximumFlowAlgorithmBase<V, E>
    implements MaximumFlowAlgorithm<V, E>
{
    /**
     * Default tolerance.
     */
    public static final double DEFAULT_EPSILON = 1e-9;

    protected Graph<V, E> network; // our network
    protected final boolean DIRECTED_GRAPH; //indicates whether the input graph is directed or not


    private Extension<V, ? extends VertexExtensionBase> vXs;
    private Extension<E, ? extends EdgeExtensionBase> eXs;


    public MaximumFlowAlgorithmBase(Graph<V,E> network){
        this.network=network;
        this.DIRECTED_GRAPH=network instanceof DirectedGraph;
    }

    @Deprecated //No longer used
    abstract Graph<V, E> getNetwork();



    <VE extends VertexExtensionBase, EE extends EdgeExtensionBase> void init(
        ExtensionFactory<VE> vertexExtensionFactory,
        ExtensionFactory<EE> edgeExtensionFactory)
    {
        vXs = new Extension<V, VE>(vertexExtensionFactory);
        eXs = new Extension<E, EE>(edgeExtensionFactory);

        buildInternal();
    }

    private void buildInternal()
    {
        if(DIRECTED_GRAPH) { //Directed graph
            DirectedGraph<V,E> directedGraph=(DirectedGraph) network;
            for (V u : directedGraph.vertexSet()) {
                VertexExtensionBase ux = extendedVertex(u);

                ux.prototype = u;

                for (E e : directedGraph.outgoingEdgesOf(u)) {
                    V v = directedGraph.getEdgeTarget(e);

                    VertexExtensionBase vx = extendedVertex(v);
                    //NOTE NOTE: vx.prototype not set?!

                    EdgeExtensionBase forwardEdge = createEdge(ux, vx, e, directedGraph.getEdgeWeight(e));
                    EdgeExtensionBase backwardEdge = createBackwardEdge(forwardEdge);

                    ux.getOutgoing().add(forwardEdge);

                    // NB: Any better?
                    if (backwardEdge.prototype == null) {
                        vx.getOutgoing().add(backwardEdge);
                    }
                }
            }
        }else{ //Undirected graph
            System.out.println("Building undirected graph");
            Map<V, VertexExtensionBase> vertexToVertexExtensionMap=new HashMap<>();
            for(V v : network.vertexSet()) {
                VertexExtensionBase vx = extendedVertex(v);
                vx.prototype=v;
                vertexToVertexExtensionMap.put(v, vx);
            }
            for(E e : network.edgeSet()){
                VertexExtensionBase ux=vertexToVertexExtensionMap.get(network.getEdgeSource(e));
                VertexExtensionBase vx=vertexToVertexExtensionMap.get(network.getEdgeTarget(e));
                EdgeExtensionBase forwardEdge = createEdge(ux, vx, e, network.getEdgeWeight(e));
                EdgeExtensionBase backwardEdge = createBackwardEdge(forwardEdge);
                ux.getOutgoing().add(forwardEdge);
                vx.getOutgoing().add(backwardEdge);
            }
            //TEMP
            for(V v : network.vertexSet()){
                VertexExtensionBase ux=vertexToVertexExtensionMap.get(v);
                for(EdgeExtensionBase edgeExtensionBase: ux.getOutgoing())
                    System.out.println(edgeExtensionBase);
            }
            //END TEMP
        }
    }

    private EdgeExtensionBase createEdge(
            VertexExtensionBase source,
            VertexExtensionBase target,
            E e,
            double weight)
    {
        EdgeExtensionBase ex = extendedEdge(e);
        ex.source = source;
        ex.target = target;
        ex.capacity = weight;
        ex.prototype = e;

        return ex;
    }

    private EdgeExtensionBase createBackwardEdge(
            EdgeExtensionBase forwardEdge)
    {
        EdgeExtensionBase backwardEdge;
        E backwardPrototype = network.getEdge(forwardEdge.target.prototype, forwardEdge.source.prototype);

        if (backwardPrototype != null) {
            if(DIRECTED_GRAPH)
                backwardEdge = createEdge(forwardEdge.target, forwardEdge.source, backwardPrototype, network.getEdgeWeight(backwardPrototype));
            else{ //Undirected graph
                backwardEdge = eXs.createInstance();
                backwardEdge.source = forwardEdge.target;
                backwardEdge.target = forwardEdge.source;
                backwardEdge.capacity= network.getEdgeWeight(backwardPrototype);
                backwardEdge.prototype=backwardPrototype;
            }

        } else {
            backwardEdge = eXs.createInstance();

            backwardEdge.source = forwardEdge.target;
            backwardEdge.target = forwardEdge.source;
        }

        forwardEdge.inverse = backwardEdge;
        backwardEdge.inverse = forwardEdge;



        return backwardEdge;
    }

    /*protected <VE extends VertexExtensionBase> VE extendedVertex(V v)
    {
        return this.vertexExtended(v);
    }

    protected <EE extends EdgeExtensionBase> EE extendedEdge(E e)
    {
        return this.edgeExtended(e);
    }*/


    protected VertexExtensionBase extendedVertex(V v)
    {
        return this.vertexExtended(v);
    }

    protected EdgeExtensionBase extendedEdge(E e)
    {
        return this.edgeExtended(e);
    }


    protected <VE extends VertexExtensionBase> VE vertexExtended(V v)
    {
        return (VE) vXs.get(v);
    }

    protected <EE extends EdgeExtensionBase> EE edgeExtended(E e)
    {
        return (EE) eXs.get(e);
    }

    /**
     * Increase flow in the direction denoted by edge (u,v). Any existing flow in the reverse direction (v,u) gets reduced first. More precisely, let f2 be the existing flow
     * in the direction (v,u), and f1 be the desired increase of flow in direction (u,v). If f1 >= f2, then the flow on (v,u) becomes 0, and the flow on (u,v) becomes f1-f2. Else, if f1<f2, the flow in the direction
     * (v,u) is reduced, i.e. the flow on (v,u) becomes f2-f1, whereas the flow on (u,v) remains zero.
     * @param edge desired direction in which the flow is increased
     * @param flow increase of flow in the the direction indicated by the forwardEdge
     */
    protected void pushFlowThrough(EdgeExtensionBase edge, double flow)
    {
        EdgeExtensionBase reverseEdge = edge.getInverse();

        assert ((compareFlowTo(edge.flow, 0.0) == 0) || (compareFlowTo(reverseEdge.flow, 0.0) == 0));

        if (compareFlowTo(reverseEdge.flow, flow) == -1) { //If f1 >= f2
            double flowDifference = flow - reverseEdge.flow;

            edge.flow += flowDifference;
            edge.capacity -= reverseEdge.flow; //Capacity on edge (u,v) PLUS flow on (v,u) gives the MAXIMUM flow in the direction (u,v) i.e edge.weight in the graph 'network'.

            reverseEdge.flow = 0;
            reverseEdge.capacity += flowDifference;
        } else { //If f1 < f2
            edge.capacity -= flow;
            reverseEdge.flow -= flow;
        }
    }

    protected Map<E, Double> composeFlow()
    {
        Map<E, Double> maxFlow = new HashMap<>();
        for (E e : network.edgeSet()) {
            EdgeExtensionBase ex = extendedEdge(e);
            maxFlow.put(e, ex.flow);
        }

        return maxFlow;
    }

    protected int compareFlowTo(double flow, double val)
    {
        double diff = flow - val;
        if (Math.abs(diff) < DEFAULT_EPSILON) {
            return 0;
        } else {
            return (diff < 0) ? -1 : 1;
        }
    }

    class VertexExtensionBase
        extends Extension.BaseExtension
    {
        private final List<? extends EdgeExtensionBase> outgoing = new ArrayList<>();

        V prototype;

        double excess;

        public <EE extends EdgeExtensionBase> List<EE> getOutgoing()
        {
            return (List<EE>) outgoing;
        }
    }

    class EdgeExtensionBase
        extends Extension.BaseExtension
    {
        private VertexExtensionBase source;
        private VertexExtensionBase target;

        private EdgeExtensionBase inverse;

        E prototype;

        double capacity;
        double flow;

        public <VE extends VertexExtensionBase> VE getSource()
        {
            return (VE) source;
        }

        public void setSource(VertexExtensionBase source)
        {
            this.source = source;
        }

        public <VE extends VertexExtensionBase> VE getTarget()
        {
            return (VE) target;
        }

        public void setTarget(VertexExtensionBase target)
        {
            this.target = target;
        }

        public <EE extends EdgeExtensionBase> EE getInverse()
        {
            return (EE) inverse;
        }

        public void setInverse(EdgeExtensionBase inverse)
        {
            this.inverse = inverse;
        }

        public String toString(){
            return "("+(source==null ? null : source.prototype)+","+(target==null ? null : target.prototype)+",c:"+capacity+")";
        }
    }
}

// End MaximumFlowAlgorithmBase.java


//BACKUP
//public abstract class MaximumFlowAlgorithmBase<V, E>
//        implements MaximumFlowAlgorithm<V, E>
//{
//    /**
//     * Default tolerance.
//     */
//    public static final double DEFAULT_EPSILON = 1e-9;
//
//    private Extension<V, ? extends VertexExtensionBase> vXs;
//    private Extension<E, ? extends EdgeExtensionBase> eXs;
//
//    abstract DirectedGraph<V, E> getNetwork();
//
//    <VE extends VertexExtensionBase, EE extends EdgeExtensionBase> void init(
//            ExtensionFactory<VE> vertexExtensionFactory,
//            ExtensionFactory<EE> edgeExtensionFactory)
//    {
//        vXs = new Extension<V, VE>(vertexExtensionFactory);
//        eXs = new Extension<E, EE>(edgeExtensionFactory);
//
//        buildInternal();
//    }
//
//    private void buildInternal()
//    {
//        DirectedGraph<V, E> n = getNetwork();
//
//        for (V u : n.vertexSet()) {
//            VertexExtensionBase ux = extendedVertex(u);
//
//            ux.prototype = u;
//
//            for (E e : n.outgoingEdgesOf(u)) {
//                V v = n.getEdgeTarget(e);
//
//                VertexExtensionBase vx = extendedVertex(v);
//
//                EdgeExtensionBase ex =
//                        createEdge(ux, vx, e, n.getEdgeWeight(e));
//                EdgeExtensionBase iex = createInverse(ex, n);
//
//                ux.getOutgoing().add(ex);
//
//                // NB: Any better?
//                if (iex.prototype == null) {
//                    vx.getOutgoing().add(iex);
//                }
//            }
//        }
//    }
//
//    private EdgeExtensionBase createEdge(
//            VertexExtensionBase source,
//            VertexExtensionBase target,
//            E e,
//            double weight)
//    {
//        EdgeExtensionBase ex = extendedEdge(e);
//
//        ex.source = source;
//        ex.target = target;
//        ex.capacity = weight;
//        ex.prototype = e;
//
//        return ex;
//    }
//
//    private EdgeExtensionBase createInverse(
//            EdgeExtensionBase ex,
//            DirectedGraph<V, E> n)
//    {
//        EdgeExtensionBase iex;
//
//        if (n.containsEdge(ex.target.prototype, ex.source.prototype)) {
//            E ie = n.getEdge(ex.target.prototype, ex.source.prototype);
//            iex = createEdge(ex.target, ex.source, ie, n.getEdgeWeight(ie));
//        } else {
//            iex = eXs.createInstance();
//
//            iex.source = ex.target;
//            iex.target = ex.source;
//        }
//
//        ex.inverse = iex;
//        iex.inverse = ex;
//
//        return iex;
//    }
//
//    private VertexExtensionBase extendedVertex(V v)
//    {
//        return this.<VertexExtensionBase>vertexExtended(v);
//    }
//
//    private EdgeExtensionBase extendedEdge(E e)
//    {
//        return this.<EdgeExtensionBase>edgeExtended(e);
//    }
//
//    protected <VE extends VertexExtensionBase> VE vertexExtended(V v)
//    {
//        return (VE) vXs.get(v);
//    }
//
//    protected <EE extends EdgeExtensionBase> EE edgeExtended(E e)
//    {
//        return (EE) eXs.get(e);
//    }
//
//    protected void pushFlowThrough(EdgeExtensionBase ex, double f)
//    {
//        EdgeExtensionBase iex = ex.<EdgeExtensionBase>getInverse();
//
//        assert ((compareFlowTo(ex.flow, 0.0) == 0)
//                || (compareFlowTo(iex.flow, 0.0) == 0));
//
//        if (compareFlowTo(iex.flow, f) == -1) {
//            double d = f - iex.flow;
//
//            ex.flow += d;
//            ex.capacity -= iex.flow;
//
//            iex.flow = 0;
//            iex.capacity += d;
//        } else {
//            ex.capacity -= f;
//            iex.flow -= f;
//        }
//    }
//
//    protected Map<E, Double> composeFlow()
//    {
//        Map<E, Double> maxFlow = new HashMap<E, Double>();
//        for (E e : getNetwork().edgeSet()) {
//            EdgeExtensionBase ex = extendedEdge(e);
//            maxFlow.put(e, ex.flow);
//        }
//
//        return maxFlow;
//    }
//
//    protected int compareFlowTo(double flow, double val)
//    {
//        double diff = flow - val;
//        if (Math.abs(diff) < DEFAULT_EPSILON) {
//            return 0;
//        } else {
//            return (diff < 0) ? -1 : 1;
//        }
//    }
//
//    class VertexExtensionBase
//            extends Extension.BaseExtension
//    {
//        private final List<? extends EdgeExtensionBase> outgoing =
//                new ArrayList<EdgeExtensionBase>();
//
//        V prototype;
//
//        double excess;
//
//        public <EE extends EdgeExtensionBase> List<EE> getOutgoing()
//        {
//            return (List<EE>) outgoing;
//        }
//    }
//
//    class EdgeExtensionBase
//            extends Extension.BaseExtension
//    {
//        private VertexExtensionBase source;
//        private VertexExtensionBase target;
//
//        private EdgeExtensionBase inverse;
//
//        E prototype;
//
//        double capacity;
//        double flow;
//
//        public <VE extends VertexExtensionBase> VE getSource()
//        {
//            return (VE) source;
//        }
//
//        public void setSource(VertexExtensionBase source)
//        {
//            this.source = source;
//        }
//
//        public <VE extends VertexExtensionBase> VE getTarget()
//        {
//            return (VE) target;
//        }
//
//        public void setTarget(VertexExtensionBase target)
//        {
//            this.target = target;
//        }
//
//        public <EE extends EdgeExtensionBase> EE getInverse()
//        {
//            return (EE) inverse;
//        }
//
//        public void setInverse(EdgeExtensionBase inverse)
//        {
//            this.inverse = inverse;
//        }
//    }
//}

    /*
@TODO remove edgeExtended/extendEdge stuff
@TODO flow directions in undirected graph
@TODO clean test classes
@TODO see whether all edgeExtended/extendEdge stuff can be shifted to child classes of MaximumFlowAlgorithmBase
@TODO combine EdgeExtension/VertexExtention classes from MaxFlowAlgBase, EdmondsKarpMaxFlow, PushRelabelMaxFlow
@TODO rename createEdge

Implementation is a bit ugly. A 'new' data structure is introduced. where each vertex maintains itself which edges are outgoing/incoming, and each edge maintains a reverse edge. All these
queries you can do on a SimpleDirectedGraph as well, but currently there's just too much overhead for that to do that efficiently.

    */