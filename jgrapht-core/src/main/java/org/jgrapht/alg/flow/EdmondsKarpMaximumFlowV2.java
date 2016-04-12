package org.jgrapht.alg.flow;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.MaximumFlowAlgorithm;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A <a href = "http://en.wikipedia.org/wiki/Flow_network">flow network</a> is a
 * graph where each edge has a capacity and each edge receives a flow.
 * The amount of flow on an edge can not exceed the capacity of the edge (note,
 * that all capacities must be non-negative!). A flow must satisfy the
 * restriction that the amount of flow into a vertex equals the amount of flow
 * out of it, except when it is a source, which "produces" flow, or sink, which
 * "consumes" flow. The graph can be either directed or undirected. In the directed case,
 * the flow on an arc must be in the direction of the arc. In the undirected case, the flow
 * on an edge {u,v} can go either from u to v, or from v to u.
 *
 * <p>This class computes maximum flow in a network using <a href =
 * "http://en.wikipedia.org/wiki/Edmonds-Karp_algorithm">Edmonds-Karp
 * algorithm</a>. Be careful: for large networks this algorithm may consume
 * significant amount of time (its upper-bound complexity is O(VE^2), where V -
 * amount of vertices, E - amount of edges in the network). The implementation handles
 * both directed and undirected graphs: the runtime O(VE^2) is independent of the type of graph.
 *
 *
 * <p>For more details see Andrew V. Goldberg's <i>Combinatorial Optimization
 * (Lecture Notes)</i>.
 *
 * Note: the graph which is provided during construction, CANNOT be changed. If you change the graph,
 * a new instance of this class must be constructed!
 *
 * @author Joris Kinable
 * @author Ilya Razensteyn
 */
public class EdmondsKarpMaximumFlowV2<V, E> implements MaximumFlowAlgorithm<V, E> {

    private Graph<V, E> graph; // our network
    private QuickLookupGraph residualNetwork;
    private final double EPSILON;     // tolerance (DEFAULT_EPSILON or user-defined)

    private final boolean DIRECTED_GRAPH; //indicates whether the input graph is directed or not
    private Map<V, V> pathPredecessorMap =new HashMap<>();
    private double maxFlowValue=-1; //Result of the max flow calculation
    private V currentSource=null; //Source vertex of last invocation of this algorithm
    private V currentSink=null; //Target vertex of last invocation of this algorithm

    /**
     * Constructs <tt>MaximumFlow</tt> instance to work with <i>a copy of</i>
     * <tt>network</tt>. Current source and sink are set to <tt>null</tt>. If
     * <tt>network</tt> is weighted, then capacities are weights, otherwise all
     * capacities are equal to one. Doubles are compared using <tt>
     * DEFAULT_EPSILON</tt> tolerance.
     *
     * @param graph network, where maximum flow will be calculated
     */
    public EdmondsKarpMaximumFlowV2(DirectedGraph<V, E> graph){
        this(graph, MaximumFlowAlgorithmBase.DEFAULT_EPSILON);
    }

    /**
     * Constructs <tt>MaximumFlow</tt> instance to work with <i>a copy of</i>
     * <tt>network</tt>. Current source and sink are set to <tt>null</tt>. If
     * <tt>network</tt> is weighted, then capacities are weights, otherwise all
     * capacities are equal to one.
     *
     * @param network network, where maximum flow will be calculated
     * @param epsilon tolerance for comparing doubles
     */
    public EdmondsKarpMaximumFlowV2(DirectedGraph<V, E> network, double epsilon){
        if (network == null)
            throw new NullPointerException("network is null");
        if (epsilon <= 0)
            throw new IllegalArgumentException("invalid epsilon (must be positive)");
        for (E e : network.edgeSet()) {
            if (network.getEdgeWeight(e) < -epsilon)
                throw new IllegalArgumentException("invalid capacity (must be non-negative)");
        }

        this.graph = network;
        this.EPSILON = epsilon;
        DIRECTED_GRAPH =true;
        initialize();
    }

    public EdmondsKarpMaximumFlowV2(UndirectedGraph<V, E> graph){
        this(graph, MaximumFlowAlgorithmBase.DEFAULT_EPSILON);
    }

    private EdmondsKarpMaximumFlowV2(Graph<V, E> network, double epsilon){
        if (network == null)
            throw new NullPointerException("network is null");
        if (epsilon <= 0)
            throw new IllegalArgumentException("invalid epsilon (must be positive)");
        for (E e : network.edgeSet()) {
            if (network.getEdgeWeight(e) < -epsilon)
                throw new IllegalArgumentException("invalid capacity (must be non-negative)");
        }

        this.graph = network;
        this.EPSILON = epsilon;
        DIRECTED_GRAPH =false;
        initialize();
    }


