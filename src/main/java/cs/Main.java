package cs;

import cs.qse.filebased.Parser;
import cs.qse.querybased.nonsampling.QbParser;
import cs.qse.querybased.sampling.QbSampling;
import cs.qse.querybased.sampling.parallel.ParallelQbSampling;
import cs.qse.filebased.sampling.ReservoirSamplingParser;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.Utils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
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
        
        datasetPath = paramVal("dataset_path");
        numberOfClasses = Integer.parseInt(paramVal("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(paramVal("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        extractMaxCardConstraints = isActivated("max_cardinality");
        isWikiData = isActivated("isWikiData");
        entitySamplingThreshold = Integer.parseInt(paramVal("entitySamplingThreshold"));
        entitySamplingTargetPercentage = Integer.parseInt(paramVal("entitySamplingTargetPercentage"));
        entitySamplingThreshold = Integer.parseInt(paramVal("entitySamplingThreshold"));
        
        execute();
    }
    
    private static void execute() {
        benchmark();
        //new PrecisionRecallComputer();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated for " + paramVal("dataset_path"));
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
                QbParser qbParser = new QbParser(typeProperty);
                qbParser.run();
            }
            
            if (isActivated("qse_approximate_query_based")) {
                QbSampling qbSampling = new QbSampling(numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold);
                qbSampling.run();
            }
            
            if (isActivated("qse_approximate_parallel_query_based")) {
                int numOfThreads = Integer.parseInt(paramVal("qse_approximate_parallel_qb_threads"));
                ParallelQbSampling parallelQbSampling = new ParallelQbSampling(numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold, numOfThreads);
                parallelQbSampling.run();
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        Utils.getCurrentTimeStamp();
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
    
    private static String paramVal(String prop) {return ConfigManager.getProperty(prop);}
}
