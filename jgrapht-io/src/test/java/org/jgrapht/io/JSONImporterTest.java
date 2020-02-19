/*
 * (C) Copyright 2019-2020, by Dimitrios Michail and Contributors.
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
package org.jgrapht.io;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.graph.builder.*;
import org.jgrapht.util.*;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link JsonImporter}.
 * 
 * @author Dimitrios Michail
 */
@Deprecated
public class JSONImporterTest
{

    @Test
    public void testUndirectedUnweighted()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\" },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { \"id\":\"3\" },\n"
                     + "  { \"id\":\"4\" }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\" },\n"
                     + "  { \"source\":\"1\", \"target\":\"3\" }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(4, g.vertexSet().size());
        assertEquals(2, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
        assertTrue(g.containsVertex("2"));
        assertTrue(g.containsVertex("3"));
        assertTrue(g.containsVertex("4"));
        assertTrue(g.containsEdge("1", "2"));
        assertTrue(g.containsEdge("1", "3"));
    }

    @Test
    public void testMixedStringAndIntegerIds()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":1 },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { \"id\":\"3\" },\n"
                     + "  { \"id\":4 }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":1, \"target\":\"2\" },\n"
                     + "  { \"source\":1, \"target\":3 }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(4, g.vertexSet().size());
        assertEquals(2, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
        assertTrue(g.containsVertex("2"));
        assertTrue(g.containsVertex("3"));
        assertTrue(g.containsVertex("4"));
        assertTrue(g.containsEdge("1", "2"));
        assertTrue(g.containsEdge("1", "3"));
    }

