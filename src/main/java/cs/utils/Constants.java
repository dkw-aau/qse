package cs.utils;

/**
 * This class contains all the constants used globally throughout the project
 */
public class Constants {
    public static String SHAPES_NAMESPACE = "http://shaclshapes.org/";
    public static String MEMBERSHIP_GRAPH_ROOT_NODE = "<http://www.schema.hng.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.schema.hng.root#HNG_Root> .";
    public static String RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    public static String INSTANCE_OF = "<http://www.wikidata.org/prop/direct/P31>";
    public static String INSTANCE_OF_ = "<http://www.wikidata.org/prop/direct/P31>";
    public static String SUB_CLASS_OF = "<http://www.w3.org/2000/01/rdf-schema#subClassOf>";
    
    // some constant addresses of files
    public static String MG_VERTICES_FILE = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "mg_vertices.csv";
    public static String MG_EDGES_FILE = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "mg_edges.csv";
    public static String MG_ENCODED_TABLE_FILE = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "mg_encoded_table.csv";
    public static String MG_ENCODED_R_TABLE_FILE = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "mg_encoded_table_reverse.csv";
    public static String FILTERED_DATASET = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "filtered.nt";
    public static String SUBCLASSOF_DATASET = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "subclassOf.nt";
    
    public static String TEMP_DATASET_FILE = ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "-shape-props-stats.csv";
    public static String TEMP_DATASET_FILE_2 = ConfigManager.getProperty("output_file_path") + "/" + "shape-props-extended-stats.csv";
    public static String TEMP_DATASET_FILE_3 = ConfigManager.getProperty("output_file_path") + "/" + "shape-props-with-class-count-stats.csv";
    //
    public static String EXPERIMENTS_RESULT = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + ".csv";
    public static String EXPERIMENTS_RESULT_CUSTOM = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + "_stacked.csv";
    public static String EXPERIMENTS_RESULT_MIN_CARD = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + "_min_card.csv";
    
    public static String RUNTIME_LOGS = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + "_RUNTIME_LOGS.csv";
    public static String SAMPLING_LOGS = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + "_SAMPLING_LOGS.csv";
    
    
    public static String THE_LOGS = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + "_THE_LOGS.csv";
}
