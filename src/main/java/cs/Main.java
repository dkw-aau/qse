package cs;

import cs.parsers.BaselineParser;
import cs.parsers.MemOptimalParser;
import cs.utils.ConfigManager;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        
        //BaselineParser baselineParser = new BaselineParser(datasetPath, numberOfClasses);
        //baselineParser.run();
        
        MemOptimalParser memOptimalParser = new MemOptimalParser(datasetPath, numberOfClasses);
        memOptimalParser.run();
    }
}
