package cs.utils;

import cs.extras.Neo4jGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.LinkedList;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class MembershipGraphVisualizer {
    public MembershipGraphVisualizer() {}
    
    public void createIntegerNodesGraph(DefaultDirectedGraph<Integer, DefaultEdge> directedGraph) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(vertex -> {
            neo.addNode(String.valueOf(vertex));
        });
        directedGraph.iterables().edges().forEach(edge -> {
            int source = directedGraph.getEdgeTarget(edge);
            int target = directedGraph.getEdgeSource(edge);
            neo.connectNodes(String.valueOf(source), String.valueOf(target));
        });
    }
    
    public void createStringNodesGraph(DefaultDirectedGraph<String, DefaultEdge> directedGraph) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(neo::addNode);
        directedGraph.iterables().edges().forEach(edge -> {
            String source = directedGraph.getEdgeTarget(edge);
            String target = directedGraph.getEdgeSource(edge);
            neo.connectNodes(source, target);
        });
    }
    
    
    public void createEncodedIRIsNodesGraph(DefaultDirectedGraph<Integer, DefaultEdge> directedGraph, NodeEncoder encoder) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(vertex -> {
            neo.addNode(encoder.decode(vertex).getLabel());
        });
        directedGraph.iterables().edges().forEach(edge -> {
            int source = directedGraph.getEdgeTarget(edge);
            int target = directedGraph.getEdgeSource(edge);
            neo.connectNodes(encoder.decode(source).getLabel(), encoder.decode(target).getLabel());
        });
    }
    
    public void createEncodedShortenIRIsNodesGraph(DefaultDirectedGraph<Integer, DefaultEdge> directedGraph, NodeEncoder encoder) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(vertex -> {
            neo.addNode(iri(encoder.decode(vertex).getLabel()).getLocalName());
        });
        directedGraph.iterables().edges().forEach(edge -> {
            int source = directedGraph.getEdgeTarget(edge);
            int target = directedGraph.getEdgeSource(edge);
            //neo.connectNodes(iri(encoder.decode(source).getLabel()).getLocalName(), iri(encoder.decode(target).getLabel()).getLocalName());
            neo.connectNodes(iri(encoder.decode(target).getLabel()).getLocalName(), iri(encoder.decode(source).getLabel()).getLocalName());
        });
    }
    
    public void createBfsTraversedEncodedIRIsNodesGraph(DefaultDirectedGraph<Integer, DefaultEdge> directedGraph, NodeEncoder encoder, Integer hng_root) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(vertex -> {
            neo.addNode(encoder.decode(vertex).getLabel());
        });
        
        HashSet<Integer> visited = new HashSet<>();
        LinkedList<Integer> queue = new LinkedList<Integer>();
        int node = hng_root;
        queue.add(node);
        visited.add(node);
        while (queue.size() != 0) {
            node = queue.poll();
            int finalNode = node;
            for (DefaultEdge edge : directedGraph.outgoingEdgesOf(node)) {
                Integer child = directedGraph.getEdgeTarget(edge);
                if (!visited.contains(child)) {
                    neo.connectNodes(encoder.decode(finalNode).getLabel(), encoder.decode(child).getLabel());
                    queue.add(child);
                    visited.add(child);
                }
            }
        }
    }
    
    
    public void createBfsTraversedEncodedShortenIRIsNodesGraph(DefaultDirectedGraph<Integer, DefaultEdge> directedGraph, NodeEncoder encoder, Integer hng_root) {
        Neo4jGraph neo = new Neo4jGraph();
        directedGraph.iterables().vertices().forEach(vertex -> {
            //neo.addNode(encoder.decode(vertex).getLabel());
            neo.addNode(iri(encoder.decode(vertex).getLabel()).getLocalName());
        });
        
        HashSet<Integer> visited = new HashSet<>();
        LinkedList<Integer> queue = new LinkedList<Integer>();
        int node = hng_root;
        queue.add(node);
        visited.add(node);
        while (queue.size() != 0) {
            node = queue.poll();
            int finalNode = node;
            for (DefaultEdge edge : directedGraph.outgoingEdgesOf(node)) {
                Integer child = directedGraph.getEdgeTarget(edge);
                if (!visited.contains(child)) {
                    //neo.connectNodes(encoder.decode(finalNode).getLabel(), encoder.decode(child).getLabel());
                    neo.connectNodes(iri(encoder.decode(finalNode).getLabel()).getLocalName(), iri(encoder.decode(child).getLabel()).getLocalName());
                    queue.add(child);
                    visited.add(child);
                }
            }
        }
    }
    
}
