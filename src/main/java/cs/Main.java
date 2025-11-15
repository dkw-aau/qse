package cs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cs.qse.filebased.Parser;
import cs.qse.filebased.sampling.ReservoirSamplingParser;
import cs.qse.querybased.nonsampling.QbParser;
import cs.qse.querybased.sampling.QbSampling;
import cs.qse.querybased.sampling.parallel.ParallelQbSampling;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.Utils;
import cs.validation.QseSHACLValidator;
import org.apache.jena.base.Sys;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static String datasetName;
    public static String outputFilePath;
    public static int numberOfClasses;
    public static int numberOfInstances;
    public static int entitySamplingThreshold;
    public static int entitySamplingTargetPercentage;
    public static boolean extractMaxCardConstraints;
    public static boolean isWikiData;
    public static boolean qseFromSpecificClasses;
    /** Indicates whether or not to extend the generated shapes with examples **/
    public static boolean addExamples;
    /** List of all predicates used to identify labels **/
    public static List<String> labelProperties;
    /** IRI used as predicate to indicate examples in Shapes and PropertyShapes **/
    public static String exampleIRI;


    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        //qseExactExecutionWithMinimumParams();

        // N.B. de-commented to use complete configuration files
        readConfig();
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
                Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, typeProperty, addExamples, labelProperties);
                parser.run();
            }
            
            if (isActivated("qse_approximate_file")) {
                ReservoirSamplingParser reservoirSamplingParser = new ReservoirSamplingParser(datasetPath, numberOfClasses, numberOfInstances, typeProperty, addExamples, labelProperties, entitySamplingThreshold);
                reservoirSamplingParser.run();
            }


            if (isActivated("qse_exact_query_based")) {
                // Can't add examples with query-based parsers TODO add examples with query-based parsers
                if (!addExamples) {
                    QbParser qbParser = new QbParser(typeProperty);
                    qbParser.run();
                }
                else {
                    System.err.println("ERROR: cannot add examples with a query-based parser. Please disable 'add_examples' property in your properties file");
                }
            }

            if (isActivated("qse_approximate_query_based")) {
                // Can't add examples with query-based parsers TODO add examples with query-based parsers
                if (!addExamples) {
                    QbSampling qbSampling = new QbSampling(numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold);
                    qbSampling.run();
                }
                else {
                    System.err.println("ERROR: cannot add examples with a query-based parser. Please disable 'add_examples' property in your properties file");
                }
            }

            if (isActivated("qse_approximate_parallel_query_based")) {
                // Can't add examples with query-based parsers TODO add examples with query-based parsers
                if (!addExamples) {
                    int numOfThreads = Integer.parseInt(paramVal("qse_approximate_parallel_qb_threads"));
                    ParallelQbSampling parallelQbSampling = new ParallelQbSampling(numberOfClasses, numberOfInstances, typeProperty, entitySamplingThreshold, numOfThreads);
                    parallelQbSampling.run();
                }
                else {
                    System.err.println("ERROR: cannot add examples with a query-based parser. Please disable 'add_examples' property in your properties file");
                }
            }


            if (isActivated("qse_validation")) {
                new QseSHACLValidator(true);
            }
            
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        Utils.getCurrentTimeStamp();
    }
    
    private static void readConfig() {
        datasetPath = paramVal("dataset_path");
        datasetName = paramVal("dataset_name");
        numberOfClasses = Integer.parseInt(paramVal("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(paramVal("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        extractMaxCardConstraints = isActivated("max_cardinality");
        isWikiData = isActivated("is_wikidata");
        entitySamplingThreshold = Integer.parseInt(paramVal("entity_sampling_threshold"));
        entitySamplingTargetPercentage = Integer.parseInt(paramVal("entity_sampling_target_percentage"));
        qseFromSpecificClasses = isActivated("qse_specific_classes");
        outputFilePath = paramVal("output_file_path");
        addExamples = isActivated("add_examples");
        exampleIRI = paramVal("example_IRI");
        // Read all possible predicates used for labels
        labelProperties = readLabelProperties();
    }
    
    private static void qseExactExecutionWithMinimumParams() {
        datasetPath = paramVal("dataset_path");
        datasetName = FilesUtil.getFileName(datasetPath);
        numberOfClasses = Integer.parseInt(paramVal("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(paramVal("expected_number_of_lines")) / 2; // expected or estimated numberOfInstances
        extractMaxCardConstraints = false;
        isWikiData = false;
        qseFromSpecificClasses = isActivated("qse_specific_classes");
        outputFilePath = paramVal("output_file_path");
        addExamples = isActivated("add_examples");
        exampleIRI = paramVal("example_IRI");
        // Read all possible predicates used for labels
        labelProperties = readLabelProperties();

        // Run QSE-Exact
        Parser parser = new Parser(datasetPath, numberOfClasses, numberOfInstances, paramVal("instance_type_property"), addExamples, labelProperties);
        parser.run();
    }
    
    private static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
    
    private static String paramVal(String prop) {return ConfigManager.getProperty(prop);}

    // Read all possible predicates used to identify labels
    private static List<String> readLabelProperties() {
        // Reading raw property value
        String rawLabelProperties = paramVal("label_properties");
        List<String> labelProperties;
        if (rawLabelProperties != null) {
            // Separating single predicates contained in the raw string
            labelProperties = new ArrayList<>(List.of(rawLabelProperties.split(",")));
        }
        else {
            labelProperties = new ArrayList<>();
        }

        return labelProperties;
    }
    /*-------------------------------------------------------------------*/
}
