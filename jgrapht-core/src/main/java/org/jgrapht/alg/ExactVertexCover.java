package org.jgrapht.alg;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.alg.util.VertexDegreeComparator;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

import javax.xml.bind.SchemaOutputResolver;
import java.util.*;

/**
 * Created by jkinable on 7/26/16.
 * TODO: ADD neighbor index
 */
public class ExactVertexCover<V,E> extends VertexCovers{

    /** Input graph **/
    private final UndirectedGraph<V,E> graph;
    /** Number of vertices in the graph**/
    private int N;

    /** Map for memoization **/
    private Map<BitSet, VertexCover> memo;

    /** Ordered list of vertices which will be iteratively considered to be included in a matching **/
    private List<V> vertices;
    /** Mapping of a vertex to its index in the list of vertices **/
    private Map<V,Integer> vertexIDDictionary;

    /**
     * Maximum weight of the vertex cover. In case there is no weight assigned to the vertices, the weight of
     * the cover equals the cover's cardinality.
     */
    private int boundOnVertexCoverWeight;

    public ExactVertexCover(UndirectedGraph<V,E> graph){
        this.graph=graph;
    }

    public Set<V> getMinimumVertexCover(){
        //Initialize
        memo=new HashMap<>();
        vertices=new ArrayList<>(graph.vertexSet());
        vertexIDDictionary=new HashMap<>();

        N=vertices.size();
        //Sort vertices based on descending degree
        //TODO JK: does it matter whether we sort the vertices (probably yes)? Are there better sortings?
        Collections.sort(vertices, new VertexDegreeComparator<>(graph, false));
        for(int i=0; i<vertices.size(); i++)
            vertexIDDictionary.put(vertices.get(i), i);
        System.out.println("Vertices: "+vertices);

        //Calculate a bound on the maximum depth using heuristics and mathematical bounding procedures.
        //TODO JK: do these 2 heuristics yield different results, does one dominate the other? If so, remove one.
        //TODO JK: lookup mathematical bounding procedures for the min vertex cover problem. The tighter the bound the better.
        //TODO JK: Is there a lower bounding procedure which allows us to prematurely terminate the search once a solution is found which is equal to the lower bound?
        boundOnVertexCoverWeight =Math.min(
                            super.find2ApproximationCover(graph).size(),
                            super.findGreedyCover(graph).size()
                        );

        System.out.println("Bound; "+boundOnVertexCoverWeight);
        //Invoke recursive algorithm
        VertexCover vertexCover= this.calculateCoverRecursively(0, new BitSet(N), 0);

        //Build solution
        System.out.println("Optimum cover weight: "+vertexCover.weight);
        Set<V> verticesInCover=new LinkedHashSet<>();
        for (int i = vertexCover.cover.nextSetBit(0); i >= 0 && i<N; i = vertexCover.cover.nextSetBit(i+1))
            verticesInCover.add(vertices.get(i));
        System.out.println("Is cover: "+isCover(verticesInCover, graph));
        return verticesInCover;

    }

