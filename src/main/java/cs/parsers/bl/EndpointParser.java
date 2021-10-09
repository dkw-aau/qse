package cs.parsers.bl;

import cs.utils.ConfigManager;
import org.semanticweb.yars.nx.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

public class EndpointParser {
    HashMap<Node, HashSet<Integer>> instanceToClass;
    
    public EndpointParser() {}
    
    public void readQueries() {
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/queries/";
            String content = new String(Files.readAllBytes(Paths.get(queriesDirectory + "query1.txt")));
            System.out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    //first Pass Ingredients
    
    
    //2nd Pass Ingredients
}
