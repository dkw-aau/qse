package cs.validation;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.VLib;
import org.apache.jena.shacl.validation.ValidationListener;
import org.apache.jena.shacl.validation.event.ValidationEvent;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomizedValidator {
    
    private static Node createResource(String uri) {return NodeFactory.createURI(uri);}
    
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
                Collection<Node> focusNodes = VLib.focusNodes(dataGraph, shape);
                for (Node focusNode : focusNodes) {
                    vCtx.setVerbose(true);
                    VLib.validateShape(vCtx, dataGraph, shape, focusNode);
                }
            }
            
           /* System.out.println(" --- Print entries ---  ");
            vCtx.generateReport().getEntries().forEach(entry -> {
                System.out.println(entry.toString());
                System.out.println();
            });*/
            
            System.out.println(" --- Writing to file ---  ");
            //Write as TTL Output
            OutputStream out = new FileOutputStream(outputSHACLFilePath, false);
            RDFDataMgr.write(out, vCtx.generateReport().getModel(), Lang.TTL);
            
            //System.out.println(vCtx.generateReport().getEntries().toString());
            
            //Set<ValidationEvent> actualEvents = listener.getEvents(); // all events have been recorded
            //System.out.println(actualEvents.size());
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


