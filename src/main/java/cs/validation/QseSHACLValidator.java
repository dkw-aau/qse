package cs.validation;

import cs.utils.ConfigManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;

import java.io.*;
import java.util.Objects;

public class QseSHACLValidator {
    
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
    
    private void validate(String inputDataFilePath, String inputSHACLFilePath, String outputSHACLFilePath, String outputCSVFilePath) {
        try {
            Graph shapesGraph = RDFDataMgr.loadGraph(inputSHACLFilePath);
            Graph dataGraph = RDFDataMgr.loadGraph(inputDataFilePath);
            
            Shapes shapes = Shapes.parse(shapesGraph);
            ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);
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
}