    /**
     * Construct a residual network graph which is used to compute the flows.
     */
    private void initialize(){
        residualNetwork =new QuickLookupGraph(DefaultWeightedEdge.class);
        Graphs.addAllVertices(residualNetwork, graph.vertexSet());
        for(E e : graph.edgeSet()){
            V u=graph.getEdgeSource(e);
            V v=graph.getEdgeTarget(e);
            double weight=graph.getEdgeWeight(e);
            if(!residualNetwork.containsEdge(u, v)){
                Graphs.addEdge(residualNetwork, u, v, weight); //Forward edge
                Graphs.addEdge(residualNetwork, v, u, DIRECTED_GRAPH ? 0 : weight); //Backward edge
            }else
                residualNetwork.setEdgeWeight(residualNetwork.getEdge(u,v), weight);
        }
    }
//    private void initialize(){
//        residualNetwork =new QuickLookupGraph(DefaultWeightedEdge.class);
//        Graphs.addAllVertices(residualNetwork, graph.vertexSet());
//        for(E e : graph.edgeSet()){
//            V u=graph.getEdgeSource(e);
//            V v=graph.getEdgeTarget(e);
//            double weight=graph.getEdgeWeight(e);
//            Graphs.addEdge(residualNetwork, u, v, weight);
//            if(DIRECTED_GRAPH) { //Directed Graph
//                if(!graph.containsEdge(v, u))
//                    Graphs.addEdge(residualNetwork, v, u, 0);
//            }else //Undirected graph
//                Graphs.addEdge(residualNetwork, v, u, weight);
//        }
//    }

    /**
     * Calculates a maximum flow from <tt>source</tt>, to <tt>sink</tt>. Note,
     * that <tt>source</tt> and <tt>sink</tt> must exist and must be different. This method
     * constructs a MaximumFlow object. If you are purely interested in the flow value
     * use the slightly faster method getMaximumFlowValue(V source, V sink) instead.
     * Each invocation of this function with a different source/sink pair than the pair used in the previous
     * invocation will recompute the maximum flow!
     *
     * @param source source vertex
     * @param sink sink vertex
     * @return object which stores both the max flow value, as well as the flow on each edge/arc
     */
    @Override
    public MaximumFlow<E> buildMaximumFlow(V source, V sink) {
        if(currentSource != source || currentSink != sink)
            calculateMaxFlow(source, sink);
        Map<E, Double> flowMap=new LinkedHashMap<>();
        if(DIRECTED_GRAPH) { //Directed graph
            for (E e : graph.edgeSet()) {
                DefaultWeightedEdge e_workingGraph = residualNetwork.getEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
                flowMap.put(e, Math.max(graph.getEdgeWeight(e) - residualNetwork.getEdgeWeight(e_workingGraph), 0));
            }
        }else{ //Undirected graph
            for (E e : graph.edgeSet()) {
                DefaultWeightedEdge e1_workingGraph = residualNetwork.getEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
                DefaultWeightedEdge e2_workingGraph = residualNetwork.getEdge(graph.getEdgeTarget(e), graph.getEdgeSource(e));
                double capacity=graph.getEdgeWeight(e);
                flowMap.put(e,  Math.max(capacity- residualNetwork.getEdgeWeight(e1_workingGraph), capacity- residualNetwork.getEdgeWeight(e2_workingGraph)));
            }
        }
        return new MaximumFlowImpl<>(maxFlowValue, flowMap);
    }

    /**
     * Calculates a maximum flow from <tt>source</tt>, to <tt>sink</tt>. Note,
     * that <tt>source</tt> and <tt>sink</tt> must be vertices of the <tt>
     * network</tt> passed to the constructor, and they must be different. This method
     * returns maximum flow value (which equals the minimum cut of the graph).
     * Each invocation of this function with a different source/sink pair than the pair used in the previous
     * invocation will recompute the maximum flow!
     *
     * @param source source vertex
     * @param sink sink vertex
     * @return value of maximum flow (or minimum cut)
     */
    public double getMaximumFlowValue(V source, V sink){
        if(currentSource != source || currentSink != sink)
            calculateMaxFlow(source, sink);
        return maxFlowValue;
    }