    @Test(expected = ImportException.class)
    public void testDuplicateNodeIds()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":1 },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { \"id\":1 }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\" }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));
    }

    @Test(expected = ImportException.class)
    public void testMissingSourceOnEdge()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":1 },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\" },\n"
                     + "  { \"target\":\"2\" },\n"                     
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));
    }

    @Test(expected = ImportException.class)
    public void testMissingTargetOnEdge()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":1 },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\" },\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        JSONImporter<String,
            DefaultEdge> importer = new JSONImporter<>(
                (label, attributes) -> label,
                (from, to, label, attributes) -> g.getEdgeSupplier().get());
        importer.importGraph(g, new StringReader(input));
    }

    @Test
    public void testWeightsOnWeighted()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\" },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { \"id\":\"3\" },\n"
                     + "  { \"id\":\"4\" }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\", \"weight\": 2.0 },\n"
                     + "  { \"source\":\"1\", \"target\":\"3\", \"weight\": 3.0 },\n"
                     + "  { \"source\":\"2\", \"target\":\"3\" }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true).weighted(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(4, g.vertexSet().size());
        assertEquals(3, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
        assertTrue(g.containsVertex("2"));
        assertTrue(g.containsVertex("3"));
        assertTrue(g.containsVertex("4"));
        assertEquals(2.0, g.getEdgeWeight(g.getEdge("1", "2")), 1e-9);
        assertEquals(3.0, g.getEdgeWeight(g.getEdge("1", "3")), 1e-9);
        assertEquals(1.0, g.getEdgeWeight(g.getEdge("2", "3")), 1e-9);
    }

    @Test
    public void testWeightsOnUnweighted()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\" },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { \"id\":\"3\" },\n"
                     + "  { \"id\":\"4\" }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\", \"weight\": 2.0 },\n"
                     + "  { \"source\":\"1\", \"target\":\"3\", \"weight\": 3.0 },\n"
                     + "  { \"source\":\"2\", \"target\":\"3\" }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true).weighted(false)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(4, g.vertexSet().size());
        assertEquals(3, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
        assertTrue(g.containsVertex("2"));
        assertTrue(g.containsVertex("3"));
        assertTrue(g.containsVertex("4"));
        assertEquals(1.0, g.getEdgeWeight(g.getEdge("1", "2")), 1e-9);
        assertEquals(1.0, g.getEdgeWeight(g.getEdge("1", "3")), 1e-9);
        assertEquals(1.0, g.getEdgeWeight(g.getEdge("2", "3")), 1e-9);
    }

    @Test
    public void testNodeAttributes()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\", \"label\": \"Label\", \"int\": 4, \"double\": 0.5, \"boolean\": true, \"boolean1\": false, \"novalue\": null }\n"
                     + "  ],\n"
                     + "  \"edges\": null"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true).weighted(false)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (id, attributes) -> {
            if (id.equals("1")) {
                assertTrue(attributes.get("label").getType().equals(AttributeType.STRING));
                assertTrue(attributes.get("label").getValue().equals("Label"));
                assertTrue(attributes.get("int").getType().equals(AttributeType.INT));
                assertTrue(attributes.get("int").getValue().equals("4"));
                assertTrue(attributes.get("double").getType().equals(AttributeType.DOUBLE));
                assertTrue(attributes.get("double").getValue().equals("0.5"));
                assertTrue(attributes.get("boolean").getType().equals(AttributeType.BOOLEAN));
                assertTrue(attributes.get("boolean").getValue().equals("true"));
                assertTrue(attributes.get("boolean1").getType().equals(AttributeType.BOOLEAN));
                assertTrue(attributes.get("boolean1").getValue().equals("false"));
                assertTrue(attributes.get("novalue").getType().equals(AttributeType.NULL));
                assertTrue(attributes.get("novalue").getValue().equals("null"));
            }
            return id;
        };
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(1, g.vertexSet().size());
        assertEquals(0, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
    }

    @Test
    public void testEdgeAttributes()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\" }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"
                     + "  { \"source\":\"1\", \"target\": \"1\", \"label\": \"Label\", \"int\": 4, \"double\": 0.5, \"boolean\": true, \"boolean1\": false, \"novalue\": null }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true).weighted(false)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (id, attributes) -> {
            return id;
        };
        EdgeProvider<String, DefaultEdge> ep = (from, to, label, attributes) -> {
            if (from.equals("1") && to.equals("1")) {
                assertTrue(attributes.get("label").getType().equals(AttributeType.STRING));
                assertTrue(attributes.get("label").getValue().equals("Label"));
                assertTrue(attributes.get("int").getType().equals(AttributeType.INT));
                assertTrue(attributes.get("int").getValue().equals("4"));
                assertTrue(attributes.get("double").getType().equals(AttributeType.DOUBLE));
                assertTrue(attributes.get("double").getValue().equals("0.5"));
                assertTrue(attributes.get("boolean").getType().equals(AttributeType.BOOLEAN));
                assertTrue(attributes.get("boolean").getValue().equals("true"));
                assertTrue(attributes.get("boolean1").getType().equals(AttributeType.BOOLEAN));
                assertTrue(attributes.get("boolean1").getValue().equals("false"));
                assertTrue(attributes.get("novalue").getType().equals(AttributeType.NULL));
                assertTrue(attributes.get("novalue").getValue().equals("null"));
            }
            return g.getEdgeSupplier().get();
        };

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(1, g.vertexSet().size());
        assertEquals(1, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
    }

    @Test
    public void testNestedAttributes()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"
                     + "  { \"id\":\"1\", \"custom\": { \"pi\": 3.14 } },\n"            
                     + "  { \"id\":\"2\", \"array\": [ { \"obj\": 3.14 } ] }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"
                     + "  { \"source\":\"1\", \"target\": \"2\", \"array\": [ { \"key1\": 1 }, { \"key2\": 2 } ] },\n"
                     + "  { \"source\":\"2\", \"target\": \"1\", \"obj\": { \"key1\": [ { \"key1\": 1 }, { \"key2\": 2 } ] } }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true).weighted(false)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (id, attributes) -> {
            if (id.equals("1")) {
                assertTrue(attributes.get("custom").getType().equals(AttributeType.UNKNOWN));
                assertTrue(attributes.get("custom").getValue().equals("{\"pi\":3.14}"));
            } else if (id.equals("2")) {
                assertTrue(attributes.get("array").getType().equals(AttributeType.UNKNOWN));
                assertTrue(attributes.get("array").getValue().equals("[{\"obj\":3.14}]"));
            }
            return id;
        };
        EdgeProvider<String, DefaultEdge> ep = (from, to, label, attributes) -> {
            if (from.equals("1") && to.equals("2")) {
                assertTrue(attributes.get("array").getType().equals(AttributeType.UNKNOWN));
                assertTrue(
                    attributes.get("array").getValue().equals("[{\"key1\":1},{\"key2\":2}]"));
            } else if (from.equals("2") && to.equals("1")) {
                assertTrue(attributes.get("obj").getType().equals(AttributeType.UNKNOWN));
                assertTrue(
                    attributes
                        .get("obj").getValue().equals("{\"key1\":[{\"key1\":1},{\"key2\":2}]}"));
            }
            return g.getEdgeSupplier().get();
        };

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(2, g.vertexSet().size());
        assertEquals(2, g.edgeSet().size());
        assertTrue(g.containsVertex("1"));
        assertTrue(g.containsVertex("2"));
    }

    @Test
    public void testSingletons()
        throws ImportException
    {
        // @formatter:off
        String input = "{\n"
                     + "  \"nodes\": [\n"    
                     + "  { \"id\":\"1\" },\n"
                     + "  { \"id\":\"2\" },\n"
                     + "  { },\n"
                     + "  { }\n"
                     + "  ],\n"
                     + "  \"edges\": [\n"    
                     + "  { \"source\":\"1\", \"target\":\"2\" }\n"
                     + "  ]\n"
                     + "}";
        // @formatter:on

        Graph<String,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createStringSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        VertexProvider<String> vp = (label, attributes) -> label;
        EdgeProvider<String, DefaultEdge> ep =
            (from, to, label, attributes) -> g.getEdgeSupplier().get();

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>(vp, ep);
        importer.importGraph(g, new StringReader(input));

        assertEquals(4, g.vertexSet().size());
        assertEquals(1, g.edgeSet().size());

    }

}
