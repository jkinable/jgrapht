package org.jgrapht.alg;

import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.util.VertexDegreeComparator;

import java.util.*;

/**
 * Created by jkinable on 7/26/16.
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
        for(int i=0; i<vertices.size(); i++)
            vertexIDDictionary.put(vertices.get(i), i);

        N=vertices.size();
        //Sort vertices in ascending degree
        //TODO JK: check whether this sorts the vertices ascending or descending
        //TODO JK: does it matter whether we sort the vertices? Are there better sortings?
        Collections.sort(vertices, new VertexDegreeComparator<>(graph, false));

        //Calculate a bound on the maximum depth using heuristics and mathematical bounding procedures.
        //TODO JK: do these 2 heuristics yield different results, does one dominate the other? If so, remove one.
        //TODO JK: lookup mathematical bounding procedures. The tighter the bound the better.
        boundOnVertexCoverWeight =Math.min(
                            super.find2ApproximationCover(graph).size(),
                            super.findGreedyCover(graph).size()
                        );

        //Invoke recursive algorithm
        VertexCover vertexCover= this.calculateCoverRecursively(0, new BitSet(vertices.size()), 0);

        //Build solution
        Set<V> verticesInCover=new LinkedHashSet<>();
        for (int i = vertexCover.cover.nextSetBit(0); i >= 0; i = vertexCover.cover.nextSetBit(i+1))
            verticesInCover.add(vertices.get(i));
        return verticesInCover;

    }

    private VertexCover calculateCoverRecursively(int lastVisited, BitSet visited, int depth){
        //Check memo
        if(memo.containsKey(visited))
            return (VertexCover)memo.get(visited).clone();

        //Base case 1: all vertices have been visited
        if(lastVisited == N) {
            VertexCover vertexCover=new VertexCover(N, depth);
            if(vertexCover.weight < boundOnVertexCoverWeight) //Found new, better vertex cover. Update bound
                boundOnVertexCoverWeight=vertexCover.weight;
            return vertexCover;
        }

        //Base case 2: we've exceeded the maximum depth -> add all remaining unvisited vertices to the cover
        if(depth >= boundOnVertexCoverWeight){
            VertexCover vertexCover=new VertexCover(N, depth);
            for(int nextVertexIndex=visited.nextClearBit(lastVisited);
                nextVertexIndex >=0;
                nextVertexIndex=visited.nextClearBit(nextVertexIndex+1)){

                vertexCover.addVertex(nextVertexIndex, 1);
            }
            return  vertexCover;
        }


        //Find the next unvisited vertex WITH neighbors (if a vertex has no neighbors, then we don't need to select it
        //because it doesn't cover any edges)
        int nextVertexIndex;
        List<V> neighbors=Collections.emptyList();
        for(nextVertexIndex=visited.nextClearBit(lastVisited);
            nextVertexIndex >=0 && neighbors.isEmpty();
            nextVertexIndex=visited.nextClearBit(nextVertexIndex+1)){

            neighbors=Graphs.neighborListOf(graph,vertices.get(nextVertexIndex));
            for(Iterator<V> it=neighbors.iterator(); it.hasNext(); ) //Exclude all visited vertices @TODO: is this implemented correctly?
                if(visited.get(vertexIDDictionary.get(it.next())))
                    it.remove();
        }

        if(nextVertexIndex==-1){ //We've visited all vertices, return the base case
            return new VertexCover(N, depth);
        }

        //Recursion
        //@TODO JK: Can we use a lower bound or estimation which of these 2 branches produces a better solution? If one of them is likely to produce a better solution,
        // then that branch should be explored first!

        //Left branch:
        BitSet visitedLeftBranch= (BitSet) visited.clone();
        visitedLeftBranch.set(nextVertexIndex);

        //Right branch:
        BitSet visitedRightBranch= (BitSet) visited.clone();
        visitedRightBranch.set(nextVertexIndex);
        for(V v : neighbors)
            visitedRightBranch.set(vertexIDDictionary.get(v));

        VertexCover leftCover=calculateCoverRecursively(nextVertexIndex, visitedLeftBranch, depth+1);
        VertexCover rightCover=calculateCoverRecursively(nextVertexIndex, visitedRightBranch, depth+1);


        //Select the best branch
        if(leftCover.weight <= rightCover.weight){
            leftCover.addVertex(nextVertexIndex); //Delayed update of the left cover
            memo.put(visited, leftCover);
            return leftCover;
        }else{
            for(V v : neighbors) //Delayed update of the right cover
                rightCover.addVertex(vertexIDDictionary.get(v));
            memo.put(visited, rightCover);
            return rightCover;
        }

    }


    public class VertexCover{
        public BitSet cover;
        public int weight;

        public VertexCover(int size, int weight){
            cover=new BitSet(size);
            this.weight=weight;
        }

        //Copy constructor
        public VertexCover(VertexCover vertexCover){
            this.cover=vertexCover.cover;
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
 */