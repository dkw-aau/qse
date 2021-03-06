package cs.others.parsers.bl;

import cs.qse.endpoint.SHACLER;
import cs.others.parsers.mg.MembershipGraph;
import cs.utils.*;
import cs.utils.encoders.NodeEncoder;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BaselineParserExtended {
    String rdfFile;
    SHACLER shacler = new SHACLER();
    
    Integer expectedNumberOfClasses;
    HashMap<String, Integer> classInstanceCount;
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount;
    HashMap<Node, List<Integer>> instanceToClass;
    HashSet<Node> properties;
    //Encoder encoder;
    MembershipGraph mg;
    NodeEncoder encoder;
    HashMap<Integer, BloomFilter<String>> ctiBf;
    DefaultDirectedGraph<Integer, DefaultEdge> membershipGraph;
    Integer membershipGraphRootNode;
    
    public BaselineParserExtended(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
        this.properties = new HashSet<>((int) (1000 * 1.33));
        //this.encoder = new Encoder();
        this.ctiBf = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.encoder = new NodeEncoder();
    }
    
    private void preFirstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            classInstanceCount.put(nodes[2].getLabel(), (classInstanceCount.getOrDefault(nodes[2].getLabel(), 0)) + 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed preFirstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            //classInstanceCount.put(nodes[2].getLabel(), (classInstanceCount.getOrDefault(nodes[2].getLabel(), 0)) + 1);
                            
                            // Track classes per instance
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2]));
                            } else {
                                List<Integer> list = new ArrayList<>();
                                list.add(encoder.encode(nodes[2]));
                                instanceToClass.put(nodes[0], list);
                            }
                            
                            if (ctiBf.containsKey(encoder.encode(nodes[2]))) {
                                ctiBf.get(encoder.encode(nodes[2])).add(nodes[0].getLabel());
                            } else {
                                BloomFilter<String> bf = new FilterBuilder(classInstanceCount.get(nodes[2].getLabel()), 0.00001).buildBloomFilter();
                                bf.add(nodes[0].getLabel());
                                ctiBf.put(encoder.encode(nodes[2]), bf);
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
    
    private void membershipGraphConstruction() {
        Utils.getCurrentTimeStamp();
        StopWatch watch = new StopWatch();
        watch.start();
        this.mg = new MembershipGraph(encoder, ctiBf, classInstanceCount);
        mg.createMembershipSets(instanceToClass);
        mg.createMembershipGraph();
        //mg.membershipGraphCompression(Integer.parseInt(ConfigManager.getProperty("mg_threshold")));
        //mg.exportGraphRelatedData();
        //mg.importGraphRelatedData();
        this.membershipGraph = mg.getMembershipGraph();
        this.membershipGraphRootNode = mg.getMembershipGraphRootNode();
        this.ctiBf = mg.getCtiBf();
        
        watch.stop();
        System.out.println("Time Elapsed MembershipGraphConstruction: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(Constants.RDF_TYPE))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
                                    if (classToPropWithObjTypes.containsKey(encoder.decode(c))) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(encoder.decode(c));
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (instanceToClass.containsKey(nodes[2])) // object is an instance of some class e.g., :Paris is an instance of :City.
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(encoder.decode(node).getLabel());
                                            });
                                        else {
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        }
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(nodes[1], objTypes);
                                        }
                                        classToPropWithObjTypes.put(encoder.decode(c).getLabel(), propToObjTypes);
                                        
                                        HashMap<Node, Integer> propToCount = classToPropWithCount.get(encoder.decode(c));
                                        if (propToCount.containsKey(nodes[1])) {
                                            propToCount.replace(nodes[1], propToCount.get(nodes[1]) + 1);
                                        } else {
                                            propToCount.put(nodes[1], 1);
                                        }
                                        classToPropWithCount.put(encoder.decode(c).getLabel(), propToCount);
                                        
                                    } else {
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (instanceToClass.containsKey(nodes[2]))  // object is an instance of some class e.g., :Paris is an instance of :City.
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(encoder.decode(node).getLabel());
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
                                            classToPropWithObjTypes.put(encoder.decode(cl).getLabel(), propToObjTypes);
                                            classToPropWithCount.put(encoder.decode(cl).getLabel(), propToCount);
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
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
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
    
    /**
     * INPUT: classToPropWithObjTypes Map and MembershipGraph
     * Purpose: Use both of the inputs to assign confidence value to the node shape properties
     */
    private void verifyNodeShapeProperties() {
        //TODO::
        // 1. Traverse the MG in using BFS
        // 2. Start from the top node
        // 3. For each node shape property of the node,
        // 4. find its existence in the other nodes
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
        preFirstPass();
        firstPass();
        membershipGraphConstruction();
        System.out.println("Construction Successful");
        secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        populateShapes();
        shacler.writeModelToFile();
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