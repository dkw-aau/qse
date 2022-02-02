package cs.utils;

import com.github.jsonldjava.core.RDFDataset;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

// ...
import static org.eclipse.rdf4j.model.util.Values.*;

public class Test {
    public static void rdfListTest() {
        String ns = "http://example.org/";
        // IRI for ex:favoriteLetters
        IRI favoriteLetters = iri(ns, "favoriteLetters");
        // IRI for ex:John
        IRI john = iri(ns, "John");
        // create a list of letters
        //List<Literal> letters = Arrays.asList(new Literal[]{literal("A"), literal("B"), literal("C")});
        List<Resource> blankNodes = Arrays.asList(new Resource[]{bnode("a"), bnode("b"), bnode("c")});
        // create a head resource for our list
        Resource head = bnode();
        // convert our list and add it to a newly-created Model
        Model aboutJohn = RDFCollections.asRDF(blankNodes, head, new LinkedHashModel());
        // set the ex:favoriteLetters property to link to the head of the list
        aboutJohn.add(john, favoriteLetters, head);
        
        //Statement a = Statements.statement(head, SHACL.DATATYPE, XSD.STRING, null);
        //Statement b = Statements.statement(head, SHACL.NODE_KIND, SHACL.LITERAL, null);
        //Statement c = Statements.statement(head, VOID.ENTITIES, iri("100"), null);
        //List<Statement> statements = Arrays.asList(a, b);
        //Model aboutJohn = RDFCollections.asRDF(statements, head, new LinkedHashModel());
        //aboutJohn.add(john, SHACL.OR, head);
        try {
            //FileWriter fileWriter = new FileWriter(ConfigManager.getProperty("output_file_path") + fileName, false);
            Rio.write(aboutJohn, System.out, RDFFormat.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