    private VertexCover calculateCoverRecursively(int lastVisited, BitSet visited, int accumulatedWeight){
//        System.out.println("Solving for: accumulatedWeight: "+accumulatedWeight+" visited: "+printVisited(visited)+" lastVisited: "+vertices.get(lastVisited));
        //Check memo
        if(memo.containsKey(visited)) {
//            System.out.println("Cache hit");
            return (VertexCover) memo.get(visited).clone();
        }

        //Find the next unvisited vertex WITH neighbors (if a vertex has no neighbors, then we don't need to select it
        //because it doesn't cover any edges)
        int indexNextVertex=-1;
        List<V> neighbors=Collections.emptyList();
        for(int index=visited.nextClearBit(lastVisited);
            index >=0 && index<N;
            index=visited.nextClearBit(index+1)){

            neighbors=Graphs.neighborListOf(graph,vertices.get(index));
            for(Iterator<V> it=neighbors.iterator(); it.hasNext(); ) //Exclude all visited vertices
                if(visited.get(vertexIDDictionary.get(it.next())))
                    it.remove();
            if(!neighbors.isEmpty()) {
                indexNextVertex = index;
                break;
            }
        }
//        System.out.println("nextVertex: "+(indexNextVertex >= 0 ? vertices.get(indexNextVertex) : indexNextVertex)+" remaining neighbors: "+neighbors);

        //Base case 1: all vertices have been visited
        if(indexNextVertex==-1){ //We've visited all vertices, return the base case
            VertexCover vertexCover=new VertexCover(N, 0);
            if(accumulatedWeight <= boundOnVertexCoverWeight) { //Found new a solution that matches our bound. Tighten the bound.
//                System.out.println("Found solution with weight: "+accumulatedWeight+" tightening bound to: "+(accumulatedWeight - 1));
                boundOnVertexCoverWeight = accumulatedWeight - 1;
            }
            return vertexCover;
        //Pruning: this vertex cover can never be better than the best cover we already have. Return a cover with a high weight, such that the other branch will be preferred over this branch.
        }else if(accumulatedWeight >= boundOnVertexCoverWeight){
//            System.out.println("Pruning");
            return new VertexCover(N, N);
        }

        //Recursion
        //@TODO JK: Can we use a lower bound or estimation which of these 2 branches produces a better solution? If one of them is likely to produce a better solution,
        // then that branch should be explored first! Futhermore, if the lower bound+accumulated cost > boundOnVertexCoverWeight, then we may prune.

        /* Create 2 branches:
            Left Branch: vertex v is added to the cover, and we solve for G_{v}
            Right branch: N(v) are added to the cover, and we solve for G_{N(v) \cup v }.
            Here N(v) is the set of neighbors of v. G_{v} indicates the graph obtained by removing vertex v and all vertices incident to it.
         */


        //Right branch:
        BitSet visitedRightBranch= (BitSet) visited.clone();
        visitedRightBranch.set(indexNextVertex);
        for(V v : neighbors)
            visitedRightBranch.set(vertexIDDictionary.get(v));

        VertexCover rightCover=calculateCoverRecursively(indexNextVertex, visitedRightBranch, accumulatedWeight+neighbors.size());
        for(V v : neighbors) //Delayed update of the right cover
            rightCover.addVertex(vertexIDDictionary.get(v), 1);

        //Left branch:
        BitSet visitedLeftBranch= (BitSet) visited.clone();
        visitedLeftBranch.set(indexNextVertex);

        VertexCover leftCover=calculateCoverRecursively(indexNextVertex, visitedLeftBranch, accumulatedWeight+1);
        leftCover.addVertex(indexNextVertex, 1); //Delayed update of the left cover


        //Return the best branch
        if(leftCover.weight <= rightCover.weight){
            memo.put(visited, (VertexCover) leftCover.clone());
            return leftCover;
        }else{

            memo.put(visited, (VertexCover) rightCover.clone());
            return rightCover;
        }

    }

    public String printVisited(BitSet visited){
        List<V> visitedVertices=new ArrayList<>();
        for (int i = visited.nextSetBit(0); i >= 0 && i<N; i = visited.nextSetBit(i+1))
            visitedVertices.add(vertices.get(i));
        return visitedVertices.toString();
    }


    public class VertexCover{
        public BitSet cover;
        public int weight;

        public VertexCover(int size, int weight){
            cover=new BitSet(size);
            this.weight=weight;
        }

//        public VertexCover(int size){
//            cover=new BitSet(size);
////            this.weight=weight;
//        }

        //Copy constructor
        public VertexCover(VertexCover vertexCover){
            this.cover= (BitSet) vertexCover.cover.clone();
            this.weight=vertexCover.weight;
        }

        @Override
        public Object clone(){
            return new VertexCover(this);
        }

        public void addVertex(int vertexIndex){
            cover.set(vertexIndex);
        }

        public void addVertex(int vertexIndex, int weight){
            cover.set(vertexIndex);
            this.weight+=weight;
        }
    }

    private boolean isCover(
            Set<V> vertexSet,
            Graph<V, E> g)
    {
        Set<E> uncoveredEdges = new HashSet<>(g.edgeSet());

        for (V v : vertexSet) {
            uncoveredEdges.removeAll(g.edgesOf(v));
        }

        return uncoveredEdges.size() == 0;
    }

