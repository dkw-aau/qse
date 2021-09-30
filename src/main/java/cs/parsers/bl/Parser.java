package cs.parsers.bl;

import cs.parsers.SHACLER;
import cs.utils.*;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.util.Pair;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Parser {
    String rdfFile;
    SHACLER shacler = new SHACLER();
    
    Integer expectedNumberOfClasses;
    HashMap<Integer, Integer> classInstanceCount;
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;

    HashMap<Node, HashSet<Integer>> instanceToClass;
    Map<Node, HashSet<Tuple2<Integer, Integer>>> instance2propertyShape; // for each instance we keep track of which property shapes they are assigned to one id is for the property id the other is for the object class id

    //HashMap<Pair<Integer, Integer>, Integer> shapeSupport;
    //HashMap<Pair<Integer, Integer>, Integer> shapeConfidence;
    
    HashSet<Integer> properties;
    Encoder encoder;
    HashSet<Integer> classes;
   
    HashMap<Tuple3<Integer, Integer, Integer>, Integer> shapeTripletSupport;
    
    public Parser(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
        this.properties = new HashSet<>((int) (1000 * 1.33));
        this.encoder = new Encoder();
        this.classes = new HashSet<>();
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(Constants.RDF_TYPE)) {
                                // Track classes per instance
                                if (instanceToClass.containsKey(nodes[0])) {
                                    instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2].getLabel()));
                                } else {
                                    HashSet<Integer> list = new HashSet<>(); // initialize 5, 10, 15
                                    list.add(encoder.encode(nodes[2].getLabel()));
                                    instanceToClass.put(nodes[0], list);
                                }
                                
                                classes.add(encoder.encode(nodes[2].getLabel()));
                                classInstanceCount.put(encoder.encode(nodes[2].getLabel()), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                                
                            } else {
                                properties.add(encoder.encode(nodes[1].getLabel()));
                                //instanceToClass.get(nodes[0]).add(encoder.encode(nodes[1].getLabel()));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        this.instance2propertyShape = new HashMap<>((int) ((classes.size() * properties.size()) / 0.75 + 1));
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> !line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
                                    
                                    HashSet<String> objTypes = new HashSet<>();
                                    HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                                    Node instance = nodes[0];
                                    
                                    // object is an instance of some class e.g., :Paris is an instance of :City.
                                    if (instanceToClass.containsKey(nodes[2])) {
                                        for (Integer node : instanceToClass.get(nodes[2])) {
                                            objTypes.add(encoder.decode(node));
                                            prop2objTypeTuples.add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), node));
                                        }
                                        if (instance2propertyShape.containsKey(instance)) {
                                            HashSet<Tuple2<Integer, Integer>> prop2objTypeTupleSet = instance2propertyShape.get(instance);
                                            prop2objTypeTupleSet.addAll(prop2objTypeTuples);
                                            instance2propertyShape.put(instance, prop2objTypeTupleSet);
                                        } else {
                                            instance2propertyShape.put(instance, prop2objTypeTuples);
                                        }
                                    }
                                    // Object is literal
                                    else {
                                        String objType = getType(nodes[2].toString());
                                        objTypes.add(objType);
                                        prop2objTypeTuples = new HashSet<>() {{
                                            add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), encoder.encode(objType)));
                                        }};
                                        if (instance2propertyShape.containsKey(instance)) {
                                            HashSet<Tuple2<Integer, Integer>> prop2objTypeTupleSet = instance2propertyShape.get(instance);
                                            prop2objTypeTupleSet.addAll(prop2objTypeTuples);
                                            instance2propertyShape.put(instance, prop2objTypeTupleSet);
                                        } else {
                                            instance2propertyShape.put(instance, prop2objTypeTuples);
                                        }
                                    }
                                    
                                    
                                    if (classToPropWithObjTypes.containsKey(encoder.decode(c))) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(encoder.decode(c));
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(nodes[1], objTypes);
                                        }
                                        classToPropWithObjTypes.put(encoder.decode(c), propToObjTypes);
                                        
                                    } else {
                                        HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(nodes[1], objTypes);
                                        
                                        instanceToClass.get(nodes[0]).forEach(node -> {
                                            if (classes.contains(node)) {
                                                classToPropWithObjTypes.put(encoder.decode(node), propToObjTypes);
                                            }
                                        });
                                    }
                                });
                            }
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
    
    public void computeSupport() {
        this.shapeTripletSupport = new HashMap<>((int) ((classes.size() * properties.size() * classes.size()) / 0.75 + 1));
        this.instance2propertyShape.forEach((instance, propertyShapeSet) -> {
            for (Integer c : instanceToClass.get(instance)) {
                for (Tuple2<Integer, Integer> propObjTuple : propertyShapeSet) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                    if (this.shapeTripletSupport.containsKey(tuple3)) {
                        Integer val = this.shapeTripletSupport.get(tuple3) + 1;
                        this.shapeTripletSupport.put(tuple3, val);
                    } else {
                        this.shapeTripletSupport.put(tuple3, 1);
                    }
                }
            }
        });
        System.out.println("Writing to File ...");
    
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
    
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, Integer> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" + encoder.decode(tupl3._3) + "|" + count;
                //System.out.println(log);
                printWriter.println(log);
            }
    
            
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        computeSupport();
        //computeShapeSupport();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        //populateShapes();
        //shacler.writeModelToFile();
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
        new StatsCollector().doTheJob();
        //runParser();
        //measureMemoryUsage();
    }
}