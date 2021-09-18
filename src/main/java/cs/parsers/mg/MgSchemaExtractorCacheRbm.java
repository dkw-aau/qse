package cs.parsers.mg;

import cs.parsers.SHACLER;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.NodeEncoder;
import org.apache.commons.lang3.time.StopWatch;
import org.ehcache.sizeof.SizeOf;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.roaringbitmap.RoaringBitmap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MgSchemaExtractorCacheRbm {
    String rdfFile;
    SHACLER shacler = new SHACLER();
    
    Integer expectedNumberOfClasses;
    Integer membershipGraphRootNode;
    HashMap<String, Integer> classInstanceCount;
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount;
    
    HashMap<Node, List<Integer>> instanceToClass;
    HashSet<Node> properties;
    NodeEncoder encoder;
    DefaultDirectedGraph<Integer, DefaultEdge> membershipGraph;
    HashMap<Integer, RoaringBitmap> ctiRbm;
    MembershipGraph mg;
    
    public MgSchemaExtractorCacheRbm(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
        this.properties = new HashSet<>((int) (1000 * 1.33));
        this.encoder = new NodeEncoder();
        this.ctiRbm = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    private void firstPass() {
        NodeEncoder instanceEncoder = new NodeEncoder();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> line.contains(Constants.RDF_TYPE))
                    .forEach(line -> {
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
                            
                            if (ctiRbm.containsKey(encoder.encode(nodes[2]))) {
                                ctiRbm.get(encoder.encode(nodes[2])).add(instanceEncoder.encode(nodes[0]));
                            } else {
                                RoaringBitmap roaringBitmap = new RoaringBitmap();
                                roaringBitmap.add(instanceEncoder.encode(nodes[0]));
                                ctiRbm.put(encoder.encode(nodes[2]), roaringBitmap);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
            
            System.out.println("ctiRbm size: " + ctiRbm.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser  HashMap<Integer, RoaringBitmap> ctiRbm: " + sizeOf.deepSizeOf(ctiRbm));
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Encoder object encoder: " + sizeOf.deepSizeOf(encoder.getTable()));
        System.out.println("Size - Parser HashMap<Node, List<Node>> instanceToClass: " + sizeOf.deepSizeOf(instanceToClass));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    private void runParser() throws IOException {
        firstPass();
        //membershipGraphConstruction();
        this.instanceToClass.clear();
        //secondPass();
        //populateShapes();
        //shacler.writeModelToFile();
        //System.out.println("OUT DEGREE OF HNG ROOT NODE: " + membershipGraph.outDegreeOf(membershipGraphRootNode));
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
    }
    
    
    public void run() throws IOException {
        runParser();
        measureMemoryUsage();
    }
}
