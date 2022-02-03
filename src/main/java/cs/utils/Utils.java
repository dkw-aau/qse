package cs.utils;

import cs.Main;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class contains some random utility functions required throughout the project
 */
public class Utils {
    
    private static long secondsTotal;
    private static long minutesTotal;
    
    public static void getCurrentTimeStamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now));
        dtf.format(now);
    }
    
    public static boolean isValidIRI(String iri) {
        return iri.indexOf(':') > 0;
    }
    
    public static IRI toIri(String value) {
        ValueFactory factory = SimpleValueFactory.getInstance();
        return factory.createIRI(value);
    }
    
    public static Node IriToNode(String Iri) {
        Resource subjRes = new Resource("<" + Iri + ">", true); // true means you are supplying proper N-Triples RDF terms that do not need to be processed
        Resource predRes = new Resource("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", true);
        BNode bn = new BNode("_:bnodeId", true);
        Node[] triple = new Node[]{subjRes, predRes, bn}; // yields <http://example.org/123> <http://example.org/123> _:bnodeId
        return triple[0];
    }
    
    public static void log(String log) {
        try {
            FileWriter fileWriter = new FileWriter(Constants.RUNTIME_LOGS, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(log);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logTime(String method, long seconds, long minutes) {
        secondsTotal += seconds;
        minutesTotal += minutes;
        //Header: "Dataset,Method,Second,Minute,SecondTotal,MinuteTotal,MaxCard,DatasetPath"
        String line = ConfigManager.getProperty("dataset_name") + "," + method + "," + seconds + "," + minutes + "," + secondsTotal + "," + minutesTotal + "," + Main.extractMaxCardConstraints + "," + Main.datasetPath;
        //log(line);
        System.out.println("Time Elapsed Time " + method + " " + seconds + " sec , " + minutes + " min");
        System.out.println("Total Parsing Time " + secondsTotal + " sec , " + minutesTotal + " min");
    }
    
    public static void simpleTest() {
        ValueFactory factory = SimpleValueFactory.getInstance();
        String iri = "http://www.wikidata.org/entity/Q829554";
        IRI subj = factory.createIRI(iri);
        if(Utils.isValidIRI(iri)){
            System.out.println("True");
        } else {
            System.out.println("False");
        }
    }
}
