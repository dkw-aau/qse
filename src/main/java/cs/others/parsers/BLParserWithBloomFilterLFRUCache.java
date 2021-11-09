package cs.others.parsers;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import cs.qse.endpoint.SHACLER;
import cs.utils.ConfigManager;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.time.StopWatch;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BLParserWithBloomFilterLFRUCache {
    private String rdfFile = "";
    private SHACLER shacler = new SHACLER();
    
    // Constants
    private final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private int expectedNumberOfClasses = 10000; // default value
    private int sizeOfDataset = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashSet<Node> properties = new HashSet<>();
    HashMap<Node, BloomFilter<CharSequence>> ctiBf = new HashMap<>();
    
    // Constructor
    public BLParserWithBloomFilterLFRUCache(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            try (ProgressBar pb = new ProgressBar("Progress1stPass", sizeOfDataset)) {
                
                Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                        .filter(line -> line.contains(RDFType))
                        .forEach(line -> {                              // - A terminal operation
                            try {
                                Node[] nodes = NxParser.parseNodes(line);
                                classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                                
                                if (ctiBf.containsKey(nodes[2])) {
                                    ctiBf.get(nodes[2]).put(nodes[0].getLabel());
                                } else {
                                    BloomFilter<CharSequence> bf = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100_000, 0.01);
                                    bf.put(nodes[0].getLabel());
                                    ctiBf.put(nodes[2], bf);
                                }
                                
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            pb.step();
                            pb.setExtraMessage("Reading...");
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        //LRUCache itcCache = new LRUCache(1000000);
        
        Cache<Node, List<Node>> subjItcCache = new Cache2kBuilder<Node, List<Node>>() {}
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .entryCapacity(1000000)
                .build();
    
        Cache<Node, List<Node>> objItcCache = new Cache2kBuilder<Node, List<Node>>() {}
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .entryCapacity(1000000)
                .build();
        
        try {
            try (ProgressBar pb = new ProgressBar("Progress2ndPass", sizeOfDataset)) {
                Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                        .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                        .forEach(line -> {                              // - A terminal operation
                            try {
                                Node[] nodes = NxParser.parseNodes(line);
                                List<Node> instanceTypes = new ArrayList<>();
                                HashSet<String> objTypes = new HashSet<String>();
                                
                                boolean literalTypeFlag = nodes[2].toString().contains("\"");
                                
                                if (literalTypeFlag) {
                                    //check the subject entry in the cache
                                    if (subjItcCache.containsKey(nodes[0])) {
                                        instanceTypes.addAll(Objects.requireNonNull(subjItcCache.get(nodes[0])));
                                    } else {
                                        lookUpSubjInBf(subjItcCache, nodes, instanceTypes);
                                    }
                                    
                                } else {
                                    //check the subject and the object entry in the cache
                                    if (subjItcCache.containsKey(nodes[0]) && objItcCache.containsKey(nodes[2])) {
                                        instanceTypes.addAll(subjItcCache.get(nodes[0]));
                                        objItcCache.get(nodes[2]).forEach(val -> {
                                            objTypes.add(val.getLabel());
                                        });
                                    } else {
                                        lookUpSubjObjInBf(subjItcCache, objItcCache, nodes, instanceTypes, objTypes);
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
                            pb.step();
                            pb.setExtraMessage("Reading ...");
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void lookUpSubjInBf(Cache<Node, List<Node>> itcCache, Node[] nodes, List<Node> instanceTypes) {
        ctiBf.forEach((c, bf) -> {
            if (bf.mightContain(nodes[0].getLabel())) {
                instanceTypes.add(c);
                if (itcCache.containsKey(nodes[0])) {
                    itcCache.get(nodes[0]).add(c);
                } else {
                    List<Node> list = new ArrayList<>();
                    list.add(c);
                    itcCache.put(nodes[0], list);
                }
            }
        });
    }
    
    private void lookUpSubjObjInBf(Cache<Node, List<Node>> subjItcCache, Cache<Node, List<Node>> objItcCache, Node[] nodes, List<Node> instanceTypes, HashSet<String> objTypes) {
        ctiBf.forEach((c, bf) -> {
            if (bf.mightContain(nodes[0].getLabel())) {
                instanceTypes.add(c);
                if (subjItcCache.containsKey(nodes[0])) {
                    subjItcCache.get(nodes[0]).add(c);
                } else {
                    List<Node> list = new ArrayList<>();
                    list.add(c);
                    subjItcCache.put(nodes[0], list);
                }
            }
            if (bf.mightContain(nodes[2].getLabel())) {
                objTypes.add(c.getLabel());
                
                if (objItcCache.containsKey(nodes[2])) {
                    objItcCache.get(nodes[2]).add(c);
                } else {
                    List<Node> list = new ArrayList<>();
                    list.add(c);
                    objItcCache.put(nodes[2], list);
                }
            }
        });
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
        secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        populateShapes();
        shacler.writeModelToFile();
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<Node, BloomFilter<CharSequence>> ctiBf: " + sizeOf.deepSizeOf(ctiBf));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    public void run() {
        runParser();
        measureMemoryUsage();
    }
}
