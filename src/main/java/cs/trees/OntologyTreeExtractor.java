package cs.trees;

import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.MembershipGraphVisualizer;
import cs.utils.graphdb.GraphDBUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class OntologyTreeExtractor {
    
    private GraphDBUtils graphDBUtils;
    DefaultDirectedGraph<String, DefaultEdge> ontologyTree;
    
    public OntologyTreeExtractor() {
        this.graphDBUtils = new GraphDBUtils();
        this.ontologyTree = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
    }
    
    public void doTheJob() {
        String rootClass = "http://schema.org/Thing";
        String queryToExtractSubClasses = buildQuery(rootClass);
        //System.out.println(queryToExtractSubClasses);
        
        HashSet<String> subClassList = new HashSet<>();
        
        graphDBUtils.runSelectQuery(queryToExtractSubClasses).forEach(result -> {
            subClassList.add(result.getValue("subclass").stringValue());
        });
        
        System.out.println("Size of subclasses of Things = " + subClassList.size());
        this.ontologyTree.addVertex(rootClass);
        buildGraph(rootClass, subClassList);
        
        FilesUtil.writeToFileInAppendMode(rootClass + "|" + subClassList.size(), Constants.SUBCLASSOF_DATASET);
        subClassList.forEach(item -> {
            HashSet<String> subSubClassList = new HashSet<>();
            graphDBUtils.runSelectQuery(buildQuery(item)).forEach(result -> {
                subSubClassList.add(result.getValue("subclass").stringValue());
            });
            System.out.println("SubClasses of " +item + " contains " + subSubClassList.size() + " items");
            FilesUtil.writeToFileInAppendMode(item + "|" + subSubClassList.size(), Constants.SUBCLASSOF_DATASET);
            buildGraph(item, subSubClassList);
        });

        
        //Plot
        System.out.println("Plotting");
        //new MembershipGraphVisualizer().createStringNodesGraph(this.ontologyTree);
        
    }
    
    private String buildQuery(String focusClass) {
        return "  PREFIX onto: <http://www.ontotext.com/>" +
                " SELECT DISTINCT ?subclass FROM onto:explicit" +
                " WHERE { \n" +
                "    ?subclass rdfs:subClassOf <" + focusClass + ">.\n" +
                "} ";
    }
    
    private void buildGraph(String node, HashSet<String> children) {
        //this.ontologyTree.addVertex(node);
        children.forEach(child -> {
            if(!ontologyTree.containsVertex(child)){
                this.ontologyTree.addVertex(child);
                this.ontologyTree.addEdge(child, node);
            }
        });
    }
    
}
