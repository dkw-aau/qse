package cs;

import cs.others.parsers.mg.MgSchemaExtractor;
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
        extractMaxCardConstraints = isActivated("EXTRACT_MAX_CARDINALITY");
        entitySamplingThreshold = Integer.parseInt(ConfigManager.getProperty("entitySamplingThreshold"));
        entitySamplingTargetPercentage = Integer.parseInt(ConfigManager.getProperty("entitySamplingTargetPercentage"));
        benchmark();
        new PrecisionRecallComputer();
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
                
                //RandomSamplingParser rsp = new RandomSamplingParser(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE);
                //rsp.run();
                
                ReservoirSamplingParser reservoirSamplingParser = new ReservoirSamplingParser(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE, entitySamplingThreshold);
                reservoirSamplingParser.run();
                
                /*MemoryTest parser = new MemoryTest(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE);
                parser.run();*/
            }
            
            if (isActivated("QSE_Endpoint")) {
                System.out.println("QSE over Endpoint");
                EndpointParser endpointParser = new EndpointParser();
                endpointParser.run();
            }
            
            if (isActivated("QSE_Wikidata")) {
                System.out.println("QSE over File -specific to WikiData");
                
                /*Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF);
                parser.run();*/
    
                /*MemoryTest parser = new MemoryTest(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF);
                parser.run();*/
                
                
                //Random Sampling
                /*RandomSamplingParser rsp = new RandomSamplingParser(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF, entitySamplingThreshold);
                rsp.run();*/
                
                //Reservoir Sampling
                ReservoirSamplingParser reservoirSamplingParser = new ReservoirSamplingParser(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF, entitySamplingThreshold);
                reservoirSamplingParser.run();
    
                /*MemoryTest parser = new MemoryTest(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF);
                parser.run();*/
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
