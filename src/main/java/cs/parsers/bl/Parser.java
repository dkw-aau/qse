package cs.parsers.bl;

import cs.parsers.SHACLER;
import cs.utils.*;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Parser {
    String rdfFile;
    SHACLER shacler = new SHACLER();
    
    Integer expectedNumberOfClasses;
    HashMap<Integer, Integer> classInstanceCount;
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<Node, HashSet<Integer>> instanceToClass;
    Map<Node, HashSet<Tuple2<Integer, Integer>>> instance2propertyShape;
    Encoder encoder;
    
    HashMap<Tuple3<Integer, Integer, Integer>, Integer> shapeTripletSupport;
    
    public Parser(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
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
                            if (nodes[1].toString().equals(Constants.RDF_TYPE)) {
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
                    .filter(line -> !line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            HashSet<String> objTypes = new HashSet<>();
                            HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                            Node instance = nodes[0];
                            String objectType = extractObjectType(nodes[2].toString());
                            
                            if (objectType.equals("IRI")) { // // object is an instance of some class e.g., :Paris is an instance of :City & :Capital
                                if (instanceToClass.containsKey(nodes[2])) {
                                    for (Integer node : instanceToClass.get(nodes[2])) {
                                        objTypes.add(encoder.decode(node));
                                        prop2objTypeTuples.add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), node));
                                    }
                                    addIntoInstance2PropertyShapeMap(prop2objTypeTuples, instance);
                                }
                            } else { // Object is literal
                                objTypes.add(objectType);
                                prop2objTypeTuples = new HashSet<>() {{
                                    add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), encoder.encode(objectType)));
                                }};
                                addIntoInstance2PropertyShapeMap(prop2objTypeTuples, instance);
                            }
                            HashSet<Integer> entityClasses = instanceToClass.get(nodes[0]);
                            if (entityClasses != null) {
                                for (Integer entityClass : entityClasses) {
                                    if (classToPropWithObjTypes.containsKey(encoder.decode(entityClass))) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(encoder.decode(entityClass));
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(nodes[1], objTypes);
                                        }
                                        classToPropWithObjTypes.put(encoder.decode(entityClass), propToObjTypes);
                                    } else {
                                        HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(nodes[1], objTypes);
                                        classToPropWithObjTypes.put(encoder.decode(entityClass), propToObjTypes);
                                        /*instanceToClass.get(nodes[0]).forEach(node -> {
                                            classToPropWithObjTypes.put(encoder.decode(node), propToObjTypes);
                                        });*/
                                    }
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
    
    public void computeSupport() {
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.instance2propertyShape.forEach((instance, propertyShapeSet) -> {
            HashSet<Integer> instanceClasses = instanceToClass.get(instance);
            if (instanceClasses != null) {
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
            }
        });
        System.out.println("Writing to File ...");
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, Integer> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue();
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
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /*    private String getType(Node value) {
        String theType = XSD.STRING.stringValue(); //default type is XSD:string
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
    */
    private void runParser() {
        firstPass();
        secondPass();
        computeSupport();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size());
        populateShapes();
        shacler.writeModelToFile();
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