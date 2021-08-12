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
        
        //new BaselineParserEncoded(datasetPath, numberOfClasses).run();
        new BLParserWithBloomFiltersAndBFS(datasetPath, numberOfClasses).run();
        //new BaselineParser(datasetPath, numberOfClasses).run();
    }
}
