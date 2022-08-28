package cs;

import cs.qse.Parser;
import cs.qse.endpoint.EndpointSampling;
import cs.qse.sampling.ReservoirSamplingParser;
import cs.qse.endpoint.EndpointParser;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.PrecisionRecallComputer;
import cs.utils.Utils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cs.utils.graphdb.ExampleQueryExecutor;
import org.slf4j.LoggerFactory;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    public static int numberOfInstances;
    public static int entitySamplingThreshold;
    public static int entitySamplingTargetPercentage;
    public static boolean extractMaxCardConstraints;
    public static boolean isWikiData;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        extractMaxCardConstraints = isActivated("max_cardinality");
        isWikiData = isActivated("isWikiData");
        entitySamplingThreshold = Integer.parseInt(ConfigManager.getProperty("entitySamplingThreshold"));
        entitySamplingTargetPercentage = Integer.parseInt(ConfigManager.getProperty("entitySamplingTargetPercentage"));
        execute();
    }
    
    private static void execute(){
        benchmark();
        //new PrecisionRecallComputer();
        //new ExampleQueryExecutor().runQuery();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated for " + ConfigManager.getProperty("dataset_path"));
        Utils.log("Dataset,Method,Second,Minute,SecondTotal,MinuteTotal,MaxCard,DatasetPath");
        Utils.getCurrentTimeStamp();
        try {
            String typeProperty = Constants.RDF_TYPE;
            if (isWikiData) {
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
                //EndpointParser endpointParser = new EndpointParser(typeProperty);
                //endpointParser.run();
                EndpointSampling endpointSampling =  new EndpointSampling(numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold);
                endpointSampling.run();
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
