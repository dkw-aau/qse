package cs.utils;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used to compute degree distribution of the graph constructed using jgraphT library
 */
public class DegreeDistribution {
    DefaultDirectedGraph<Integer, DefaultEdge> directedGraph;
    List<Integer> degrees = new ArrayList<>();
    List<Integer> inDegrees = new ArrayList<>();
    List<Integer> outDegrees = new ArrayList<>();
    
    
    public DegreeDistribution(DefaultDirectedGraph<Integer, DefaultEdge> d) {
        this.directedGraph = d;
        directedGraph.vertexSet().forEach(v -> {
            degrees.add(directedGraph.degreeOf(v));
            inDegrees.add(directedGraph.inDegreeOf(v));
            outDegrees.add(directedGraph.outDegreeOf(v));
            //FilesUtil.writeToFileInAppendMode(v + "," + directedGraph.degreeOf(v), ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "degreeDistribution.csv");
        });
    }
    
    public void computeDegreeDistribution() {
        HashMap<Integer, Integer> dd = new HashMap<>();
        degrees.forEach(degree -> {
            int counter = 0;
            for (Integer v : directedGraph.vertexSet()) {
                if (directedGraph.degreeOf(v) == degree) {
                    counter++;
                }
            }
            dd.put(degree, counter);
        });
    }
    
    
    public Integer getNodeWithDegree(Integer d) {
        Integer vertex = 0;
        for (Integer v : directedGraph.vertexSet()) {
            if (directedGraph.degreeOf(v) == d) {
                vertex = v;
            }
        }
        return vertex;
    }
    
    public void computeInDegreeDistribution() {
        HashMap<Integer, Integer> inDd = new HashMap<>();
        inDegrees.forEach(inDegree -> {
            int counter = 0;
            for (Integer v : directedGraph.vertexSet()) {
                if (directedGraph.inDegreeOf(v) == inDegree) {
                    counter++;
                }
            }
            inDd.put(inDegree, counter);
        });
    }
    
    public void computeOutDegreeDistribution() {
        HashMap<Integer, Integer> outDd = new HashMap<>();
        outDegrees.forEach(outDegree -> {
            int counter = 0;
            for (Integer v : directedGraph.vertexSet()) {
                if (directedGraph.outDegreeOf(v) == outDegree) {
                    counter++;
                }
            }
            outDd.put(outDegree, counter);
        });
    }
}
