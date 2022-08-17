package cs;

import cs.qse.Parser;
import cs.qse.sampling.ReservoirSamplingParser;
import cs.qse.endpoint.EndpointParser;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.PrecisionRecallComputer;
import cs.utils.Utils;


public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    public static int numberOfInstances;
    public static int entitySamplingThreshold;
    public static int entitySamplingTargetPercentage;
    public static boolean extractMaxCardConstraints;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        extractMaxCardConstraints = isActivated("max_cardinality");
        entitySamplingThreshold = Integer.parseInt(ConfigManager.getProperty("entitySamplingThreshold"));
        entitySamplingTargetPercentage = Integer.parseInt(ConfigManager.getProperty("entitySamplingTargetPercentage"));
        execute();
    }
    
    private static void execute(){
        benchmark();
        //new PrecisionRecallComputer();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated for " + ConfigManager.getProperty("dataset_path"));
        Utils.log("Dataset,Method,Second,Minute,SecondTotal,MinuteTotal,MaxCard,DatasetPath");
        Utils.getCurrentTimeStamp();
        try {
            String typeProperty = Constants.RDF_TYPE;
            if (isActivated("isWikiData")) {
                typeProperty = Constants.INSTANCE_OF;
            }
            
            if (isActivated("qse_exact_file")) {
                Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, typeProperty);
                parser.run();
            }
            
            if (isActivated("qse_approximate_file")) {
                ReservoirSamplingParser reservoirSamplingParser = new ReservoirSamplingParser(datasetPath, numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold);
                reservoirSamplingParser.run();
            }
            
            if (isActivated("qse_exact_query_based")) {
                EndpointParser endpointParser = new EndpointParser();
                endpointParser.run();
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
