package cs;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import cs.extras.ExtractTaxonomy;
import cs.extras.RDFVault;
import cs.extras.VariousFileReadingApproaches;
import cs.parsers.*;
import cs.trees.BlParserWithTrees;
import cs.utils.ConfigManager;

import java.util.NavigableMap;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        
        //new BaselineParser(datasetPath, numberOfClasses).run();
        
        System.out.println("BaselineParserWithBloomFilters");
        new BaselineParserWithBloomFilters(datasetPath, numberOfClasses).run();
        
        System.out.println("BLParserWithBloomFilterLFRUCache");
        new BLParserWithBloomFilterLFRUCache(datasetPath, numberOfClasses).run();
        
        System.out.println("BaselineParserEncoded");
        new BaselineParserEncoded(datasetPath, numberOfClasses).run();
        
    }
}
