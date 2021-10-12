package cs.parsers;

import cs.qse.SHACLER;
import cs.utils.ConfigManager;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class OnDiskMapParser {
    private String rdfFile = "";
    private SHACLER shacler = new SHACLER();
    
    // Constants
    private final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private int expectedNumberOfClasses = 10000; // default value
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    //HashMap<Node, List<Node>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    
    //Map DB
    DB db = DBMaker.fileDB(ConfigManager.getProperty("mapdb_file_path")).make();
    HTreeMap mapdb = db.hashMap("instanceToClassMap").create();
  
    // Constructor
    public OnDiskMapParser(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
    }
    
    private void firstPass() {
        
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> line.contains(RDFType))
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                            if (mapdb.containsKey(nodes[0])) {
                                List<Node> list = (List<Node>) mapdb.get(nodes[0]);
                                list.add(nodes[2]);
                                mapdb.put(nodes[0], list);
                            } else {
                                List<Node> list = new ArrayList<>();
                                list.add(nodes[2]);
                                mapdb.put(nodes[0], list);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (mapdb.containsKey(nodes[0])) {
                                List<Node> classList = (List<Node>) mapdb.get(nodes[0]);
                                classList.forEach(c -> {
                                    if (classToPropWithObjTypes.containsKey(c)) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(c);
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (mapdb.containsKey(nodes[2])) // object is an instance of some class e.g., :Paris is an instance of :City.
                                            ((List<Node>) mapdb.get(nodes[0])).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else {
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        }
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(nodes[1], objTypes);
                                        }
                                        classToPropWithObjTypes.put(c, propToObjTypes);
                                        
                                        
                                    } else {
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (mapdb.containsKey(nodes[2]))  // object is an instance of some class e.g., :Paris is an instance of :City.
                                            ((List<Node>) mapdb.get(nodes[0])).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else {
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        }
                                        HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(nodes[1], objTypes);
                                        
                                        ((List<Node>) mapdb.get(nodes[0])).forEach(cl -> {
                                            classToPropWithObjTypes.put(cl, propToObjTypes);
                                        });
                                    }
                                });
                            }
                            properties.add(nodes[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private String getType(String value) {
        String theType = "<http://www.w3.org/2001/XMLSchema#string>"; //default type is XSD:string
        
        if (value.contains("\"^^")) {
            //value.split("\\^\\^")[0] is the literal value, and value.split("\\^\\^")[1]  the type of the value ;
            if (value.split("\\^\\^").length > 1) {
                theType = value.split("\\^\\^")[1];
            }
        } else if (value.contains("\"@")) {
            //value.split("\"@")[0] is the literal value and value.split("\"@")[1] is the language tag
            theType = RDF.LANGSTRING.toString();  //rdf:langString
        }
        return theType;
    }
    
    private void runParser() {
        firstPass();
        secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        populateShapes();
        shacler.writeModelToFile();
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Parser MapDB: " + sizeOf.deepSizeOf(mapdb));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    private void deleteTempMapFile() {
        File file = new File(ConfigManager.getProperty("mapdb_file_path"));
        if (file.delete()) {
            System.out.println("File deleted successfully");
        } else {
            System.out.println("Failed to delete the file");
        }
    }
    
    public void run() {
        //deleteTempMapFile();
        runParser();
        measureMemoryUsage();
        db.close();
    }
}