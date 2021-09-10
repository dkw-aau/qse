package cs;

import cs.parsers.*;
import cs.trees.OntologyTreeExtractor;
import cs.utils.ConfigManager;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
    
        
        //System.out.println("BaselineParser");
        //new BaselineParser(datasetPath, numberOfClasses).run();
//        System.out.println("Baseline Algorithm A - Encoded Strings");
//        new BaselineParserEncoded(datasetPath, numberOfClasses).run();
//
//        System.out.println("\n\n\n\nBaseline Algorithm B - Bloom Filters");
//        new BaselineParserWithBloomFilters(datasetPath, numberOfClasses).run();
//
//        System.out.println("\n\n\n\n Optimal Algorithm - Bloom Filters - BFS - Membership Graph");
//        new MembershipGraphBasedParser(datasetPath, numberOfClasses).run();
        
        //new MembershipGraph(true);
        //new SmartTriplesFilterator(datasetPath).extractSubClassOfTriples();
        // If you want to extract Triples mapping to membership sets
        new SmartTriplesFilterator(datasetPath, numberOfClasses).run();
    
        //new OntologyTreeExtractor().doTheJob();
    }
}
