package cs.qse.common;

import cs.qse.common.encoders.ConcurrentEncoder;
import cs.qse.common.encoders.Encoder;
import cs.qse.filebased.SupportConfidence;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.Tuple3;
import cs.utils.Utils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 */
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
    
    
    private static String buildQuery(String classIri, String property, String objectType, String queryFile, String typePredicate) {
        String query = (FilesUtil.readQuery(queryFile)
                .replace(":Class", " <" + classIri + "> "))
                .replace(":Prop", " <" + property + "> ")
                .replace(":ObjectType", " " + objectType + " ");
        query = query.replace(":instantiationProperty", typePredicate);
        return query;
    }
    
    
    public static void writeSupportToFile(Encoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        System.out.println("Started writeSupportToFile()");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), false);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" +
                        encoder.decode(tupl3._3) + "|" + count + "|" + sampledEntitiesPerClass.get(tupl3._1).size();
                printWriter.println(log);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("writeSupportToFile() ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public static void writeSupportToFile(ConcurrentEncoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        System.out.println("Started writeSupportToFile()");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), false);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" +
                        encoder.decode(tupl3._3) + "|" + count + "|" + sampledEntitiesPerClass.get(tupl3._1).size();
                printWriter.println(log);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("writeSupportToFile() ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}
