package cs.qse;

import cs.qse.experiments.ExperimentsUtil;
import cs.qse.experiments.MinCardinalityExperiment;
import cs.utils.Constants;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.custom.CustomForEach;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import cs.utils.parallelism.ThreadExecutor;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class parses RDF NT file triples to extract SHACL shapes, compute the confidence/support for shape constraints,
 * and perform node and property shape constraints pruning based on defined threshold for confidence and support
 */
public class TestParser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;
    
    //HashMap<Node, EntityData> entityDataHashMap;
    //HashMap<String, EntityData> entityDataHashMapLite;
    
    HashMap<Integer, EntityData> encodedEntityDataHashMap;
    NodeEncoder nodeEncoder;
    
    HashMap<Integer, Integer> classEntityCount;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    
    HashMap<Node, BloomFilter<String>> cteBf;
    ConcurrentHashMap<Node, BloomFilter<String>> concurrentHashMapCteBf;
    HashMap<Integer, Integer> classEntityCountForSampling;
    
    HashMap<Node, EntityData> entityDataHashMap;
    
    public TestParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
        
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classEntityCountForSampling = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.encodedEntityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        
        //this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        //this.cteBf = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        System.out.println(encodedEntityDataHashMap);
        //collectClassEntityCount();
        //parsing();
        //parallelFileParsing();
        //test();
        //bfExperiment();
        //reservoirSamplingFirstPass();
        //iterateOverBloomFilters();
        //collectClassEntityCount();
        //collectEntities();
        //secondPass();
        //computeSupportConfidence();
        //extractSHACLShapes();
        //assignCardinalityConstraints();
        //System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    private void testParallelStreamWithCustomCore(int cores) {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Stream<String> lines = Files.lines(Path.of(rdfFilePath));
            ThreadExecutor.execute(cores, "FileParsingTask", lines,
                    (Stream<String> lineStream) -> lines.parallel().forEach(this::parse)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed testParallelStreamWithCustomCore with " + cores + " cores : " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    public void parse(String line) {
        try {
            Node[] nodes = NxParser.parseNodes(line);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void reservoirSamplingFirstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            AtomicInteger lineNo = new AtomicInteger();
            int k = 10;
            ArrayList<String> reservoir = new ArrayList<>();
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    lineNo.getAndIncrement();
                    Node[] nodes = NxParser.parseNodes(line);
                    if (nodes[1].toString().equals(typePredicate)) {
                        if (lineNo.get() < k) {
                            reservoir.add(line);
                        }
                        Random random = new Random();
                        int randomIndex = random.nextInt(lineNo.get() + 1);
                        if (randomIndex < k) {
                            reservoir.add(randomIndex, line);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
            System.out.println(reservoir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed reservoirSamplingFirstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void parallelFileParsing() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).parallel().forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line);
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed parallelFileParsing: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void parsing() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line);
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed parsing: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void bfExperiment() {
        System.out.println("BF Creation Experiment");
        ArrayList<Double> fppSet = new ArrayList<>(Arrays.asList(0.0000001, 0.000001, 0.00001, 0.0001));
        System.out.println("::: FPP,Creation Time (Minutes),Iterating Time (MS)");
        fppSet.forEach(fpp -> {
            this.cteBf = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
            System.out.println("fpp: " + fpp);
            long creationTime = collectEntitiesInBloomFilter(fpp);
            System.out.println("creationTime: " + creationTime);
            double iteratingTime = iterateOverBloomFilters();
            System.out.println("iteratingTime: " + iteratingTime);
            //double iteratingTimeParallel = iterateOverBloomFiltersInParallel();
            //System.out.println("iteratingTimeParallel: " + iteratingTimeParallel);
            System.out.println("::: " + fpp + "," + creationTime + "," + iteratingTime + ",");
        });
    }
    
/*    private void shortEntityStrings() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                // Track classes per entity
                                String entityIri = nodes[0].getLabel();
                                int index = entityIri.lastIndexOf("/");
                                if (index != -1) {
                                    String entityShort = entityIri.substring(index + 1);
                                    if (entityDataHashMapLite.containsKey(entityShort)) {
                                        entityDataHashMapLite.get(entityShort).getClassTypes().add(encoder.encode(nodes[2].getLabel()));
                                    } else {
                                        HashSet<Integer> hashSet = new HashSet<>();
                                        hashSet.add(encoder.encode(nodes[2].getLabel()));
                                        EntityData entityData = new EntityData();
                                        entityData.getClassTypes().addAll(hashSet);
                                        entityDataHashMapLite.put(entityShort, entityData);
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
        System.out.println("Size of shortEntityStrings: " + entityDataHashMapLite.size());
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }*/
    
    private void collectClassEntityCount() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                if (classEntityCount.containsKey(encoder.encode(nodes[2].getLabel()))) {
                                    Integer val = classEntityCount.get(encoder.encode(nodes[2].getLabel()));
                                    classEntityCount.put(encoder.encode(nodes[2].getLabel()), val + 1);
                                } else {
                                    classEntityCount.put(encoder.encode(nodes[2].getLabel()), 1);
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
        System.out.println("Time Elapsed collectClassEntityCount: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private long collectEntitiesInBloomFilter(double falsePositiveProbability) {
        StopWatch watch = new StopWatch();
        watch.start();
        //System.out.println("invoked collectEntitiesInBloomFilter() ");
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                if (cteBf.containsKey(nodes[2])) {
                                    cteBf.get(nodes[2]).add(nodes[0].getLabel());
                                } else {
                                    BloomFilter<String> bf = new FilterBuilder(classEntityCount.get(encoder.encode(nodes[2].getLabel())), falsePositiveProbability).buildBloomFilter();
                                    bf.add(nodes[0].getLabel());
                                    cteBf.put(nodes[2], bf);
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
        //System.out.println("Time Elapsed collectEntitiesInBloomFilter: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return TimeUnit.MILLISECONDS.toMinutes(watch.getTime());
    }
    
    private double iterateOverBloomFilters() {
        StopWatch watch = new StopWatch();
        watch.start();
        //System.out.println("invoked iterateOverBloomFilters() ");
        List<Long> bfIterationTime = new ArrayList<>();
        try {
            Set<String> typesDiscovered = new HashSet<>();
            Stream<String> lines = Files.lines(Path.of(rdfFilePath));
            CustomForEach.forEach(lines, (line, breaker) -> {
                try {
                    if (typesDiscovered.size() > 1000) {
                        System.out.println("Breaking at " + typesDiscovered.size());
                        breaker.stop();
                    } else {
                        Node[] nodes = NxParser.parseNodes(line);
                        Set<String> types = new HashSet<>();
                        StopWatch innerWatch = new StopWatch();
                        if (!typesDiscovered.contains(nodes[0].getLabel())) {
                            typesDiscovered.add(nodes[0].getLabel());
                            innerWatch.start();
                            cteBf.forEach((key, v) -> {
                                if (v.contains(nodes[0].getLabel())) {
                                    types.add(key.getLabel());
                                }
                            });
                            innerWatch.stop();
                            bfIterationTime.add(innerWatch.getTime());
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
        System.out.println("Time Elapsed iterateOverBloomFilters: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return findAverage(bfIterationTime);
    }
    
    private double iterateOverBloomFiltersInParallel() {
        StopWatch watch = new StopWatch();
        watch.start();
        //System.out.println("invoked iterateOverBloomFilters() ");
        List<Long> bfIterationTime = new ArrayList<>();
        try {
            Set<String> typesDiscovered = new HashSet<>();
            Stream<String> lines = Files.lines(Path.of(rdfFilePath));
            CustomForEach.forEach(lines, (line, breaker) -> {
                try {
                    if (typesDiscovered.size() > 1000) {
                        System.out.println("Breaking at " + typesDiscovered.size());
                        breaker.stop();
                    } else {
                        System.out.println(typesDiscovered.size());
                        Node[] nodes = NxParser.parseNodes(line);
                        StopWatch innerWatch = new StopWatch();
                        if (!typesDiscovered.contains(nodes[0].getLabel())) {
                            typesDiscovered.add(nodes[0].getLabel());
                            Set<String> types = new HashSet<>();
                            innerWatch.start();
                            cteBf.entrySet().parallelStream().parallel().forEach(entry -> {
                                BloomFilter<String> v = entry.getValue();
                                if (v.contains(nodes[0].getLabel())) {
                                    types.add(entry.getKey().getLabel());
                                }
                            });
                            innerWatch.stop();
                            bfIterationTime.add(innerWatch.getTime());
                        }
                        //System.out.println("Types of given entity: " + types.size());
                        //System.out.println("Time Elapsed iterateOverBloomFilters: MilliSeconds:" + innerWatch.getTime() + " , Seconds: " + TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed iterateOverBloomFiltersInParallel: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return findAverage(bfIterationTime);
    }
    
    public double findAverage(List<Long> array) {
        double sum = array.stream().mapToDouble(Long::doubleValue).sum();
        System.out.println("Sum: " + sum + ", size: " + array.size());
        return sum / array.size();
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                // Track classes per entity
                                if (entityDataHashMap.containsKey(nodes[0])) {
                                    entityDataHashMap.get(nodes[0]).getClassTypes().add(encoder.encode(nodes[2].getLabel()));
                                } else {
                                    HashSet<Integer> hashSet = new HashSet<>();
                                    hashSet.add(encoder.encode(nodes[2].getLabel()));
                                    EntityData entityData = new EntityData();
                                    entityData.getClassTypes().addAll(hashSet);
                                    entityDataHashMap.put(nodes[0], entityData);
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
    
    private void collectPropertiesOfEntities() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (!nodes[1].toString().equals(typePredicate)) {
                                // Keep track of each property of the node
                                if (entityDataHashMap.get(nodes[0]) != null) {
                                    entityDataHashMap.get(nodes[0]).getProperties().add(encoder.encode(nodes[1].getLabel()));
                                } else {
                                    EntityData entityData = new EntityData();
                                    entityData.getProperties().add(encoder.encode(nodes[1].getLabel()));
                                    entityDataHashMap.put(nodes[0], entityData);
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
  
    
}