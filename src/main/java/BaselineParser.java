import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;

public class BaselineParser {
    public String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    public final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<Node, HashSet<Node>> classToInstances = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<Node, List<Node>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    
    // Constructor
    BaselineParser(String filePath) {
        this.rdfFile = filePath;
    }
    
    BaselineParser(String filePath, String expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = Integer.parseInt(expSizeOfClasses);
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> line.contains(RDFType))
                    .forEach(line -> {                              // - A terminal operation
                        //String[] nodes = line.split(" ");     // - Parse a <subject> <predicate> <object> string
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            //Track instances per class
                            if (classToInstances.containsKey(nodes[2])) {
                                classToInstances.get(nodes[2]).add(nodes[0]);
                            } else {
                                HashSet<Node> cti = new HashSet<Node>() {{ add(nodes[0]); }};
                                classToInstances.put(nodes[2], cti);
                            }
                            // Track classes per instance
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).add(nodes[2]);
                            } else {
                                List<Node> list = new ArrayList<>();
                                list.add(nodes[2]);
                                instanceToClass.put(nodes[0], list);
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
    
    public void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            //String[] nodes = line.split(" ");     // - Parse a <subject> <predicate> <object> string
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
                                    if (classToPropWithObjTypes.containsKey(c)) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(c);
                                        HashSet<String> objTypes = new HashSet<String>();
                                        
                                        if (instanceToClass.containsKey(nodes[2])) // object is an instance of some class e.g., :Paris is an instance of :City.
                                            //objTypes.addAll(instanceToClass.get(nodes[2]));
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else
                                            propToObjTypes.put(nodes[1], objTypes);
                                        
                                        if (instanceToClass.get(nodes[0]) != null) {
                                            classToPropWithObjTypes.put(c, propToObjTypes);
                                        }
                                        
                                    } else {
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (instanceToClass.containsKey(nodes[2]))  // object is an instance of some class e.g., :Paris is an instance of :City.
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        
                                        HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(nodes[1], objTypes);
                                        if (instanceToClass.get(nodes[0]) != null) {
                                            instanceToClass.get(nodes[0]).forEach(cl -> {
                                                classToPropWithObjTypes.put(cl, propToObjTypes);
                                            });
                                        }
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
    
    
    public void prioritizeClasses() {
        StopWatch watch = new StopWatch();
        watch.start();
        this.classToInstances = classToInstances.entrySet()
                .stream()
                .sorted(comparingInt(e -> e.getValue().size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
        watch.stop();
        System.out.println("Time Elapsed prioritizeClasses: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public String getType(String value) {
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
    
    public static void runParser(BaselineParser parser) {
        parser.firstPass();
        parser.secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + parser.classToInstances.size() + "\n\t" + "No. of distinct Properties: " + parser.properties.size());
        
        parser.prioritizeClasses();
        parser.populateShapes();
        System.out.println("******** OUTPUT COMPLETE SHAPES GRAPH");
        parser.shacler.printModel();
    }
    
    public static void measureMemoryUsage(BaselineParser parser) {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, HashSet<String>> classToInstances: " + sizeOf.deepSizeOf(parser.classToInstances));
        System.out.println("Size - Parser HashMap<String, String> instanceToClass: " + sizeOf.deepSizeOf(parser.instanceToClass));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(parser.properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(parser.classToPropWithObjTypes));
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        String expectedNumberOfClasses = args[1];
        BaselineParser parser = new BaselineParser(filePath, expectedNumberOfClasses);
        runParser(parser);
        measureMemoryUsage(parser);
    }
}