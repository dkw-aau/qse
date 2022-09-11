package cs.utils;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.util.Arrays;
import java.util.List;
import static org.eclipse.rdf4j.model.util.Values.*;

public class ExampleRDFShaclListCode {
    public static void main(String[] args) throws Exception {
        String ns = "http://example.org/";
        // IRI for ex:favoriteLetters
        IRI favoriteLetters = iri(ns, "favoriteLetters");
        // IRI for ex:John
        IRI john = iri(ns, "John");
        // create a list of letters
        List<Literal> letters = Arrays.asList(new Literal[]{literal("A"), literal("B"), literal("C")});
        //List<Resource> blankNodes = Arrays.asList(new Resource[]{bnode("a"), bnode("b"), bnode("c")});
        // create a head resource for our list
        Resource head = bnode();
        // convert our list and add it to a newly-created Model
        Model aboutJohn = RDFCollections.asRDF(letters, head, new LinkedHashModel());
        
        
        // set the ex:favoriteLetters property to link to the head of the list
        aboutJohn.add(john, favoriteLetters, head);
        ValueFactory vf = SimpleValueFactory.getInstance();
        
        SimpleValueFactory RD4JFactory = SimpleValueFactory.getInstance();
        
        ModelBuilder builder = new ModelBuilder();
        builder.add(john, iri(ns, "hasGotten"), RD4JFactory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
        Model model = builder.build();
        aboutJohn.addAll(model);
        
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
