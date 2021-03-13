/*
 * (C) Copyright 2015-2020, by Alexey Kudinkin and Contributors.
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
package org.jgrapht.perf.connectivity;

import org.jgrapht.*;
import org.jgrapht.alg.connectivity.*;
import org.jgrapht.alg.flow.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.perf.clique.*;
import org.jgrapht.util.*;
import org.junit.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.util.*;
import java.util.concurrent.*;

public class GabowPerformanceTest
{

    public static final int NUMBER_OF_GRAPHS = 20;
    public static final int PERF_BENCHMARK_VERTICES_COUNT = 100;
    public static final int PERF_BENCHMARK_EDGES_COUNT = 100;

    @State(Scope.Benchmark)
    private static abstract class RandomGraphBenchmarkBase
    {

        public static final long SEED = 1446523573696201013L;

        private List<Graph<Integer, DefaultEdge>> graphs;

        abstract StrongConnectivityAlgorithm<Integer, DefaultEdge> createSolver(
            Graph<Integer, DefaultEdge> network);

        @Setup
        public void setup()
        {
            graphs = new ArrayList<>();

            GraphGenerator<Integer, DefaultEdge, Integer> rgg =
                new GnmRandomGraphGenerator<>(
                    PERF_BENCHMARK_VERTICES_COUNT, PERF_BENCHMARK_EDGES_COUNT, SEED);

            for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
                SimpleDirectedGraph<Integer,
                        DefaultEdge> network = new SimpleDirectedGraph<>(
                        SupplierUtil.createIntegerSupplier(0),
                        SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);

                rgg.generateGraph(network);
                graphs.add(network);
            }
        }

        @Benchmark
        public void run()
        {
            for (Graph<Integer, DefaultEdge> g : graphs) {
                createSolver(g).getStronglyConnectedComponents();
            }
        }
    }

    public static class GabowRandomGraphBenchmark
        extends
        RandomGraphBenchmarkBase
    {
        @Override
        StrongConnectivityAlgorithm<Integer, DefaultEdge> createSolver(
            Graph<Integer, DefaultEdge> network)
        {
            return new GabowStrongConnectivityInspector<>(network);
        }
    }

    public static class GabowRecursiveRandomGraphBenchmark
        extends
        RandomGraphBenchmarkBase
    {
        @Override
        StrongConnectivityAlgorithm<Integer, DefaultEdge> createSolver(
            Graph<Integer, DefaultEdge> network)
        {
            return new GabowStrongConnectivityInspectorRecursive<>(network);
        }
    }

    @Test
    public void testMaximalCliqueRandomGraphBenchmark()
            throws RunnerException
    {
        Options opt = new OptionsBuilder()
                .include(".*" + GabowPerformanceTest.GabowRandomGraphBenchmark.class.getSimpleName() + ".*")
                .include(".*" + GabowPerformanceTest.GabowRecursiveRandomGraphBenchmark.class.getSimpleName() + ".*")
                .mode(Mode.SingleShotTime).timeUnit(TimeUnit.MILLISECONDS).warmupIterations(5)
                .measurementIterations(10).forks(1).shouldFailOnError(true).shouldDoGC(true).build();

        new Runner(opt).run();
    }
}
