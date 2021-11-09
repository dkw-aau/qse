package cs.others.trees;

import cs.utils.Constants;
import cs.utils.graphdb.GraphDBUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

public class OntologyTreeExtractor {
    
    private GraphDBUtils graphDBUtils;
    private Integer count = 0;
    DefaultDirectedGraph<String, DefaultEdge> ontologyTree;
    
    public OntologyTreeExtractor() {
        this.graphDBUtils = new GraphDBUtils();
        this.ontologyTree = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
    }
    
    public void doTheJob() {
        String current = "http://schema.org/Thing";
        recursive(current);
    }
    
    private void recursive(String current) {
        try {
            if (containsSubClasses(current)) {
                FileWriter fileWriter = new FileWriter(Constants.TEMP_DATASET_FILE, true);
                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.println(current + "|" + this.count);
                printWriter.close();
                printSubClassTriplesInFile(current);
                getSubClassesSet(current).forEach(this::recursive);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private HashSet<String> getSubClassesSet(String theClass) {
        HashSet<String> subClassList = new HashSet<>();
        graphDBUtils.runSelectQuery(buildQuery(theClass)).forEach(result -> {
            subClassList.add(result.getValue("subclass").stringValue());
        });
        return subClassList;
    }
    
    private boolean containsSubClasses(String theClass) {
        this.count = graphDBUtils.runSelectQuery(buildQuery(theClass)).size();
        return this.count > 0;
    }
    
    public void printSubClassTriplesInFile(String theClass) {
        graphDBUtils.runConstructQuery(buildConstructSubClassQuery(theClass), Constants.SUBCLASSOF_DATASET);
    }
    
    private String buildQuery(String focusClass) {
        return "  PREFIX onto: <http://www.ontotext.com/>" +
                " SELECT DISTINCT ?subclass FROM onto:explicit" +
                " WHERE { \n" +
                "    ?subclass rdfs:subClassOf <" + focusClass + ">.\n" +
                "} ";
    }
    
    private String buildConstructSubClassQuery(String focusClass) {
        return "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX onto: <http://www.ontotext.com/>" +
                "CONSTRUCT\n" +
                "FROM onto:explicit\n" +
                "WHERE {\n" +
                "    ?subclass rdfs:subClassOf <" + focusClass + "> .\n" +
                "}";
    }
    
    private void buildGraph(String node, HashSet<String> children) {
        //this.ontologyTree.addVertex(node);
        children.forEach(child -> {
            if (!ontologyTree.containsVertex(child)) {
                this.ontologyTree.addVertex(child);
                this.ontologyTree.addEdge(child, node);
            }
        });
    }
}
