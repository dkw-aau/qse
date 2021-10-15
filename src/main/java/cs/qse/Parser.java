package cs.qse;

import cs.utils.*;
import cs.utils.encoders.Encoder;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Literal;
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
    Integer expectedNumberOfClasses;
    Encoder encoder;
    String instanceOfProperty;
    HashMap<Integer, Integer> classInstanceCount;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes;
    
    HashMap<Node, HashSet<Integer>> instanceToClass;
    HashMap<Node, HashSet<Tuple2<Integer, Integer>>> instance2propertyShape;
    
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String instanceOfProperty) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.instanceOfProperty = instanceOfProperty;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.instanceToClass = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(instanceOfProperty)) {
                                // Track classes per instance
                                if (instanceToClass.containsKey(nodes[0])) {
                                    instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2].getLabel()));
                                } else {
                                    HashSet<Integer> list = new HashSet<>(); // initialize 5, 10, 15
                                    list.add(encoder.encode(nodes[2].getLabel()));
                                    instanceToClass.put(nodes[0], list);
                                }
                                if (classInstanceCount.containsKey(encoder.encode(nodes[2].getLabel()))) {
                                    Integer val = classInstanceCount.get(encoder.encode(nodes[2].getLabel()));
                                    classInstanceCount.put(encoder.encode(nodes[2].getLabel()), val + 1);
                                } else {
                                    classInstanceCount.put(encoder.encode(nodes[2].getLabel()), 1);
                                }
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
        this.instance2propertyShape = new HashMap<>((int) ((classInstanceCount.size() / 0.75 + 1)));
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> !line.contains(instanceOfProperty))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            HashSet<Integer> objTypes = new HashSet<>();
                            HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                            Node instance = nodes[0];
                            String objectType = extractObjectType(nodes[2].toString());
                            
                            if (objectType.equals("IRI")) { // // object is an instance of some class e.g., :Paris is an instance of :City & :Capital
                                if (instanceToClass.containsKey(nodes[2])) {
                                    for (Integer node : instanceToClass.get(nodes[2])) {
                                        objTypes.add(node);
                                        prop2objTypeTuples.add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), node));
                                    }
                                    addIntoInstance2PropertyShapeMap(prop2objTypeTuples, instance);
                                }
                            } else { // Object is literal
                                objTypes.add(encoder.encode(objectType));
                                prop2objTypeTuples = new HashSet<>() {{
                                    add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), encoder.encode(objectType)));
                                }};
                                addIntoInstance2PropertyShapeMap(prop2objTypeTuples, instance);
                            }
                            HashSet<Integer> entityClasses = instanceToClass.get(nodes[0]);
                            if (entityClasses != null) {
                                for (Integer entityClass : entityClasses) {
                                    HashMap<Integer, HashSet<Integer>> propToObjTypes;
                                    if (classToPropWithObjTypes.containsKey(entityClass)) {
                                        propToObjTypes = classToPropWithObjTypes.get(entityClass);
                                        int prop = encoder.encode(nodes[1].getLabel());
                                        if (propToObjTypes.containsKey(prop))
                                            propToObjTypes.get(prop).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(prop, objTypes);
                                        }
                                    } else {
                                        propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(encoder.encode(nodes[1].getLabel()), objTypes);
                                    }
                                    classToPropWithObjTypes.put(entityClass, propToObjTypes);
                                }
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
    
    private void addIntoInstance2PropertyShapeMap(HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples, Node instance) {
        instance2propertyShape.putIfAbsent(instance, prop2objTypeTuples);
        HashSet<Tuple2<Integer, Integer>> prop2objTypeTupleSet = instance2propertyShape.get(instance);
        prop2objTypeTupleSet.addAll(prop2objTypeTuples);
        instance2propertyShape.put(instance, prop2objTypeTupleSet);
    }
    
    private String extractObjectType(String node) {
        Literal theLiteral = new Literal(node, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(node)) {
                if (SimpleValueFactory.getInstance().createIRI(node).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    public void computeShapeStatistics() {
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        ComputeStatistics cs = new ComputeStatistics(shapeTripletSupport);
        cs.compute(this.instance2propertyShape, instanceToClass, classInstanceCount);
        this.shapeTripletSupport = cs.getShapeTripletSupport();
        System.out.println("Done");
    }
    
    private void writeSupportToFile() {
        System.out.println("Writing to File ...");
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SC> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" +
                        encoder.decode(tupl3._3) + "|" + count + "|" + classInstanceCount.get(tupl3._1);
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
        SHACLER shacler = new SHACLER(encoder, shapeTripletSupport);
        shacler.constructDefaultShapes(classToPropWithObjTypes);
        
        ArrayList<Integer> supportRange = new ArrayList<>(Arrays.asList(1, 50, 100, 500, 1000));
        HashMap<Double, List<Integer>> confSuppMap = new HashMap<>();
        confSuppMap.put(0.25, supportRange);
        confSuppMap.put(0.50, supportRange);
        confSuppMap.put(0.75, supportRange);
        confSuppMap.put(0.90, supportRange);
        
        confSuppMap.forEach((conf,supportParams)-> {
            supportRange.forEach(supp -> {
                shacler.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
            });
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void runParser() {
        firstPass();
        secondPass();
        computeShapeStatistics();
        populateShapes();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size());
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Encoder object encoder: " + sizeOf.deepSizeOf(encoder.getTable()));
        System.out.println("Size - Parser HashMap<Node, List<Node>> instanceToClass: " + sizeOf.deepSizeOf(instanceToClass));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    public void run() {
        runParser();
        //new StatsCollector().doTheJob();
        //measureMemoryUsage();
    }
}