package cs;

import cs.parsers.*;
import cs.utils.ConfigManager;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
    
        /*
        System.out.println("BaselineParser");
        new BaselineParser(datasetPath, numberOfClasses).run();
        System.out.println("BaselineParserEncoded");
        new BaselineParserEncoded(datasetPath, numberOfClasses).run();
        System.out.println("BaselineParserWithBloomFilters");
        new BaselineParserWithBloomFilters(datasetPath, numberOfClasses).run();
        System.out.println("BaselineParserWithBloomFilterCache");
        new BaselineParserWithBloomFilterCache(datasetPath, numberOfClasses).run();*/
        System.out.println("BLParserWithBloomFiltersAndBFS");
        new BLParserWithBloomFiltersAndBFS(datasetPath, numberOfClasses).run();
    }
}
