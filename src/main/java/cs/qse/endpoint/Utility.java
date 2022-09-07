package cs.qse.endpoint;

import cs.utils.FilesUtil;
import cs.utils.Utils;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;

import java.util.Set;

public class Utility {
    
    /**
     * A utility method to extract the literal object type - todo: Handle more data types like Data etc.
     *
     * @param literalIri : IRI for the literal object
     * @return String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
     */
    public static String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    
    public static String buildQuery(String entity, Set<String> types, String typePredicate) {
        StringBuilder query = new StringBuilder("PREFIX onto: <http://www.ontotext.com/> \nSELECT * from onto:explicit WHERE { \n");
        
        for (String type : types) {
            query.append("<").append(entity).append("> ").append(typePredicate).append(" <").append(type).append("> .\n");
        }
        query.append("<").append(entity).append("> ").append("?p ?o . \n }\n");
        return query.toString();
    }
    
    
    private String buildQuery(String classIri, String property, String objectType, String queryFile, String typePredicate) {
        String query = (FilesUtil.readQuery(queryFile)
                .replace(":Class", " <" + classIri + "> "))
                .replace(":Prop", " <" + property + "> ")
                .replace(":ObjectType", " " + objectType + " ");
        query = query.replace(":instantiationProperty", typePredicate);
        return query;
    }
}