    /**
     * Performs a Breadth First Search to find a path from source to sink. The search terminates as soon as a single path has been found.
     * The path is stored in the pathPredecessorMap map.
     * @param source source
     * @param sink sink
     * @return true if a path from source to sink exists, false otherwise
     */
    private boolean bfs(V source, V sink){
        pathPredecessorMap.clear();

        // Create a queue, enqueue source vertex and mark source vertex as visited
        LinkedList<V> queue = new LinkedList<>();
        queue.add(source);
        pathPredecessorMap.put(source, null);

        // Standard BFS Loop
        while (!queue.isEmpty() && !pathPredecessorMap.containsKey(sink)){
            V u = queue.poll();

            for(DefaultWeightedEdge e : residualNetwork.outgoingEdgesOf(u)){
                V v = residualNetwork.getEdgeTarget(e);
                double weight= residualNetwork.getEdgeWeight(e);
                if(!pathPredecessorMap.containsKey(v) && weight > EPSILON){ //Needs to be: weight > epsilon
                    queue.add(v);
                    pathPredecessorMap.put(v, u);
                }
            }
        }

        // If we reached sink in BFS starting from source, then
        // return true, else false
        return pathPredecessorMap.containsKey(sink);
    }

    /**
     * Calculate a maximum flow from <tt>source</tt> to <tt>sink</tt> on the given graph
     * @param source source
     * @param sink sink
     */
    private void calculateMaxFlow(V source, V sink){
        currentSource=source;
        currentSink=sink;
        maxFlowValue = 0;  // There is no flow initially

        // Augment the flow while there exist augmenting paths from source to sink
        while (bfs(source, sink)){
            // Find minimum residual capacity of the edges along the path found by BFS.
            double pathFlow = Integer.MAX_VALUE;
            for (V v=sink; v !=source; v=pathPredecessorMap.get(v)){
                V u = pathPredecessorMap.get(v);
                pathFlow = Math.min(pathFlow, residualNetwork.getEdgeWeight(residualNetwork.getEdge(u, v)));
            }

//            System.out.println("Found augmenting flow with value: "+path_flow);
            // update residual capacities of the arcs in the residual network
            for (V v=sink; v !=source; v=pathPredecessorMap.get(v)){
                V u = pathPredecessorMap.get(v);

                DefaultWeightedEdge e1= residualNetwork.getEdge(u, v);
//                System.out.println("Updating weight edge: " + e1 + " before: " + residualNetwork.getEdgeWeight(e1) + " after: " + (residualNetwork.getEdgeWeight(e1) - path_flow));
                residualNetwork.setEdgeWeight(e1, residualNetwork.getEdgeWeight(e1)-pathFlow);

                DefaultWeightedEdge e2= residualNetwork.getEdge(v, u);
//                System.out.println("Updating weight edge: "+e2+" before: "+ residualNetwork.getEdgeWeight(e2) + " after: "+(residualNetwork.getEdgeWeight(e2)+path_flow));
                residualNetwork.setEdgeWeight(e2, residualNetwork.getEdgeWeight(e2)+pathFlow);
            }

            // Add path flow to overall flow
            maxFlowValue += pathFlow;
        }
    }

    /**
     * Returns the source vertex used in the last invocation of this algorithm
     *
     * @return source of last maxFlow call, null if there was no call
     */
    public V getCurrentSource()
    {
        return currentSource;
    }

    /**
     * Returns the sink vertex used in the last invocation of this algorithm
     *
     * @return sink of last maxFlow call, null if there was no call
     */
    public V getCurrentSink()
    {
        return currentSink;
    }




