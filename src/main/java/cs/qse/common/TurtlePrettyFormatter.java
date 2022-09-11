package cs.qse.common;

import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class TurtlePrettyFormatter {
    String fileAddress;
    
    public TurtlePrettyFormatter(String fileAddress) {
        this.fileAddress = fileAddress;
    }
    
    public void format(String outputPath) {
        try {
            System.out.println("Pretty Formatting");
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            OutputStream out = new FileOutputStream(outputPath, false);
            // Build or load a Jena Model
            Model model = RDFDataMgr.loadModel(fileAddress);
            //System.out.println(model.size());
            // Either create a string... String prettyPrintedModel = formatter.apply(model);
            //System.out.println(prettyPrintedModel);
            // ...or write directly to an OutputStream (of file) or System.out
            formatter.accept(model, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
