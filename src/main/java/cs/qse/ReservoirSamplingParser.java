package cs.qse;

import cs.Main;
import cs.qse.experiments.ExperimentsUtil;
import cs.utils.Constants;
import cs.utils.Tuple2;
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
    Integer entityThreshold;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Integer, EntityData> entityDataMapContainer; // Size == N For every entity (encoded as integer) we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, List<Integer>> classSampledEntityReservoir; // Size == O(T*entityThreshold)
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    
    
    public ReservoirSamplingParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate, Integer entitySamplingThreshold) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classSampledEntityReservoir = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMapContainer = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
        this.entityThreshold = entitySamplingThreshold;
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        System.out.println("Entity Sampling Threshold : " + entityThreshold);
        //firstPass();
        firstPassBullyApproach();
        secondPass();
        computeSupportConfidence();
        extractSHACLShapes(false);
        //assignCardinalityConstraints();
        System.out.println("No. of Classes: Total: " + classEntityCount.size());
        
    }
    
    @Override
    protected void firstPass() {
        System.out.println("FirstPass");
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line);
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        classSampledEntityReservoir.putIfAbsent(objID, new ArrayList<>(entityThreshold));
                        int numberOfSampledEntities = classSampledEntityReservoir.get(objID).size();
                        // Initializing entityDataMapContainer with first k = entityThreshold elements for each class
                        if (numberOfSampledEntities < entityThreshold) {
                            int subjID = nodeEncoder.encode(nodes[0]); // encoding subject
                            EntityData entityData = entityDataMapContainer.get(subjID); // Track classes per entity
                            if (entityData == null) {
                                entityData = new EntityData();
                            }
                            entityData.getClassTypes().add(objID);
                            entityDataMapContainer.put(subjID, entityData);
                            classSampledEntityReservoir.get(objID).add(subjID);
                        }
                        // once the reservoir (entityDataMap) is filled with entities upto the defined entityThreshold for specific classes, we enter the else block
                        //  Now one by one consider all items from (k+1)th item to nth item.
                        //      a) Generate a random number from 0 to i where i is the index of the current item in stream, i.e., lineCounter.
                        //      Let the generated random number is candidateIndex.
                        //	    b) If candidateIndex is in range 0 to number of entities sampled for the current class (obtained from classSampledEntityReservoir)
                        //	    replace the candidateNode at candidateIndex in the reservoir with current node , additionally remove the candidate node from classSampledEntityReservoir
                        
                        else {
                            int candidateIndex = random.nextInt(lineCounter.get()); //The nextInt(int n) is used to get a random number between 0 (inclusive) and the number passed in this argument(n), exclusive.
                            int currSize = classSampledEntityReservoir.get(objID).size();
                            
                            //Rolling the dice again by generating another random number
                            if (candidateIndex > currSize) {
                                candidateIndex = random.nextInt(lineCounter.get());
                            }
                            
                            if (candidateIndex < currSize) {
                                int candidateNode = classSampledEntityReservoir.get(objID).get(candidateIndex); // get the candidate node at the candidate index
                                
                                if (entityDataMapContainer.get(candidateNode) != null) {
                                    //Remove the candidate node from the classSampledEntityReservoir
                                    entityDataMapContainer.get(candidateNode).getClassTypes().forEach(obj -> {
                                        if (classSampledEntityReservoir.containsKey(obj)) {
                                            classSampledEntityReservoir.get(obj).remove(Integer.valueOf(candidateNode));
                                        }
                                    });
                                    
                                    entityDataMapContainer.remove(candidateNode); // Remove the candidate node from the entityDataMapContainer
                                    boolean status = nodeEncoder.remove(candidateNode);
                                    if (!status)
                                        System.out.println("WARNING::Failed to remove the candidateNode: " + candidateNode);
                                    
                                    //Update the reservoir and container with the current focus node
                                    int subjID = nodeEncoder.encode(nodes[0]); // Encode the current focus node
                                    EntityData entityData = entityDataMapContainer.get(subjID);
                                    if (entityData == null) {
                                        entityData = new EntityData();
                                    }
                                    entityData.getClassTypes().add(objID);
                                    entityDataMapContainer.put(subjID, entityData); // Add the focus node in the reservoir
                                    classSampledEntityReservoir.get(objID).add(subjID); // Update the classSampledEntityReservoir with the current focus node for current class
                                } else {
                                    System.out.println("WARNING::It's null for candidateNode " + candidateNode);
                                    classSampledEntityReservoir.forEach((k, v) -> {
                                        if (v.contains(candidateNode))
                                            System.out.println("Class " + k + " : " + encoder.decode(k) + " has candidate " + candidateNode);
                                    });
                                }
                            }
                        }
                        classEntityCount.merge(objID, 1, Integer::sum); // Get the real entity count for current class
                    }
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass:ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
        //Printing some statistics
        System.out.println("entityDataMapContainer.size(): " + NumberFormat.getInstance().format(entityDataMapContainer.size()));
        System.out.println("nodeEncoder.getTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getTable().size()));
        System.out.println("nodeEncoder.getReverseTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getReverseTable().size()));
        System.out.println("nodeEncoder.counter: " + NumberFormat.getInstance().format(nodeEncoder.counter));
    }
    
    private void firstPassBullyApproach() {
        System.out.println("FirstPass: Bully Approach");
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line);
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        classSampledEntityReservoir.putIfAbsent(objID, new ArrayList<>(entityThreshold));
                        int numberOfSampledEntities = classSampledEntityReservoir.get(objID).size();
                        // Initializing entityDataMapContainer with first k = entityThreshold elements for each class
                        if (numberOfSampledEntities < entityThreshold) {
                            int subjID = nodeEncoder.encode(nodes[0]); // encoding subject
                            EntityData entityData = entityDataMapContainer.get(subjID); // Track classes per entity
                            if (entityData == null) {
                                entityData = new EntityData();
                            }
                            entityData.getClassTypes().add(objID);
                            entityDataMapContainer.put(subjID, entityData);
                            classSampledEntityReservoir.get(objID).add(subjID);
                        }
                        // once the reservoir (entityDataMap) is filled with entities upto the defined entityThreshold for specific classes, we enter the else block
                        //  Now one by one consider all items from (k+1)th item to nth item.
                        //      a) Generate a random number from 0 to i where i is the index of the current item in stream, i.e., lineCounter.
                        //      Let the generated random number is candidateIndex.
                        //	    b) If candidateIndex is in range 0 to number of entities sampled for the current class (obtained from classSampledEntityReservoir)
                        //	    replace the candidateNode at candidateIndex in the reservoir with current node , additionally remove the candidate node from classSampledEntityReservoir
                        
                        else {
                            int candidateIndex = random.nextInt(lineCounter.get()); //The nextInt(int n) is used to get a random number between 0 (inclusive) and the number passed in this argument(n), exclusive.
                            int currSize = classSampledEntityReservoir.get(objID).size();
                            
                            if (candidateIndex < currSize) {
                                int candidateNodeLeft = -1, candidateNodeRight = -1, scopeCandidateNodeLeft = 999999999, scopeCandidateNodeRight = 999999999;
                                
                                if (candidateIndex != 0) {
                                    candidateNodeLeft = classSampledEntityReservoir.get(objID).get(candidateIndex - 1); // get the candidate node at the left of candidate index
                                    if (entityDataMapContainer.get(candidateNodeLeft) != null) {
                                        scopeCandidateNodeLeft = entityDataMapContainer.get(candidateNodeLeft).getClassTypes().size();
                                    }
                                }
                                if (candidateIndex != currSize - 1) {
                                    candidateNodeRight = classSampledEntityReservoir.get(objID).get(candidateIndex + 1); // get the candidate node at the right of candidate index
                                    if (entityDataMapContainer.get(candidateNodeRight) != null) {
                                        scopeCandidateNodeRight = entityDataMapContainer.get(candidateNodeRight).getClassTypes().size();
                                    }
                                }
                                
                                int candidateNode = classSampledEntityReservoir.get(objID).get(candidateIndex); // get the candidate node at the candidate index
                                
                                if (entityDataMapContainer.get(candidateNode) != null) {
                                    int scopeCandidateNode = entityDataMapContainer.get(candidateNode).getClassTypes().size();
                                    
                                    BinaryNode node = new BinaryNode(candidateNode, scopeCandidateNode);
                                    node.left = new BinaryNode(candidateNodeLeft, scopeCandidateNodeLeft);
                                    node.right = new BinaryNode(candidateNodeRight, scopeCandidateNodeRight);
                                    
                                    BinaryNode min = nodeWithMinimumScope(node, node.left, node.right);
                                    //System.out.println(min.id + " - " + min.scope);
                                    candidateNode = min.id;
                                    
                                    //Remove the candidate node from the classSampledEntityReservoir
                                    for (Integer obj : entityDataMapContainer.get(candidateNode).getClassTypes()) {
                                        if (classSampledEntityReservoir.containsKey(obj)) {
                                            classSampledEntityReservoir.get(obj).remove(Integer.valueOf(candidateNode));
                                        }
                                    }
                                    
                                    entityDataMapContainer.remove(candidateNode); // Remove the candidate node from the entityDataMapContainer
                                    boolean status = nodeEncoder.remove(candidateNode);
                                    if (!status)
                                        System.out.println("WARNING::Failed to remove the candidateNode: " + candidateNode);
                                    
                                    //Update the reservoir and container with the current focus node
                                    int subjID = nodeEncoder.encode(nodes[0]); // Encode the current focus node
                                    EntityData entityData = entityDataMapContainer.get(subjID);
                                    if (entityData == null) {
                                        entityData = new EntityData();
                                    }
                                    entityData.getClassTypes().add(objID);
                                    entityDataMapContainer.put(subjID, entityData); // Add the focus node in the reservoir
                                    classSampledEntityReservoir.get(objID).add(subjID); // Update the classSampledEntityReservoir with the current focus node for current class
                                } else {
                                    System.out.println("WARNING::It's null for candidateNode " + candidateNode);
                                    for (Map.Entry<Integer, List<Integer>> entry : classSampledEntityReservoir.entrySet()) {
                                        Integer k = entry.getKey();
                                        List<Integer> v = entry.getValue();
                                        if (v.contains(candidateNode))
                                            System.out.println("Class " + k + " : " + encoder.decode(k) + " has candidate " + candidateNode);
                                    }
                                }
                            }
                        }
                        classEntityCount.merge(objID, 1, Integer::sum); // Get the real entity count for current class
                    }
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass:ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
        //Printing some statistics
        System.out.println("entityDataMapContainer.size(): " + NumberFormat.getInstance().format(entityDataMapContainer.size()));
        System.out.println("nodeEncoder.getTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getTable().size()));
        System.out.println("nodeEncoder.getReverseTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getReverseTable().size()));
        System.out.println("nodeEncoder.counter: " + NumberFormat.getInstance().format(nodeEncoder.counter));
    }
    
    @Override
    protected void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).filter(line -> !line.contains(typePredicate)).forEach(line -> {
                try {
                    //Declaring required sets
                    Set<Integer> objTypes = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                    //Node subject = nodes[0];
                    int subjID = nodeEncoder.encode(nodes[0]);
                    // if the entity is in the Reservoir, we go for it
                    if (entityDataMapContainer.get(subjID) != null) {
                        String objectType = extractObjectType(nodes[2].toString());
                        int propID = encoder.encode(nodes[1].getLabel());
                        if (objectType.equals("IRI")) { // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                            EntityData currEntityData = entityDataMapContainer.get(nodeEncoder.encode(nodes[2]));
                            if (currEntityData != null) {
                                objTypes = currEntityData.getClassTypes();
                                for (Integer node : objTypes) { // get classes of node2
                                    prop2objTypeTuples.add(new Tuple2<>(propID, node));
                                }
                                addEntityToPropertyConstraints(prop2objTypeTuples, subjID);
                            }
                            /*else { // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*/
                            
                        } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                            int objID = encoder.encode(objectType);
                            objTypes.add(objID);
                            prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                            addEntityToPropertyConstraints(prop2objTypeTuples, subjID);
                        }
                        
                        EntityData entityData = entityDataMapContainer.get(subjID);
                        if (entityData != null) {
                            for (Integer entityClass : entityData.getClassTypes()) {
                                Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.get(entityClass);
                                if (propToObjTypes == null) {
                                    propToObjTypes = new HashMap<>();
                                    classToPropWithObjTypes.put(entityClass, propToObjTypes);
                                }
                                
                                Set<Integer> classObjTypes = propToObjTypes.get(propID);
                                if (classObjTypes == null) {
                                    classObjTypes = new HashSet<>();
                                    propToObjTypes.put(propID, classObjTypes);
                                }
                                
                                classObjTypes.addAll(objTypes);
                            }
                        }
                    } // if condition for presence of node in the reservoir ends here
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass:ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * * A utility method to add property constraints of each entity in the 2nd pass
     *
     * @param prop2objTypeTuples
     * @param subject
     */
    private void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Integer subject) {
        EntityData currentEntityData = entityDataMapContainer.get(subject);
        if (currentEntityData == null) {
            currentEntityData = new EntityData();
        }
        //Add Property Constraint and Property cardinality
        for (Tuple2<Integer, Integer> tuple2 : prop2objTypeTuples) {
            currentEntityData.addPropertyConstraint(tuple2._1, tuple2._2);
            if (Main.extractMaxCardConstraints) {
                currentEntityData.addPropertyCardinality(tuple2._1);
            }
        }
        //Add entity data into the map
        entityDataMapContainer.put(subject, currentEntityData);
    }
    
    /**
     * Computing support and confidence using the metadata extracted in the 2nd pass for shape constraints
     */
    @Override
    public void computeSupportConfidence() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.statsComputer = new StatsComputer();
        statsComputer.setShapeTripletSupport(shapeTripletSupport);
        statsComputer.computeSupportConfidenceWithEncodedEntities(entityDataMapContainer, classEntityCount);
        watch.stop();
        Utils.logTime("computeSupportConfidence:ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    @Override
    protected void extractSHACLShapes(Boolean performPruning) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:ReservoirSampling: No Pruning";
        ShapesExtractor se = new ShapesExtractor(encoder, shapeTripletSupport, classEntityCount);
        se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                });
            });
            methodName = "extractSHACLShapes:ReservoirSampling";
        }
        
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    
    public BinaryNode nodeWithMinimumScope(BinaryNode a, BinaryNode b, BinaryNode c) {
        BinaryNode smallest;
        if (a.scope < b.scope) {
            if (c.scope < a.scope) {
                smallest = c;
            } else {
                smallest = a;
            }
        } else {
            if (b.scope < c.scope) {
                smallest = b;
            } else {
                smallest = c;
            }
        }
        return smallest;
    }
}

class BinaryNode {
    int id;
    int scope;
    BinaryNode left, right;
    
    BinaryNode(int id, int scope) {
        this.id = id;
        this.scope = scope;
        left = null;
        right = null;
    }
}
