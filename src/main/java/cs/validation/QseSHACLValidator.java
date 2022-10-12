package cs.validation;

import cs.utils.ConfigManager;
import cs.utils.FilesUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.VLib;
import org.apache.jena.shacl.validation.ValidationListener;
import org.apache.jena.shacl.validation.event.ValidationEvent;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.util.*;

/**
 * This class allows to validate given shacl shapes against provided data graph.
 * It allows standalone running with specified files as well
 * It has some other functionalities to play with shapes and custom validator events
 */
public class QseSHACLValidator {
    
    public QseSHACLValidator() {}
    
    public QseSHACLValidator(Boolean flag) {
        System.out.println("Invoked::QseSHACLValidator");
        prepareValidation();
    }
    
    public void prepareValidation() {
        String dataFilePath = ConfigManager.getProperty("dataset_path"); // Get the data file path
        String inputFilesDirPath = ConfigManager.getProperty("validation_input_dir"); //Specify directory containing SHACL files
        
        assert inputFilesDirPath != null;
        File outputDir = new File(inputFilesDirPath + "/Output");
        
        if (!outputDir.exists())
            if (outputDir.mkdir())
                System.out.println(outputDir.getAbsoluteFile() + " created successfully.");
            else System.out.println("WARNING::directory creation failed");
        
        File inputDir = new File(inputFilesDirPath);
        for (final File fileEntry : Objects.requireNonNull(inputDir.listFiles())) {
            if (!fileEntry.isDirectory() && !fileEntry.isHidden()) {
                System.out.println(fileEntry.getName());
                String outputFilePath = outputDir.getAbsolutePath() + "/" + FilenameUtils.removeExtension(fileEntry.getName()) + "_Validation";
                System.out.println("Validating data graph using " + fileEntry.getName());
                validate(dataFilePath, fileEntry.getAbsolutePath(), outputFilePath + ".ttl", outputFilePath);
            }
        }
    }
    
