package cs.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