    public static void main(String[] args){


        /*SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> g1=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for(int i=0; i<6; i++)
            g1.addVertex(i);
        Graphs.addEdge(g1, 0, 1, 16);
        Graphs.addEdge(g1, 0, 2, 13);
        Graphs.addEdge(g1, 1, 2, 10);
        Graphs.addEdge(g1, 1, 3, 12);
        Graphs.addEdge(g1, 2, 1, 4);
        Graphs.addEdge(g1, 2, 4, 14);
        Graphs.addEdge(g1, 3, 2, 9);
        Graphs.addEdge(g1, 3, 5, 20);
        Graphs.addEdge(g1, 4, 3, 7);
        Graphs.addEdge(g1, 4, 5, 4);


        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> maxFlow1=new EdmondsKarpMaximumFlowV2<>(g1);
        double time=System.currentTimeMillis();
        System.out.println("Max flow: "+maxFlow1.buildMaximumFlow(0,5));
        time=System.currentTimeMillis()-time;
        System.out.println("time: "+time);
        EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> maximumFlow1b=new EdmondsKarpMaximumFlow<>(g1);
        time=System.currentTimeMillis();
        System.out.println("Max flow: "+maximumFlow1b.buildMaximumFlow(0,5));
        time=System.currentTimeMillis()-time;
        System.out.println("time: "+time);

        SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> g2=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        g2.addVertex(0);
        g2.addVertex(1);
        Graphs.addEdge(g2, 0, 1, 2.0);

        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> maxFlow2=new EdmondsKarpMaximumFlowV2<>(g2);
        System.out.println("Max flow: "+maxFlow2.buildMaximumFlow(0,1));
        EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> maximumFlow2b=new EdmondsKarpMaximumFlow<>(g2);
        System.out.println("Max flow: "+maximumFlow2b.buildMaximumFlow(0,1));

        //Undirected graph
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> g3=new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for(int i=0; i<9; i++)
            g3.addVertex(i);
        Graphs.addEdge(g3, 0, 1, 12);
        Graphs.addEdge(g3, 0, 2, 15);
        Graphs.addEdge(g3, 0, 3, 20);
        Graphs.addEdge(g3, 1, 5, 5);
        Graphs.addEdge(g3, 1, 6, 2);
        Graphs.addEdge(g3, 1, 2, 5);
        Graphs.addEdge(g3, 2, 6, 6);
        Graphs.addEdge(g3, 2, 4, 3);
        Graphs.addEdge(g3, 2, 3, 11);
        Graphs.addEdge(g3, 3, 4, 4);
        Graphs.addEdge(g3, 3, 7, 8);
        Graphs.addEdge(g3, 4, 6, 6);
        Graphs.addEdge(g3, 4, 7, 1);
        Graphs.addEdge(g3, 5, 6, 9);
        Graphs.addEdge(g3, 5, 8, 18);
        Graphs.addEdge(g3, 6, 7, 7);
        Graphs.addEdge(g3, 6, 8, 13);
        Graphs.addEdge(g3, 7, 8, 10);
        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> maxFlow3=new EdmondsKarpMaximumFlowV2<>(g3);
        System.out.println("Max flow: "+maxFlow3.buildMaximumFlow(0,8));*/


        /*SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> g4=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for(int i=0; i<9; i++)
            g4.addVertex(i);
        Graphs.addEdge(g4, 0, 1, 12);
        Graphs.addEdge(g4, 0, 2, 8);
        Graphs.addEdge(g4, 0, 3, 11);
        Graphs.addEdge(g4, 1, 5, 7);
        Graphs.addEdge(g4, 1, 6, 6 );
        Graphs.addEdge(g4, 1, 2, 5);
        Graphs.addEdge(g4, 2, 6, 9);
        Graphs.addEdge(g4, 2, 4, 4);
        Graphs.addEdge(g4, 2, 3, 2);
        Graphs.addEdge(g4, 3, 2, 13);
        Graphs.addEdge(g4, 3, 4, 6);
        Graphs.addEdge(g4, 3, 7, 12);
        Graphs.addEdge(g4, 4, 7, 7);
        Graphs.addEdge(g4, 5, 6, 4);
        Graphs.addEdge(g4, 5, 8, 3);
        Graphs.addEdge(g4, 6, 4, 15);
        Graphs.addEdge(g4, 6, 8, 9);
        Graphs.addEdge(g4, 7, 6, 8);
        Graphs.addEdge(g4, 7, 8, 10);
        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> maxFlow1=new EdmondsKarpMaximumFlowV2<>(g4);
        System.out.println("Max flow: "+maxFlow1.buildMaximumFlow(0,8));
        EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> maximumFlow1b=new EdmondsKarpMaximumFlow<>(g4);
        System.out.println("Max flow: "+maximumFlow1b.buildMaximumFlow(0,8));*/

        RandomGraphGenerator<Integer, DefaultWeightedEdge> rgg= new RandomGraphGenerator<>(10000, 1000000, 0);

        SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> network
                = new SimpleDirectedWeightedGraph<>((sourceVertex, targetVertex) -> {
            return new DefaultWeightedEdge();
        });

        rgg.generateGraph(
                network,
                new VertexFactory<Integer>() {
                    int i;
                    @Override
                    public Integer createVertex() {
                        return ++i;
                    }
                },
                null
        );
        Object[] vs = network.vertexSet().toArray();
        Integer source  = (Integer) vs[0];
        Integer sink    = (Integer) vs[vs.length - 1];

        System.out.println("Finished creating random graph");

        //TEMP: create a copy of the graph
        long time=System.currentTimeMillis();
        SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> test=new SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        Graphs.addAllVertices(test, network.vertexSet());
        for(DefaultWeightedEdge e : network.edgeSet()){
            int i=network.getEdgeSource(e);
            int j=network.getEdgeTarget(e);
            test.addEdge(i,j);
        }
        time=System.currentTimeMillis()-time;
        System.out.println("Creating a copy of the network took: "+time+" edges in 2: "+test.edgeSet().size());

        time=System.currentTimeMillis();
        SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> test2=new SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        for(int u : network.vertexSet()){
            test2.addVertex(u);
            for(DefaultWeightedEdge e : network.outgoingEdgesOf(u)){
                int v=network.getEdgeTarget(e);
                test2.addVertex(v);
                test2.addEdge(u,v);
            }
        }
        time=System.currentTimeMillis()-time;
        System.out.println("Creating a copy 2 of the network took: "+time+" edges in 2: "+test2.edgeSet().size());



        //end TEMP
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Starting");

        time=System.currentTimeMillis();
        EdmondsKarpMaximumFlow<Integer, DefaultWeightedEdge> solver1=new EdmondsKarpMaximumFlow<>(network);
        time=System.currentTimeMillis()-time;
        System.out.println("Construction took: "+time);
        time=System.currentTimeMillis();
        MaximumFlow<DefaultWeightedEdge> maximumFlow1 =solver1.buildMaximumFlow(source, sink);
        time=System.currentTimeMillis()-time;
        System.out.println("Found flow with value: "+maximumFlow1.getValue()+" in: "+time+"ms");

        time=System.currentTimeMillis();
        EdmondsKarpMaximumFlowV2<Integer, DefaultWeightedEdge> solver2=new EdmondsKarpMaximumFlowV2<>(network);
        time=System.currentTimeMillis()-time;
        System.out.println("Construction took: "+time);
        time=System.currentTimeMillis();
        //MaximumFlow<Integer, DefaultWeightedEdge> maximumFlow2 =solver2.buildMaximumFlow(source, sink);
        double maximumFlow2 =solver2.getMaximumFlowValue(source, sink);
        time=System.currentTimeMillis()-time;
        System.out.println("(new)Found flow with value: "+maximumFlow2+" in: "+time+"ms");
        //System.out.println("(new)Found flow with value: "+maximumFlow2.getValue()+" in: "+time+"ms");
    }

