package cs.qse;

import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReservoirSamplingParser extends Parser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;
    NodeEncoder nodeEncoder;
    Integer entityThreshold = 100;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Integer, EntityData> entityDataMap; // Size == N For every entity (encoded as integer) we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, List<Integer>> class2SampledEntityCount; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    
    
    public ReservoirSamplingParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.class2SampledEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        firstPass();
        //secondPass();
        //computeSupportConfidence();
        //extractSHACLShapes(false);
        //assignCardinalityConstraints();
        System.out.println("No. of Classes: Total: " + classEntityCount.size());
        
    }
    
    @Override
    protected void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        AtomicInteger lineCounter = new AtomicInteger();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        class2SampledEntityCount.putIfAbsent(objID, new ArrayList<>(entityThreshold));
                        int numberOfSampledEntities = class2SampledEntityCount.get(objID).size();
                        
                        
                        if (numberOfSampledEntities < entityThreshold) {
                            int subjID = nodeEncoder.encode(nodes[0]);
                            // Track classes per entity
                            EntityData entityData = entityDataMap.get(subjID);
                            if (entityData == null) {
                                entityData = new EntityData();
                            }
                            entityData.getClassTypes().add(objID);
                            entityDataMap.put(subjID, entityData);
                            class2SampledEntityCount.get(objID).add(subjID);
                            //System.out.println("Inserting " + subjID + " " + nodeEncoder.decode(subjID) + " for " + objID);
                            //System.out.println("1. Types After Insertion: " + encodedEntityDataMap.get(subjID).getClassTypes());
                        } else {
                            int candidateIndex = new Random().nextInt(lineCounter.get()); //The nextInt(int n) is used to get a random number between 0 (inclusive) and the number passed in this argument(n), exclusive.
                            if (candidateIndex < class2SampledEntityCount.get(objID).size()) {
                                int candidate = class2SampledEntityCount.get(objID).get(candidateIndex);
                                //FIXME : It should never be null
                                if (entityDataMap.get(candidate) != null) {
                                    //Removal
                                    entityDataMap.get(candidate).getClassTypes().forEach(obj -> {
                                        if (class2SampledEntityCount.containsKey(obj)) {
                                            //System.out.println("Removing " + candidate + " " + nodeEncoder.decode(candidate) + " from " + obj + " : " + objID);
                                            class2SampledEntityCount.get(obj).remove(Integer.valueOf(candidate));
                                        }
                                    });
                                    //System.out.println("Types: " + encodedEntityDataMap.get(candidate).getClassTypes());
                                    entityDataMap.remove(candidate);
                                    boolean status = nodeEncoder.remove(candidate);
                                    if (!status)
                                        System.out.println("Failed to remove the node with id: " + candidate);
                                    
                                    //Update or Addition
                                    int subjID = nodeEncoder.encode(nodes[0]);
                                    EntityData entityData = entityDataMap.get(subjID);
                                    if (entityData == null) {
                                        entityData = new EntityData();
                                    }
                                    entityData.getClassTypes().add(objID);
                                    entityDataMap.put(subjID, entityData);
                                    class2SampledEntityCount.get(objID).add(subjID);
                                    //System.out.println("Inserting " + subjID + " " + nodeEncoder.decode(subjID) + " for " + objID);
                                    //System.out.println("2. Types After Insertion: " + encodedEntityDataMap.get(subjID).getClassTypes());
                                } else {
                                    class2SampledEntityCount.forEach((k, v) -> {
                                        if (v.contains(candidate)) {
                                            System.out.println("Class " + k + " : " + encoder.decode(k) + " has candidate " + candidate);
                                        }
                                    });
                                    System.out.println("It's null " + candidate);
                                }
                            }
                        }
                        classEntityCount.merge(objID, 1, Integer::sum);
                    }
                    lineCounter.getAndIncrement();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("encodedEntityDataMap.size(): " + NumberFormat.getInstance().format(entityDataMap.size()));
        System.out.println("nodeEncoder.getTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getTable().size()));
        System.out.println("nodeEncoder.getReverseTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getReverseTable().size()));
        System.out.println("nodeEncoder.counter: " + NumberFormat.getInstance().format(nodeEncoder.counter));
        watch.stop();
        Utils.logTime("firstPass:RandomSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    @Override
    protected void secondPass() {
    
    }
}
