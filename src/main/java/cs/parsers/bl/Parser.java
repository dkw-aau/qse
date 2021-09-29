package cs.parsers.bl;

import cs.parsers.SHACLER;
import cs.utils.*;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.util.Pair;
import org.apache.solr.common.util.Hash;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.In;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Parser {
    String rdfFile;
    SHACLER shacler = new SHACLER();
    
    Integer expectedNumberOfClasses;
    HashMap<String, Integer> classInstanceCount;
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount;
    HashMap<Node, HashSet<Integer>> instanceToClass;
    HashMap<Pair<Integer, Integer>, Integer> shapeSupport = new HashMap<>();
    HashSet<Node> properties;
    Encoder encoder;
    Set<Integer> classes;
    
    public Parser(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
        this.properties = new HashSet<>((int) (1000 * 1.33));
        this.encoder = new Encoder();
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    //.filter(line -> line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(Constants.RDF_TYPE)) {
                                classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                                // Track classes per instance
                                if (instanceToClass.containsKey(nodes[0])) {
                                    instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2].getLabel()));
                                } else {
                                    HashSet<Integer> list = new HashSet<>(); // initialize 5, 10, 15
                                    list.add(encoder.encode(nodes[2].getLabel()));
                                    instanceToClass.put(nodes[0], list);
                                }
                            } else {
                                properties.add(nodes[1]);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
            
            properties.forEach(property -> {
                encoder.encode(property.getLabel());
            });
            classes = this.encoder.getTable().keySet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> !line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
                                    if (classes.contains(c)) {
                                        //Fixme: adding the property of the instance.
                                        instanceToClass.get(nodes[0]).add(encoder.encode(nodes[1].getLabel()));
                                        
                                        if (classToPropWithObjTypes.containsKey(encoder.decode(c))) {
                                            HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(encoder.decode(c));
                                            HashSet<String> objTypes = new HashSet<String>();
                                            if (instanceToClass.containsKey(nodes[2])) // object is an instance of some class e.g., :Paris is an instance of :City.
                                                instanceToClass.get(nodes[2]).forEach(node -> {
                                                    objTypes.add(encoder.decode(node));
                                                });
                                            else {
                                                objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                            }
                                            if (propToObjTypes.containsKey(nodes[1]))
                                                propToObjTypes.get(nodes[1]).addAll(objTypes);
                                            else {
                                                propToObjTypes.put(nodes[1], objTypes);
                                            }
                                            classToPropWithObjTypes.put(encoder.decode(c), propToObjTypes);
                                            
                                            HashMap<Node, Integer> propToCount = classToPropWithCount.get(encoder.decode(c));
                                            if (propToCount.containsKey(nodes[1])) {
                                                propToCount.replace(nodes[1], propToCount.get(nodes[1]) + 1);
                                            } else {
                                                propToCount.put(nodes[1], 1);
                                            }
                                            classToPropWithCount.put(encoder.decode(c), propToCount);
                                            
                                        } else {
                                            HashSet<String> objTypes = new HashSet<String>();
                                            if (instanceToClass.containsKey(nodes[2]))  // object is an instance of some class e.g., :Paris is an instance of :City.
                                                instanceToClass.get(nodes[2]).forEach(node -> {
                                                    objTypes.add(encoder.decode(node));
                                                });
                                            else {
                                                objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                            }
                                            HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                            propToObjTypes.put(nodes[1], objTypes);
                                            
                                            
                                            //Add Count of Props
                                            HashMap<Node, Integer> propToCount = new HashMap<>();
                                            propToCount.put(nodes[1], 1);
                                            
                                            instanceToClass.get(nodes[0]).forEach(cl -> {
                                                classToPropWithObjTypes.put(encoder.decode(cl), propToObjTypes);
                                                classToPropWithCount.put(encoder.decode(cl), propToCount);
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
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public void tester() {
        
        for (Map.Entry<Node, HashSet<Integer>> entry : instanceToClass.entrySet()) {
            HashSet<Integer> nodeProps = new HashSet<>();//properties of this node
            HashSet<Integer> nodeClasses = new HashSet<>();//classes of this node
            Node node = entry.getKey();
            HashSet<Integer> classOrProperty = entry.getValue();
            for (Integer cop : classOrProperty) {
                if (properties.contains(cop)) {
                    nodeProps.add(cop);
                }
                if (classes.contains(cop)) {
                    nodeClasses.add(cop);
                }
            }
            nodeClasses.forEach(c -> {
                nodeProps.forEach(p -> {
                    if (shapeSupport.containsKey(new Pair<Integer, Integer>(c, p))) {
                        shapeSupport.put(new Pair<Integer, Integer>(c, p), shapeSupport.get(new Pair<Integer, Integer>(c, p)) + 1);
                        
                    } else {
                        shapeSupport.put(new Pair<Integer, Integer>(c, p), 1);
                    }
                });
            });
            
        }
    }
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
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
        tester();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        //populateShapes();
        //shacler.writeModelToFile();
        System.out.println(shapeSupport);
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Encoder object encoder: " + sizeOf.deepSizeOf(encoder.getTable()));
        System.out.println("Size - Parser HashMap<Node, List<Node>> instanceToClass: " + sizeOf.deepSizeOf(instanceToClass));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    public void run() {
        runParser();
        //measureMemoryUsage();
    }
}