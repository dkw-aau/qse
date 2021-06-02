package cs.parsers;

import cs.utils.Encoder;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.text.DecimalFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.ejml.UtilEjml.assertTrue;

public class BLParserWithSupport {
    String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    //HashMap<Node, List<Node>> instanceToClass = new HashMap<>((int) (1000000 / 0.75 + 1));
    HashMap<Node, List<Integer>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    Encoder encoder = new Encoder();
    
    // Constructor
    BLParserWithSupport(String filePath) {
        this.rdfFile = filePath;
    }
    
    public BLParserWithSupport(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        //HashMap<String, List<Integer>> groups = new HashMap<>();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> line.contains(RDFType))
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            classInstanceCount.put(nodes[2].getLabel(), (classInstanceCount.getOrDefault(nodes[2].getLabel(), 0)) + 1);
                            // Track classes per instance
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2].getLabel()));
                            } else {
                                List<Integer> list = new ArrayList<>();
                                list.add(encoder.encode(nodes[2].getLabel()));
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
    
    private void grouping() {
        encoder.getTable().forEach((k, v) -> {
            System.out.println(k + " - " + v);
        });
        instanceToClass.values().stream().distinct().forEachOrdered(val -> {
            System.out.println(val);
        });
        
        
        Graph<Integer, DefaultEdge> directedGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        
        instanceToClass.values().stream().distinct().collect(Collectors.groupingBy(List::size)).forEach((groupSize, groupSlot) -> {
            System.out.println(groupSize + " - " + groupSlot);
            if (groupSize == 1) {
                groupSlot.forEach(group -> {
                    group.forEach(directedGraph::addVertex);
                });
            } else if (groupSize > 1) {
                groupSlot.forEach(group -> {
                    HashMap<Integer, Integer> memberFrequency = new HashMap<>();
                    group.forEach(groupMember -> {
                        memberFrequency.put(groupMember, classInstanceCount.get(encoder.decode(groupMember)));
                        if (!directedGraph.containsVertex(groupMember)) directedGraph.addVertex(groupMember);
                    });
                    Integer[] sortedGroupMembers = sortingKeysOfMapByValues(memberFrequency).keySet().toArray(new Integer[0]);
                    //System.out.println(Arrays.toString(sortedGroupMembers));
                    
                    for (int i = 1; i < sortedGroupMembers.length; i++) {
                        directedGraph.addEdge(sortedGroupMembers[i - 1], sortedGroupMembers[i]);
                    }
                });
            }
            
        });
        System.out.println(directedGraph.toString());
        
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
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
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
        //secondPass();
        grouping();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        
        DecimalFormat formatter = new DecimalFormat("#,###");
        classInstanceCount.forEach((c, i) -> {
            System.out.println(c + " -> " + formatter.format(i));
        });
        
        System.out.println();
        
        /*    classToPropWithCount.forEach((k, v) -> {
            System.out.println(k);
            v.forEach((k1, v1) -> {
                System.out.println("\t " + k1 + " -> " + formatter.format(v1));
            });
            System.out.println();
        });*/
        
        //populateShapes();
        //shacler.writeModelToFile();
    }
    
    //https://www.baeldung.com/java-sorting
    public Map<Integer, Integer> sortingKeysOfMapByValues(HashMap<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(
                    Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        Map<Integer, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
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