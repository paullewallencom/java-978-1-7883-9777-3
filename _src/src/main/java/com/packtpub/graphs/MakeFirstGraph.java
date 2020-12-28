/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.packtpub.graphs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.janusgraph.core.JanusGraphFactory;

/**
 *
 * @author Tinkerpop documentation
 * @See http://tinkerpop.apache.org/docs/current/reference/#_the_graph_structure
 */
public class MakeFirstGraph {

    public static void main(String[] args) {

        Graph graph = TinkerGraph.open();
        
        Vertex marko = graph.addVertex(T.label, "person", T.id, 1, "name", "marko", "age", 29); //2
        Vertex vadas = graph.addVertex(T.label, "person", T.id, 2, "name", "vadas", "age", 27);
        Vertex lop = graph.addVertex(T.label, "software", T.id, 3, "name", "lop", "lang", "java");
        Vertex josh = graph.addVertex(T.label, "person", T.id, 4, "name", "josh", "age", 32);
        Vertex ripple = graph.addVertex(T.label, "software", T.id, 5, "name", "ripple", "lang", "java");
        Vertex peter = graph.addVertex(T.label, "person", T.id, 6, "name", "peter", "age", 35);
        marko.addEdge("knows", vadas, T.id, 7, "weight", 0.5f); //3
        marko.addEdge("knows", josh, T.id, 8, "weight", 1.0f);
        marko.addEdge("created", lop, T.id, 9, "weight", 0.4f);
        josh.addEdge("created", ripple, T.id, 10, "weight", 1.0f);
        josh.addEdge("created", lop, T.id, 11, "weight", 0.4f);
        peter.addEdge("created", lop, T.id, 12, "weight", 0.2f);

        try {
            graph.io(IoCore.graphml()).writeGraph("tinkerpop-modern.graphml");
            graph.io(IoCore.graphson()).writeGraph("tinkerpop-modern.json");
        } catch (IOException ex) {
            Logger.getLogger(MakeFirstGraph.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Now query the graph
        //@See http://tinkerpop.apache.org/docs/current/tutorials/getting-started/
        //Unlike SQL, Gremlin isn't a syntax so much as a chain of functions
        System.out.println("Querying Marko (vertex 1)");
        graph.traversal().V(1).outE().forEachRemaining(edge -> System.out.println("Edge[" + edge.id() + "] " + edge.label() + " " + edge.inVertex().value("name")));

        //For more advanced Gremlin @See http://sql2gremlin.com/
    }
    
    //The main difference with graph backend is not setting the IDs
    public static void janusmain(String[] args) {
        Graph graph = JanusGraphFactory.build().
                set("storage.backend", "berkeleyje").
                set("storage.directory", "target/graph").
                open();
        
        Vertex marko = graph.addVertex(T.label, "person", "name", "marko", "age", 29); //2
        Vertex vadas = graph.addVertex(T.label, "person", "name", "vadas", "age", 27);
        Vertex lop = graph.addVertex(T.label, "software", "name", "lop", "lang", "java");
        Vertex josh = graph.addVertex(T.label, "person", "name", "josh", "age", 32);
        Vertex ripple = graph.addVertex(T.label, "software", "name", "ripple", "lang", "java");
        Vertex peter = graph.addVertex(T.label, "person", "name", "peter", "age", 35);
        marko.addEdge("knows", vadas, "weight", 0.5f); //3
        marko.addEdge("knows", josh, "weight", 1.0f);
        marko.addEdge("created", lop, "weight", 0.4f);
        josh.addEdge("created", ripple, "weight", 1.0f);
        josh.addEdge("created", lop, "weight", 0.4f);
        peter.addEdge("created", lop, "weight", 0.2f);

        try {
            graph.io(IoCore.graphml()).writeGraph("tinkerpop-modern.graphml");
            graph.io(IoCore.graphson()).writeGraph("tinkerpop-modern.json");
        } catch (IOException ex) {
            Logger.getLogger(MakeFirstGraph.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Now query the graph
        //@See http://tinkerpop.apache.org/docs/current/tutorials/getting-started/
        //Unlike SQL, Gremlin isn't a syntax so much as a chain of functions
        System.out.println("Querying Marko (vertex " + marko.id() + ")");
        graph.traversal().V(marko.id()).outE().forEachRemaining(edge -> System.out.println("Edge[" + edge.id() + "] " + edge.label() + " " + edge.inVertex().value("name")));

        try {
            //For more advanced Gremlin @See http://sql2gremlin.com/
            graph.close();
        } catch (Exception ex) {
            Logger.getLogger(MakeFirstGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
