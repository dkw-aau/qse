package cs.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class EntityValidator {
    String rdfFilePath;
    
    public EntityValidator(String rdfFilePath) {
        this.rdfFilePath = rdfFilePath;
        this.rdf4jParsing();
    }
    
    public void rdf4jParsing() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started Parsing...");
        try {
            FileInputStream fileInputStream = new FileInputStream(rdfFilePath);
            ModelBuilder builder = new ModelBuilder();
            MyParser myParser = new MyParser();
            myParser.setRDFHandler(new StatementCollector(builder.build()));
            try {
                myParser.parse(fileInputStream, "");
            } catch (IOException | RDFHandlerException | RDFParseException e) {
                e.printStackTrace();
            } finally {
                fileInputStream.close();
            }
            System.out.println("About to write model to file ...");
            Model model = builder.build();
            
            FileOutputStream out = new FileOutputStream(Constants.CLEAN_DATASET_FILE);
            RDFWriter writer = Rio.createWriter(RDFFormat.N3, out);
            writer.startRDF();
            for (Statement st : model) {
                writer.handleStatement(st);
                writer.getWriterConfig();
            }
            writer.endRDF();
            out.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed rdf4jParsing: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public void xyz() {
        ValueFactory factory = SimpleValueFactory.getInstance();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            String subject = nodes[0].getLabel();
                            String predicate = nodes[1].getLabel();
                            String object = nodes[2].getLabel();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
