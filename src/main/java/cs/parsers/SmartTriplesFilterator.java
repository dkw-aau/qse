package cs.parsers;

import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.NodeEncoder;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import org.apache.commons.lang3.time.StopWatch;
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

public class SmartTriplesFilterator {
    String rdfFile;
    Integer expectedNumberOfClasses;
    Integer membershipGraphRootNode;
    HashMap<String, Integer> classInstanceCount;
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<String, HashMap<Node, Integer>> classToPropWithCount;
    HashMap<Node, List<Integer>> instanceToClass;
    HashSet<Node> properties;
    NodeEncoder encoder;
    DefaultDirectedGraph<Integer, DefaultEdge> membershipGraph;
    HashMap<Integer, BloomFilter<String>> ctiBf;
    MembershipGraph mg;
    
    public SmartTriplesFilterator(String filePath) {
        this.rdfFile = filePath;
    }
    
    
    public SmartTriplesFilterator(String filePath, Integer expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        
        //FIXME : Initialize in a proper way
        this.instanceToClass = new HashMap<>();
        this.properties = new HashSet<>();
        this.encoder = new NodeEncoder();
        this.ctiBf = new HashMap<>();
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
                                ctiBf.get(encoder.encode(nodes[2])).add(nodes[0].getLabel());
                            } else {
                                BloomFilter<String> bf = new FilterBuilder(100_000, 0.000001).buildBloomFilter();
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
        StopWatch watch = new StopWatch();
        watch.start();
        this.mg = new MembershipGraph(encoder, ctiBf);
        mg.createMembershipSets(instanceToClass);
        mg.createMembershipGraph(classInstanceCount);
        this.membershipGraph = mg.getMembershipGraph();
        this.membershipGraphRootNode = mg.getMembershipGraphRootNode();
        watch.stop();
        System.out.println("Time Elapsed MembershipGraphConstruction: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void filteringInstances() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started secondPassToFilterInstancesOnly");
        HashMap<List<Integer>, Integer> membersCount = new HashMap<>();
        HashSet<List<Integer>> membersHashSet = new HashSet<>();
        for (List<List<Integer>> lists : this.mg.membershipSets.values()) {
            lists.forEach(val -> {
                membersHashSet.add(val);
                membersCount.put(val, 0);
            });
        }
        
        HashSet<Node> instancesToKeep = new HashSet<>();
        this.instanceToClass.forEach((instance, classMembers) -> {
            if (membersHashSet.contains(classMembers)) {
                instancesToKeep.add(instance);
                if (membersCount.get(classMembers).equals(Integer.parseInt(ConfigManager.getProperty("numberOfTriplesPerMembershipSet")))) {
                    membersHashSet.remove(classMembers);
                } else {
                    membersCount.put(classMembers, membersCount.get(classMembers) + 1);
                }
            }
        });
        
        try {
            Files.lines(Path.of(rdfFile))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instancesToKeep.contains(nodes[0])) {
                                FilesUtil.writeToFileInAppendMode(line, Constants.FILTERED_DATASET);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        watch.stop();
        System.out.println("Time Elapsed secondPassToFilterInstancesOnly: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public void extractSubClassOfTriples() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))
                    .filter(line -> line.contains(Constants.SUB_CLASS_OF))
                    .forEach(line -> {
                        try {
                            FilesUtil.writeToFileInAppendMode(line, Constants.SUBCLASSOF_DATASET);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed extractSubClassOfTriples: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public void run() {
        firstPass();
        membershipGraphConstruction();
        filteringInstances();
    }
}
