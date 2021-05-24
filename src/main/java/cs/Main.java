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
        
        new BaselineParserEncoded(datasetPath, numberOfClasses).run();
        //new BLParserWithSupport(datasetPath, numberOfClasses).run();
    }
}
