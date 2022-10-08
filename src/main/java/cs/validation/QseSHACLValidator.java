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

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class allows to validate given shacl shapes against provided data graph.
 * It allows standalone running with specified files as well
 * It has some other functionalities to play with shapes and custom validator events
 */
public class QseSHACLValidator {
    
    private static Node createResource(String uri) {return NodeFactory.createURI(uri);}
    
    public QseSHACLValidator() {
        System.out.println("Invoked::QseSHACLValidator");
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
                validate(dataFilePath, fileEntry.getAbsolutePath(), outputFilePath + ".ttl", outputFilePath + ".csv");
            }
        }
    }
    
    private static void validate(String inputDataFilePath, String inputSHACLFilePath, String outputSHACLFilePath, String outputCSVFilePath) {
        try {
            Graph shapesGraph = RDFDataMgr.loadGraph(inputSHACLFilePath);
            Graph dataGraph = RDFDataMgr.loadGraph(inputDataFilePath);
            
            Shapes shapes = Shapes.parse(shapesGraph);
            
            //ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);
            Node city = createResource("http://schema.org/Aalborg");
            ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph, city);
            
            //ShLib.printReport(report);
            //Write as TTL Output
            OutputStream out = new FileOutputStream(outputSHACLFilePath, false);
            RDFDataMgr.write(out, report.getModel(), Lang.TTL);
            //Write as CSV Output
            FileWriter fileWriter = new FileWriter(outputCSVFilePath, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            String header = "SourceShape|FocusNode|ResultPath|Value|SourceConstraintComponent|Message";
            printWriter.println(header);
            report.getEntries().forEach(re -> {
                String line = re.source() + "|" + re.focusNode() + "|" + re.resultPath() + "|" + re.value() + "|" + re.sourceConstraintComponent() + "|" + re.message();
                printWriter.println(line);
            });
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void parseShapeGraph(String inputDataFilePath, String inputSHACLFilePath, String outputSHACLFilePath, String outputCSVFilePath) {
        try {
            Graph shapesGraph = RDFDataMgr.loadGraph(inputSHACLFilePath);
            Shapes shapes = Shapes.parse(shapesGraph);
            shapes.getShapeMap().forEach((n, s) -> {
                System.out.println(n + " -> " + s);
            });
            
            for (Shape shape : shapes.getTargetShapes()) {
                shape.getPropertyShapes().forEach(propertyShape -> {
                    propertyShape.getConstraints().forEach(constraint -> {
                        System.out.println();
                    });
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void validateWithListener(String inputDataFilePath, String inputSHACLFilePath, String outputSHACLFilePath, String outputCSVFilePath) {
        try {
            Graph shapesGraph = RDFDataMgr.loadGraph(inputSHACLFilePath);
            Graph dataGraph = RDFDataMgr.loadGraph(inputDataFilePath);
            
            Shapes shapes = Shapes.parse(shapesGraph);
            
            RecordingValidationListener listener = new RecordingValidationListener();  // see above
            
            ValidationContext vCtx = ValidationContext.create(shapes, dataGraph, listener); // pass listener here
            for (Shape shape : shapes.getTargetShapes()) {
                shape.getPropertyShapes().forEach(propertyShape -> {
                    propertyShape.getConstraints();
                });
                Collection<Node> focusNodes = VLib.focusNodes(dataGraph, shape);
                for (Node focusNode : focusNodes) {
                    vCtx.setVerbose(true);
                    VLib.validateShape(vCtx, dataGraph, shape, focusNode);
                }
            }
            System.out.println("--- ");
            vCtx.generateReport().getEntries().forEach(entry -> {
                System.out.println(entry.toString());
                System.out.println();
            });
            //System.out.println(vCtx.generateReport().getEntries().toString());
            
            Set<ValidationEvent> actualEvents = listener.getEvents(); // all events have been recorded
            System.out.println(actualEvents.size());
            /*
            actualEvents.forEach(event -> {
                //System.out.println(event.getValidationContext().generateReport().getEntries().toString());
                Collection<ReportEntry> entries = event.getValidationContext().generateReport().getEntries();
                entries.forEach(entry -> {
                    if (entry.constraint().getComponent().getLocalName().equals(SHACL.NOT_CONSTRAINT_COMPONENT.getLocalName())) {
                        //System.out.println("break");
                    }
                });
            });
            
            
            ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);
            //ShLib.printReport(report);
            //Write as TTL Output
            OutputStream out = new FileOutputStream(outputSHACLFilePath, false);
            RDFDataMgr.write(out, report.getModel(), Lang.TTL);
            //Write as CSV Output
            FileWriter = new FileWriter(outputCSVFilePath, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            String header = "SourceShape|FocusNode|ResultPath|Value|SourceConstraintComponent|Message";
            printWriter.println(header);
            report.getEntries().forEach(re -> {
                String line = re.source() + "|" + re.focusNode() + "|" + re.resultPath() + "|" + re.value() + "|" + re.sourceConstraintComponent() + "|" + re.message();
                printWriter.println(line);
            });
            printWriter.close();*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void testValidation() {
        //String inputDataFilePath = "/Users/kashifrabbani/Documents/GitHub/data/CityDBpedia.nt";
        //String inputSHACLFilePath =  "/Users/kashifrabbani/Documents/GitHub/shacl/Output/TEMP/positiveAndNegativeWithShNot/DBPEDIA_ML_CUSTOM_0.25_25_SHACL_Pretty_SHACL.ttl";
        
        String inputDataFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/example_data.ttl";
        String inputSHACLFilePath = "/Users/kashifrabbani/Documents/GitHub/shacl/validation/example/example_shapes.ttl";
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

class RecordingValidationListener implements ValidationListener {
    private final Set<ValidationEvent> events = new HashSet<>();
    
    @Override
    public void onValidationEvent(ValidationEvent e) {
        events.add(e);
    }
    
    public Set<ValidationEvent> getEvents() {
        return events;
    }
}

