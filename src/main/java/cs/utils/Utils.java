package cs.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * This class contains some random utility functions required throughout the project
 */
public class Utils {
    
    public static String getCurrentTimeStamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now));
        return dtf.format(now);
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
    
    public static boolean isActivated(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
}
