package cs;

import cs.qse.endpoint.EndpointParser;
import cs.qse.Parser;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.Utils;


public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    public static int numberOfInstances;
    public static boolean extractMaxCardConstraints = false;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        benchmark();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated for " + ConfigManager.getProperty("dataset_path"));
        Utils.log("Dataset,Method,Second,Minute,SecondTotal,MinuteTotal,MaxCard,DatasetPath");
        Utils.getCurrentTimeStamp();
        try {
            if (isActivated("QSE_File")) {
                System.out.println("QSE over File");
                Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE);
                parser.run();
            }
            
            if (isActivated("QSE_Endpoint")) {
                System.out.println("QSE over Endpoint");
                EndpointParser endpointParser = new EndpointParser();
                endpointParser.run();
            }
            
            if (isActivated("QSE_Wikidata")) {
                System.out.println("QSE over File -specific to WikiData");
                Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF);
                parser.run();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
