package cs.parsers;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import cs.utils.NodeEncoder;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BLParserWithBloomFiltersAndBFS {
    String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    //HashMap<Node, List<Node>> instanceToClass = new HashMap<>((int) (1000000 / 0.75 + 1));
    HashMap<Node, List<Integer>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    NodeEncoder encoder = new NodeEncoder();
    DefaultDirectedGraph<Integer, DefaultEdge> directedGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
    
    HashMap<Integer, BloomFilter<CharSequence>> ctiBf = new HashMap<>();
    
    // Constructor
    BLParserWithBloomFiltersAndBFS(String filePath) {
        this.rdfFile = filePath;
    }
    
    public BLParserWithBloomFiltersAndBFS(String filePath, int expSizeOfClasses) {
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
                            classInstanceCount.put(nodes[2].getLabel(), (classInstanceCount.getOrDefault(nodes[2].getLabel(), 0)) + 1);
                            
                            // Track classes per instance
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).add(encoder.encode(nodes[2]));
                            } else {
                                List<Integer> list = new ArrayList<>();
                                list.add(encoder.encode(nodes[2]));
                                instanceToClass.put(nodes[0], list);
                            }
                            
                            if (ctiBf.containsKey(encoder.encode(nodes[2]))) {
                                ctiBf.get(encoder.encode(nodes[2])).put(nodes[0].getLabel());
                            } else {
                                BloomFilter<CharSequence> bf = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100_000, 0.000001);
                                bf.put(nodes[0].getLabel());
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
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void hierarchicalSchemaGraphConstruction() {
        StopWatch watch = new StopWatch();
        watch.start();
        instanceToClass.values().stream().distinct().collect(Collectors.groupingBy(List::size)).forEach((mSize, mSets) -> {
            //System.out.println(mSize + " - " + mSets);
            if (mSize == 1) {
                mSets.forEach(set -> {
                    set.forEach(directedGraph::addVertex);
                });
            } else if (mSize > 1) {
                mSets.forEach(set -> {
                    HashMap<Integer, Integer> memberFrequency = new HashMap<>();
                    set.forEach(element -> {
                        memberFrequency.put(element, classInstanceCount.get(encoder.decode(element).getLabel()));
                        if (!directedGraph.containsVertex(element)) directedGraph.addVertex(element);
                    });
                    Integer[] sortedElementsOfMemberSet = sortingKeysOfMapByValues(memberFrequency).keySet().toArray(new Integer[0]);
                    //System.out.println(Arrays.toString(sortedGroupMembers));
                    
                    for (int i = 1; i < sortedElementsOfMemberSet.length; i++) {
                        if (memberFrequency.get(sortedElementsOfMemberSet[i - 1]).equals(memberFrequency.get(sortedElementsOfMemberSet[i]))) {
                            //System.out.println("SAME " + sortedElementsOfMemberSet[i - 1] + " -- " + sortedElementsOfMemberSet[i]);
                            directedGraph.addEdge(sortedElementsOfMemberSet[i - 1], sortedElementsOfMemberSet[i]);
                            directedGraph.addEdge(sortedElementsOfMemberSet[i], sortedElementsOfMemberSet[i - 1]);
                        } else {
                            directedGraph.addEdge(sortedElementsOfMemberSet[i - 1], sortedElementsOfMemberSet[i]);
                        }
                    }
                });
            }
        });
        
        //System.out.println("Printing graph"); System.out.println(directedGraph);
        
        AtomicBoolean flag = new AtomicBoolean(false);
        ArrayList<Integer> rootNodesOfSubGraphs = new ArrayList<>();
        
        ConnectivityInspector<Integer, DefaultEdge> connectivityInspector = new ConnectivityInspector<>(directedGraph);
        connectivityInspector.connectedSets().stream().sorted(Comparator.comparingInt(Set::size)).forEach(subGraphVertices -> {
            //System.out.println("Size: " + subGraphVertices.size());
            if (subGraphVertices.size() > 1) {
                subGraphVertices.forEach(vertex -> {
                    if (directedGraph.inDegreeOf(vertex) == 0) {
                        //System.out.println("Root Node of Current Sub-graph: " + vertex + " :: " + encoder.decode(vertex));
                        rootNodesOfSubGraphs.add(vertex);
                        flag.set(true);
                    }
                    
                    if (!flag.get()) {
                        //System.out.println("There doesn't exist root node in this graph");
                        rootNodesOfSubGraphs.add(new Random().nextInt(subGraphVertices.size()));
                    }
                });
                
            } else if (subGraphVertices.size() == 1) {
                rootNodesOfSubGraphs.addAll(subGraphVertices);
            }
        });
        
        //Add a main root to connect all the sub-graphs
        directedGraph.addVertex(-999);
        rootNodesOfSubGraphs.forEach(node -> {
            directedGraph.addEdge(-999, node);
        });
        
        watch.stop();
        System.out.println("Time Elapsed hierarchicalSchemaGraphConstruction: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            
            //NeighborCache<Integer, DefaultEdge> neighborCache = new NeighborCache<Integer, DefaultEdge>(directedGraph);
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            
                            List<Node> instanceTypes = new ArrayList<>();
                            HashSet<String> objTypes = new HashSet<String>();
                            
                            // Mark all the vertices as not visited(By default set as false)
                            //boolean[] visited = new boolean[directedGraph.vertexSet().size()];
                            List<Integer> visited = new ArrayList<Integer>();
                            // Create a queue for BFS
                            LinkedList<Integer> queue = new LinkedList<Integer>();
                            
                            // Mark the current node as visited and enqueue it
                            int node = -999;
                            queue.add(node);
                            visited.add(node);
                            while (queue.size() != 0) {
                                // Dequeue a vertex from queue and print it
                                node = queue.poll();
                                //System.out.println("Neighbours: "); -> Graphs.successorListOf(directedGraph, node)
                                // Get all adjacent vertices of the dequeued node
                                // If a adjacent has not been visited, then mark it visited and enqueue it, else continue
                                for (Integer neigh : Graphs.successorListOf(directedGraph, node)) {
                                    if (!visited.contains(neigh)) {
                                        boolean flag = false;
                                        if (ctiBf.get(neigh).mightContain(nodes[0].getLabel())) {
                                            instanceTypes.add(encoder.decode(neigh));
                                            flag = true;
                                        }
                                        if (ctiBf.get(neigh).mightContain(nodes[2].getLabel())) {
                                            objTypes.add(encoder.decode(neigh).getLabel());
                                            flag = true;
                                        }
                                        if (flag) {
                                            queue.add(neigh);
                                        }
                                        visited.add(neigh);
                                    }
                                }
                            }
                            
                            
                            instanceTypes.forEach(c -> {
                                if (objTypes.isEmpty()) {
                                    objTypes.add(getType(nodes[2].toString()));
                                }
                                
                                if (classToPropWithObjTypes.containsKey(c)) {
                                    HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(c);
                                    
                                    if (propToObjTypes.containsKey(nodes[1]))
                                        propToObjTypes.get(nodes[1]).addAll(objTypes);
                                    else {
                                        propToObjTypes.put(nodes[1], objTypes);
                                    }
                                    classToPropWithObjTypes.put(c, propToObjTypes);
                                    
                                } else {
                                    HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                    propToObjTypes.put(nodes[1], objTypes);
                                    instanceTypes.forEach(type -> {
                                        classToPropWithObjTypes.put(type, propToObjTypes);
                                    });
                                }
                            });
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
    
    private void runParser() throws IOException {
        firstPass();
        hierarchicalSchemaGraphConstruction();
        secondPass();
        System.out.println("Done Grouping");
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        
        DecimalFormat formatter = new DecimalFormat("#,###");
        classInstanceCount.forEach((c, i) -> { System.out.println(c + " -> " + formatter.format(i)); });
        
        classToPropWithCount.forEach((k, v) -> {
            System.out.println(k);
            v.forEach((k1, v1) -> {
                System.out.println("\t " + k1 + " -> " + formatter.format(v1));
            });
            System.out.println();
        });
        
        populateShapes();
        shacler.writeModelToFile();
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
    
    public void run() throws IOException {
        runParser();
        //measureMemoryUsage();
    }
}