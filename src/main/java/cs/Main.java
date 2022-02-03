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
    public static boolean extractMaxCardConstraints;
    public static String instantiation_property;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        instantiation_property = ConfigManager.getProperty("instantiation_property");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        //extractMaxCardConstraints = isActivated("EXTRACT_MAX_CARDINALITY");
        benchmark();
    }
    
    private static void benchmark() {
        System.out.println("Extraction Initiated for " + ConfigManager.getProperty("dataset_path"));
        //Utils.log("Dataset,Method,Second,Minute,SecondTotal,MinuteTotal,MaxCard,DatasetPath");
        Utils.getCurrentTimeStamp();
        try {
            Parser parser;
            if (instantiation_property != null) {
                System.out.println("Using custom instantiation_property : " + instantiation_property);
                parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, instantiation_property);
            } else {
                System.out.println("Using rdf:type as instantiation_property");
                parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE);
            }
            parser.run();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