    //Temporary code, needs to be deleted
    public static void main(String[] args){
//        UndirectedGraph<Integer, DefaultEdge> g1=new SimpleGraph<>(DefaultEdge.class);
//        Graphs.addAllVertices(g1, Arrays.asList(0,1,2,3));
//        g1.addEdge(0,1);
//        g1.addEdge(1,2);
//        g1.addEdge(2,3);
//        g1.addEdge(3,0);
//        ExactVertexCover<Integer, DefaultEdge> c1=new ExactVertexCover<>(g1);
//        System.out.println("Cover c1: "+c1.getMinimumVertexCover()); //Optimal: 2


//        UndirectedGraph<Integer, DefaultEdge> g2=new SimpleGraph<>(DefaultEdge.class);
//        Graphs.addAllVertices(g2, Arrays.asList(0,1,2,3,4,5,6,7));
//        g2.addEdge(1,2);
//        g2.addEdge(2,3);
//        g2.addEdge(3,4);
//        g2.addEdge(4,5);
//        g2.addEdge(5,6);
//        g2.addEdge(6,7);
//        g2.addEdge(7,1);
//        g2.addEdge(0,1);
//        g2.addEdge(0,2);
//        g2.addEdge(0,3);
//        g2.addEdge(0,4);
//        g2.addEdge(0,5);
//        g2.addEdge(0,6);
//        g2.addEdge(0,7);
//        ExactVertexCover<Integer, DefaultEdge> c2=new ExactVertexCover<>(g2);
//        System.out.println("Cover c2: "+c2.getMinimumVertexCover()); //Optimal: 5


//        UndirectedGraph<Integer, DefaultEdge> g3=new SimpleGraph<>(DefaultEdge.class);
//        Graphs.addAllVertices(g3, Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11));
//        g3.addEdge(0,1);
//        g3.addEdge(0,9);
//        g3.addEdge(0,7);
//        g3.addEdge(1,2);
//        g3.addEdge(1,5);
//        g3.addEdge(2,3);
//        g3.addEdge(2,4);
//        g3.addEdge(3,4);
//        g3.addEdge(3,5);
//        g3.addEdge(4,11);
//        g3.addEdge(5,6);
//        g3.addEdge(6,7);
//        g3.addEdge(6,8);
//        g3.addEdge(7,8);
//        g3.addEdge(8,10);
//        g3.addEdge(9,10);
//        g3.addEdge(9,11);
//        g3.addEdge(10,11);
//        ExactVertexCover<Integer, DefaultEdge> c3=new ExactVertexCover<>(g3);
//        System.out.println("Cover c3: "+c3.getMinimumVertexCover()); //Optimal: 7
//        System.out.println("Cover c3: "+VertexCovers.findMinimumVertexCover(g3)); //Optimal: 7

//        UndirectedGraph<Integer, DefaultEdge> g4=new SimpleGraph<>(DefaultEdge.class);
//        Graphs.addAllVertices(g4, Arrays.asList(0,1,2,3,4,5));
//        g4.addEdge(0,2);
//        g4.addEdge(1,2);
//        g4.addEdge(2,3);
//        g4.addEdge(3,4);
//        g4.addEdge(3,5);
//        ExactVertexCover<Integer, DefaultEdge> c4=new ExactVertexCover<>(g4);
//        System.out.println("Cover c4: "+c4.getMinimumVertexCover()); //Optimal: 2


        RandomGraphGenerator<Integer, DefaultEdge> gen =
                new RandomGraphGenerator<>(
                        200,
                        15000);
        VertexFactory<Integer> f =
                new VertexFactory<Integer>() {
                    int gid;

                    @Override
                    public Integer createVertex()
                    {
                        return gid++;
                    }
                };

        UndirectedGraph<Integer, DefaultEdge> g5 = new SimpleGraph<>(DefaultEdge.class);
        gen.generateGraph(g5, f, new HashMap<>());
        ExactVertexCover<Integer, DefaultEdge> c5=new ExactVertexCover<>(g5);
        System.out.println("Cover c5: "+c5.getMinimumVertexCover());
//        System.out.println("Cover c5: "+VertexCovers.findMinimumVertexCover(g5)); //Optimal: 7

//        UndirectedGraph<Integer, DefaultEdge> g3=new SimpleGraph<>(DefaultEdge.class);
//        Graphs.addAllVertices(g3, Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11));
    }
}


/*
Many of the issues from the previous revision remain:
-you still use toString() as a unique key, which is still insecure and very slow. Read about hashing. Also read about String modifications. Strings in Java are immutable, so a toString() on a Collection object translates to a whole bunch of string concatenations (see http://stackoverflow.com/questions/10078912/best-practices-performance-mixing-stringbuilder-append-with-string-concat) which again is expensive and unnecessary.
-the algorithm should benefit from a quick greedy initial solution: this can limit the depth of the recursion.
-there's a lot of overhead in your recursion. Consider what happens for example if you choose a vertex without any neighbors?
-You added sorting, but did you actually test whether that made a difference in performance? Aside from that, you shouldn't implement a sorting mechanism that runs in O(n^2) time; use the build in sort mechanisms in java. Read about Comparators, Collections.sort and anonymous functions, e.g.:
	List<V> vertices=new ArrayList<>(g.vertexSet());
        Collections.sort(vertices, new VertexDegreeComparator<>(g, false));
	//OR USE:
        Collections.sort(vertices, (v1, v2) -> Integer.compare(g.degreeOf(v1), g.degreeOf(v2)));
-I'm not sure that your additional code to maintain lists of neighbors is necessary or faster than just querying the neighbors. First it helps to check how getNeighbors() gets its vertices. If that's efficient, then there's nothing you can optimize. Alternatively, if it's inefficient, then you could use NeighborIndex.
-While iterating over a bitset, you should not check every bit; instead, use the nextSetBit method
Seems to be an error in your code: does not always return the minimum cover
 */