    protected class QuickLookupGraph extends SimpleDirectedWeightedGraph<V, DefaultWeightedEdge>{
        Map<V, Map<V,DefaultWeightedEdge>> edgeCache=new HashMap<>();

        public QuickLookupGraph(Class<? extends DefaultWeightedEdge> edgeClass) {
            super(edgeClass);
        }

        @Override
        public boolean addVertex(V v){
            boolean result=super.addVertex(v);
            if(result)
                edgeCache.put(v, new HashMap<>());
            return result;
        }
        @Override
        public boolean removeVertex(V v){
            boolean result=super.removeVertex(v);
            if(result)
                edgeCache.remove(v);
            return result;
        }


        @Override
        public boolean addEdge(V v1, V v2, DefaultWeightedEdge e){
            boolean result=super.addEdge(v1, v2, e);
            if(result)
                edgeCache.get(v1).put(v2, e);
            return result;
        }

        @Override
        public DefaultWeightedEdge addEdge(V v1, V v2){
            EdgeFactory<V, DefaultWeightedEdge> edgeFactory=super.getEdgeFactory();
            DefaultWeightedEdge e=edgeFactory.createEdge(v1, v2);
            this.addEdge(v1, v2, e);
            return e;
        }

        @Override
        public boolean containsEdge(V v1, V v2){
            return getEdge(v1, v2)!=null;
        }
        @Override
        public boolean containsEdge(DefaultWeightedEdge e){
            return containsEdge(this.getEdgeSource(e), this.getEdgeTarget(e));
        }

        @Override
        public DefaultWeightedEdge removeEdge(V v1, V v2){
            DefaultWeightedEdge edge=super.removeEdge(v1, v2);
            if(edge!=null)
                edgeCache.get(v1).remove(v2);
            return edge;
        }
        @Override
        public boolean removeEdge(DefaultWeightedEdge e){
            V source=this.getEdgeSource(e);
            V target=this.getEdgeTarget(e);
            boolean result=super.removeEdge(e);
            if(result)
                edgeCache.get(source).remove(target);
            return result;
        }

        @Override
        public DefaultWeightedEdge getEdge(V v1, V v2){
            if(!edgeCache.containsKey(v1) || !edgeCache.get(v1).containsKey(v2))
                return null;
            else
                return edgeCache.get(v1).get(v2);
        }
    }
}

//public class EdgeHashMap<V,E> extends HashMap<V,E>{
//    @Override
//    public boolean containsKey(Object o){
//        return table[5]==1;
//        //return true;
//    }
//}