package cs.qse.common;

import cs.Main;
import cs.qse.common.encoders.ConcurrentStringEncoder;
import cs.qse.common.encoders.StringEncoder;
import cs.qse.filebased.SupportConfidence;
import cs.utils.*;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.semanticweb.yars.nx.Literal;

import java.io.*;
import java.util.HashMap;
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
    
    public static String buildBatchQuery(Set<String> types, List<String> entities, String typePredicate) {
        StringBuilder query = new StringBuilder("PREFIX onto: <http://www.ontotext.com/> \nSELECT * from onto:explicit WHERE { \n");
        
        for (String type : types) {
            query.append("?entity ").append(typePredicate).append(" <").append(type).append("> .\n");
        }
        query.append("?entity ?p ?o . \n");
        query.append("VALUES (?entity) { \n");
        for (String entity : entities) {
            query.append("\t( ").append(entity).append(" ) \n");
        }
        query.append(" } \n} ");
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
    
    public static void writeSupportToFile(StringEncoder stringEncoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        System.out.println("Started writeSupportToFile()");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), false);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = stringEncoder.decode(tupl3._1) + "|" + stringEncoder.decode(tupl3._2) + "|" +
                        stringEncoder.decode(tupl3._3) + "|" + count + "|" + sampledEntitiesPerClass.get(tupl3._1).size();
                printWriter.println(log);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("writeSupportToFile() ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public static void writeSupportToFile(ConcurrentStringEncoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, List<Integer>> sampledEntitiesPerClass) {
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
    
    public static Map<Integer, Map<Integer, Set<Integer>>> extractShapesForSpecificClasses(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Map<Integer, Integer> classEntityCount, StringEncoder stringEncoder) {
        Map<Integer, Map<Integer, Set<Integer>>> filteredClassToPropWithObjTypes = new HashMap<>();
        String fileAddress = ConfigManager.getProperty("config_dir_path") + "pruning/classes.txt";
        List<String> classes = FilesUtil.readAllLinesFromFile(fileAddress);
        classes.forEach(classIri -> {
            int key = stringEncoder.encode(classIri);
            Map<Integer, Set<Integer>> value = classToPropWithObjTypes.get(key);
            if (classEntityCount.containsKey(key))
                filteredClassToPropWithObjTypes.put(key, value);
        });
        return filteredClassToPropWithObjTypes;
    }
    
    public static List<String> getListOfClasses() {
        String fileAddress = ConfigManager.getProperty("config_dir_path") + "pruning/classes.txt";
        return FilesUtil.readAllLinesFromFile(fileAddress);
    }
    
    public static void writeClassFrequencyInFile(Map<Integer, Integer> classEntityCount, StringEncoder stringEncoder) {
        String fileNameAndPath = Main.outputFilePath + "/classFrequency.csv";
        try {
            FileWriter fileWriter = new FileWriter(fileNameAndPath, false);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("Class,Frequency");
            classEntityCount.forEach((classVal, entityCount) -> {
                printWriter.println(stringEncoder.decode(classVal) + "," + entityCount);
            });
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static RepositoryConnection readFileAsRdf4JModel(String inputFileAddress) {
        RepositoryConnection conn = null;
        try {
            InputStream input = new FileInputStream(inputFileAddress);
            Model model = Rio.parse(input, "", RDFFormat.TURTLE);
            Repository db = new SailRepository(new MemoryStore());
            conn = db.getConnection();
            conn.add(model);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

}
