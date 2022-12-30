package cs.utils;

import cs.Main;

/**
 * This class contains all the constants used globally throughout the project
 */
public class Constants {
    public static String SHAPES_NAMESPACE = "http://shaclshapes.org/";
    public static String SHACL_NAMESPACE = "http://www.w3.org/ns/shacl#";
    public static String MEMBERSHIP_GRAPH_ROOT_NODE = "<http://www.schema.hng.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.schema.hng.root#HNG_Root> .";
    public static String RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    public static String INSTANCE_OF = "<http://www.wikidata.org/prop/direct/P31>";
    public static String INSTANCE_OF_ = "<http://www.wikidata.org/prop/direct/P31>";
    public static String SUB_CLASS_OF = "<http://www.w3.org/2000/01/rdf-schema#subClassOf>";
    public static String OBJECT_UNDEFINED_TYPE = "http://shaclshapes.org/object-type/undefined";
    
    public static String CONFIDENCE = "http://shaclshapes.org/confidence";
    public static String SUPPORT = "http://shaclshapes.org/support";
    // some constant addresses of files
    public static String MG_VERTICES_FILE = Main.outputFilePath + "/" + Main.datasetName + "_" + "mg_vertices.csv";
    public static String MG_EDGES_FILE = Main.outputFilePath + "/" + Main.datasetName + "_" + "mg_edges.csv";
    public static String MG_ENCODED_TABLE_FILE = Main.outputFilePath + "/" + Main.datasetName + "_" + "mg_encoded_table.csv";
    public static String MG_ENCODED_R_TABLE_FILE = Main.outputFilePath + "/" + Main.datasetName + "_" + "mg_encoded_table_reverse.csv";
    public static String FILTERED_DATASET = Main.outputFilePath + "/" + Main.datasetName + "_" + "filtered.nt";
    public static String SUBCLASSOF_DATASET = Main.outputFilePath + "/" + Main.datasetName + "_" + "subclassOf.nt";
    
    public static String TEMP_DATASET_FILE = Main.outputFilePath + "/" + Main.datasetName + "-shape-props-stats.csv";
    public static String TEMP_DATASET_FILE_2 = Main.outputFilePath + "/" + "shape-props-extended-stats.csv";
    public static String TEMP_DATASET_FILE_3 = Main.outputFilePath + "/" + "shape-props-with-class-count-stats.csv";
    //
    public static String EXPERIMENTS_RESULT = Main.outputFilePath + Main.datasetName + ".csv";
    public static String EXPERIMENTS_RESULT_CUSTOM = Main.outputFilePath + Main.datasetName + "_stacked.csv";
    public static String EXPERIMENTS_RESULT_MIN_CARD = Main.outputFilePath + Main.datasetName + "_min_card.csv";
    
    public static String RUNTIME_LOGS = Main.outputFilePath + Main.datasetName + "_RUNTIME_LOGS.csv";
    public static String SAMPLING_LOGS = Main.outputFilePath + Main.datasetName + "_SAMPLING_LOGS.csv";
    
    
    public static String THE_LOGS = Main.outputFilePath + Main.datasetName + "_THE_LOGS.csv";
}
