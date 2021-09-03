package cs.parsers;

import com.google.common.math.Quantiles;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.NodeEncoder;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BLParserWithBloomFiltersAndBFS {
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
    HashMap<Integer, BloomFilter<String>> ctiBf;
    
    MembershipGraph mg;
    
    // Constructor
    public BLParserWithBloomFiltersAndBFS(String filePath, int expSizeOfClasses) {
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
        this.mg = new MembershipGraph(encoder);
        mg.createMembershipSets(instanceToClass);
        mg.createMembershipGraph(classInstanceCount);
        mg.membershipGraphOutlierNormalization(expectedNumberOfClasses, ctiBf);
        //mg.exportGraphRelatedData();
        //mg.importGraphRelatedData();
        this.membershipGraph = mg.getMembershipGraph();
        this.membershipGraphRootNode = mg.getMembershipGraphRootNode();
        
        watch.stop();
        System.out.println("Time Elapsed MembershipGraphConstruction: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void secondPassToFilterInstancesOnly() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started secondPassToFilterInstancesOnly");
        Map<Integer, List<List<Integer>>> copy = this.mg.membershipSets;
        HashSet<List<Integer>> markedSets = new HashSet<>();
        HashSet<Node> instancesToKeep = new HashSet<>();
        this.instanceToClass.forEach((instance, classes) -> {
            copy.forEach((member, memberSets) -> {
                memberSets.forEach(set -> {
                    if (!markedSets.contains(set)) {
                        if (set.equals(classes)) {
                            markedSets.add(set);
                            instancesToKeep.add(instance);
                        }
                    }
                });
            });
        });
        System.out.println("Pre filtering Done: Size" + instancesToKeep.size());
        System.out.println("Started Writing into a File:");
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
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        ArrayList<Long> innerWatchTime = new ArrayList<>(nol);
        ArrayList<Long> innerInnerWatchTime = new ArrayList<>(nol);
        ArrayList<Double> coverage = new ArrayList<>(nol);
        
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(Constants.RDF_TYPE))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            String result = "";
                            StopWatch innerWatch = new StopWatch();
                            innerWatch.start();
                            int visitedNodesCounter = 0;
                            
                            Node[] nodes = NxParser.parseNodes(line);
                            List<Node> instanceTypes = new ArrayList<>();
                            HashSet<String> objTypes = new HashSet<String>();
                            
                            int node = this.membershipGraphRootNode;
                            HashSet<Integer> visited = new HashSet<>(expectedNumberOfClasses);
                            LinkedList<Integer> queue = new LinkedList<Integer>();
                            queue.add(node);
                            visited.add(node);
                            
                            while (queue.size() != 0) {
                                node = queue.poll();
                                for (DefaultEdge edge : membershipGraph.outgoingEdgesOf(node)) {
                                    Integer neigh = membershipGraph.getEdgeTarget(edge);
                                    if (!visited.contains(neigh)) {
                                        boolean flag = false;
                                        if (ctiBf.get(neigh).contains(nodes[0].getLabel())) {
                                            instanceTypes.add(encoder.decode(neigh));
                                            flag = true;
                                        }
                                        if (ctiBf.get(neigh).contains(nodes[2].getLabel())) {
                                            objTypes.add(encoder.decode(neigh).getLabel());
                                            flag = true;
                                        }
                                        if (flag) {
                                            queue.add(neigh);
                                        }
                                        visited.add(neigh);
                                        visitedNodesCounter++;
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
                            
                            innerWatch.stop();
                            innerWatchTime.add(innerWatch.getTime());
                            coverage.add(((double) visitedNodesCounter / (double) membershipGraph.vertexSet().size()));
                            result += innerWatch.getTime() + "," + ((double) visitedNodesCounter / (double) membershipGraph.vertexSet().size());
                            FilesUtil.writeToFileInAppendMode(result, ConfigManager.getProperty("output_file_path") + "/" + ConfigManager.getProperty("dataset_name") + "_" + "stats.csv");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
            
            
        } catch (Exception e) {
            
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        //computeStatistics(innerWatchTime, innerInnerWatchTime, coverage);
    }
    
    private void computeStatistics(ArrayList<Long> innerWatchTime, ArrayList<Long> innerInnerWatchTime, ArrayList<Double> coverage) {
        StopWatch watch = new StopWatch();
        watch.start();
        
        //Average
        StopWatch avgWatch = new StopWatch();
        avgWatch.start();
        double avgInnerWatchTime = innerWatchTime.stream().mapToLong(d -> d).average().orElse(0.0);
        double avgInnerInnerWatchTime = innerInnerWatchTime.stream().mapToLong(d -> d).average().orElse(0.0);
        double avgCoverage = coverage.stream().mapToDouble(d -> d).average().orElse(0.0);
        avgWatch.stop();
        System.out.println("\nAverageInnerWatchTime, AverageInnerInnerWatchTime, AverageCoverage");
        System.out.println(avgInnerWatchTime + ", " + avgInnerInnerWatchTime + ", " + avgCoverage);
        System.out.println("Time Elapsed computing average: " + TimeUnit.MILLISECONDS.toSeconds(avgWatch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(avgWatch.getTime()));
        
        //Median
       /* StopWatch medianWatch = new StopWatch();
        medianWatch.start();
        double medianInnerWatchTime = Quantiles.median().compute(innerWatchTime);
        double medianInnerInnerWatchTime = Quantiles.median().compute(innerInnerWatchTime);
        double medianCoverage = Quantiles.median().compute(coverage);
        medianWatch.stop();
        System.out.println("Time Elapsed computing median: " + TimeUnit.MILLISECONDS.toSeconds(medianWatch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(medianWatch.getTime()));
        */
        //Percentiles
        StopWatch percentileWatch = new StopWatch();
        percentileWatch.start();
        
        System.out.println("50 Percentile - innerWatchTime: " + Quantiles.percentiles().index(50).compute(innerWatchTime));
        System.out.println("50 Percentile - innerInnerWatchTime: " + Quantiles.percentiles().index(50).compute(innerInnerWatchTime));
        System.out.println("50 Percentile - coverage: " + Quantiles.percentiles().index(50).compute(coverage));
        
        System.out.println("90 Percentile - innerWatchTime: " + Quantiles.percentiles().index(90).compute(innerWatchTime));
        System.out.println("90 Percentile - innerInnerWatchTime: " + Quantiles.percentiles().index(90).compute(innerInnerWatchTime));
        System.out.println("90 Percentile - coverage: " + Quantiles.percentiles().index(90).compute(coverage));
        
        System.out.println("75 Percentile - innerWatchTime: " + Quantiles.percentiles().index(75).compute(innerWatchTime));
        System.out.println("75 Percentile - innerInnerWatchTime: " + Quantiles.percentiles().index(75).compute(innerInnerWatchTime));
        System.out.println("75 Percentile - coverage: " + Quantiles.percentiles().index(75).compute(coverage));
        
        System.out.println("25 Percentile - innerWatchTime: " + Quantiles.percentiles().index(25).compute(innerWatchTime));
        System.out.println("25 Percentile - innerInnerWatchTime: " + Quantiles.percentiles().index(25).compute(innerInnerWatchTime));
        System.out.println("25 Percentile - coverage: " + Quantiles.percentiles().index(25).compute(coverage));
        
        System.out.println("10 Percentile - innerWatchTime: " + Quantiles.percentiles().index(10).compute(innerWatchTime));
        System.out.println("10 Percentile - innerInnerWatchTime: " + Quantiles.percentiles().index(10).compute(innerInnerWatchTime));
        System.out.println("10 Percentile - coverage: " + Quantiles.percentiles().index(10).compute(coverage));
        
        percentileWatch.stop();
        System.out.println("Time Elapsed computing percentiles: " + TimeUnit.MILLISECONDS.toSeconds(percentileWatch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(percentileWatch.getTime()));
        
        watch.stop();
        System.out.println("Time Elapsed computing statistics: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
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
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Encoder object encoder: " + sizeOf.deepSizeOf(encoder.getTable()));
        System.out.println("Size - Parser HashMap<Node, List<Node>> instanceToClass: " + sizeOf.deepSizeOf(instanceToClass));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    private void runParser() throws IOException {
        firstPass();
        membershipGraphConstruction();
        secondPassToFilterInstancesOnly();
        //there is one node having degree = 1630 in the current membership graph
        //System.out.println(encoder.decode(new DegreeDistribution(membershipGraph).getNodeWithDegree(1630)));
        
        //secondPass();
        //populateShapes();
        //shacler.writeModelToFile();
        System.out.println("OUT DEGREE OF HNG ROOT NODE: " + membershipGraph.outDegreeOf(membershipGraphRootNode));
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
    }
    
    
    public void run() throws IOException {
        runParser();
        //measureMemoryUsage();
    }
}

 /*DecimalFormat formatter = new DecimalFormat("#,###");
classInstanceCount.forEach((c, i) -> { System.out.println(c + " -> " + formatter.format(i)); });

classToPropWithCount.forEach((k, v) -> {
System.out.println(k);
v.forEach((k1, v1) -> {
System.out.println("\t " + k1 + " -> " + formatter.format(v1));
});
System.out.println();
});*/