    private static void validate(String inputDataFilePath, String inputSHACLFilePath, String outputSHACLFilePath, String outputCSVFilePath) {
        try {
            Graph shapesGraph = RDFDataMgr.loadGraph(inputSHACLFilePath);
            Graph dataGraph = RDFDataMgr.loadGraph(inputDataFilePath);
            Shapes shapes = Shapes.parse(shapesGraph);
            
            System.out.println("Write validation report as output!");
            //Write validation report as TTL Output
            OutputStream out = new FileOutputStream(outputSHACLFilePath, false);
            RDFDataMgr.write(out, ShaclValidator.get().validate(shapes, dataGraph).getModel(), Lang.TTL);
            out.close();
            
            RepositoryConnection conn = readFileAsRdf4JModel(inputSHACLFilePath);
            
            //Prepare a CSV file for constraints other than NotConstraints Components
            FileWriter fileWriter = new FileWriter(outputCSVFilePath + ".csv", true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            //Prepare a CSV file for NotConstraints Component
            FileWriter fileWriterNotConstraints = new FileWriter(outputCSVFilePath + "_NotConstraints.csv", true);
            PrintWriter printWriterNotConstraints = new PrintWriter(fileWriterNotConstraints);
            
            //Prepare headers for both files
            String header = "SourceShape|FocusNode|ResultPath|Value|SourceConstraintComponent|Message";
            String headerNotConstraints = "SourceShape|FocusNode|ResultPath|property|support|confidence|class";
            
            //Write headers for both files
            printWriter.println(header);
            printWriterNotConstraints.println(headerNotConstraints);
    
            System.out.println("Iterating over report entries");
            //Iterate over output report entries
            ShaclValidator.get().validate(shapes, dataGraph).getEntries().forEach(re -> {
                if (re.sourceConstraintComponent().getLocalName().equals("NotConstraintComponent")) {
                    // focusNode and value are same for not constraint component
                    String line = re.source() + "|" + re.focusNode() + "|" + re.resultPath();
                    //This query is for property shapes having only one sh:class value
                    String queryResult = executeQueryForOneClassType(conn, buildQuery(extractShNotShapeIri(re.message())));
                    
                    if (Objects.equals(queryResult, "")) {
                        //This query is for property shapes having more than one sh:class value (using sh:or )
                        List<String> outputLines = executeQueryForMultipleClassType(conn, buildQueryForMultipleClassTypesPs(extractShNotShapeIri(re.message())));
                        for (String result : outputLines) {
                            String x = line + "|" + result;
                            printWriterNotConstraints.println(x);
                        }
                    } else {
                        line += "|" + queryResult;
                        printWriterNotConstraints.println(line);
                    }
                } else {
                    String line = re.source() + "|" + re.focusNode() + "|" + re.resultPath() + "|" + re.value() + "|" + re.sourceConstraintComponent() + "|" + re.message();
                    printWriter.println(line);
                }
            });
            System.out.println("Closing files.");
            printWriterNotConstraints.close();
            printWriter.close();
            
            //close the connection
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String extractShNotShapeIri(String message) {
        //Not[NodeShape[http://shaclshapes.org/heightSong_PS_NotShape]] at focusNode <http://dbpedia.org/resource/Otome_Pasta_ni_Kand>
        return message.replace("Not[NodeShape[", "").replace("]]", "").split(" ")[0];
    }
    
    private static RepositoryConnection readFileAsRdf4JModel(String inputFileAddress) {
        RepositoryConnection conn = null;
        try {
            InputStream input = new FileInputStream(inputFileAddress);
            Model model = Rio.parse(input, "", RDFFormat.TURTLE);
            Repository db = new SailRepository(new MemoryStore());
            conn = db.getConnection();
            conn.add(model);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
    
    private static String executeQueryForOneClassType(RepositoryConnection conn, String queryString) {
        TupleQuery query = conn.prepareTupleQuery(queryString);
        String line = "";
        try (TupleQueryResult result = query.evaluate()) {
            // we just iterate over all solutions in the result...
            for (BindingSet solution : result) {
                //?property ?support ?confidence  ?class are binding variables
                
                if (solution.hasBinding("class")) {
                    line = solution.getValue("property") + "|" + solution.getValue("support").stringValue() + "|" + solution.getValue("confidence").stringValue() + "|" + solution.getValue("class").stringValue();
                } else {
                    line = solution.getValue("property") + "|" + solution.getValue("support").stringValue() + "|" + solution.getValue("confidence").stringValue() + "|" + "NULL";
                }
            }
            result.close();
        }
        return line;
    }
    
    private static List<String> executeQueryForMultipleClassType(RepositoryConnection conn, String queryString) {
        TupleQuery query = conn.prepareTupleQuery(queryString);
        List<String> outputLines = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            // we just iterate over all solutions in the result...
            for (BindingSet solution : result) {
                //?property ?support ?confidence  ?class are binding variables
                String line = solution.getValue("property") + "|" + solution.getValue("support").stringValue() + "|" + solution.getValue("confidence").stringValue() + "|" + solution.getValue("class").stringValue();
                outputLines.add(line);
            }
            result.close();
        }
        return outputLines;
    }
    
    private static String buildQuery(String iri) {
        String queryTemplate = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?property ?support ?confidence ?class WHERE { \n" +
                " \t<_NS_>  rdf:type  <http://www.w3.org/ns/shacl#NodeShape> .\n" +
                "    <_NS_> ?p ?propertyShape .    \n" +
                "    ?propertyShape rdf:type <http://www.w3.org/ns/shacl#PropertyShape> .\n" +
                "    ?propertyShape <http://shaclshapes.org/support>  ?support. \n" +
                "    ?propertyShape <http://shaclshapes.org/confidence> ?confidence .\n" +
                "    Optional {?propertyShape <http://www.w3.org/ns/shacl#class> ?class .}\n" +
                "    ?propertyShape   <http://www.w3.org/ns/shacl#path> ?property .\n" +
                "}\n";
        return queryTemplate.replace("_NS_", iri);
    }
    
    private static String buildQueryForMultipleClassTypesPs(String iri) {
        String queryTemplate = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "SELECT ?property ?class ?support ?confidence   WHERE { \n" +
                " \t<_NS_>  rdf:type  <http://www.w3.org/ns/shacl#NodeShape> .\n" +
                "  <_NS_> ?p ?propertyShape .    \n" +
                "  ?propertyShape rdf:type <http://www.w3.org/ns/shacl#PropertyShape> .\n" +
                "\t?propertyShape <http://www.w3.org/ns/shacl#or>/rdf:rest*/rdf:first  ?orPsConstraints. \n" +
                "\t\n" +
                "\t?orPsConstraints <http://shaclshapes.org/confidence> ?confidence .\n" +
                "  ?orPsConstraints <http://shaclshapes.org/support> ?support.\n" +
                "  ?orPsConstraints  <http://www.w3.org/ns/shacl#class> ?class .\n" +
                "  ?propertyShape   <http://www.w3.org/ns/shacl#path> ?property .\n" +
                "}";
        return queryTemplate.replace("_NS_", iri);
    }
    
    public static void testValidation() {
        String inputDataFilePath = "/Users/kashifrabbani/Documents/GitHub/data/CityDBpedia.nt";
        String inputSHACLFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/Output/TEMP/dbpedia_city.ttl";
        
        //String inputDataFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/example_data.ttl";
        //String inputSHACLFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/example_shapes.ttl";
        String outputSHACLFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/Output/valid.ttl";
        String outputCSVFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/Output/valid.csv";
        
        FilesUtil.deleteFile(outputSHACLFilePath);
        FilesUtil.deleteFile(outputCSVFilePath);
        validate(inputDataFilePath, inputSHACLFilePath, outputSHACLFilePath, outputCSVFilePath);
    }
    
    public static void main(String[] args) throws Exception {
        testValidation();
    }
}

