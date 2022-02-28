package cs.qse.sampling;

import cs.Main;
import cs.qse.*;
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
    Integer maxEntityThreshold;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Integer, EntityData> entityDataMapContainer; // Size == N For every entity (encoded as integer) we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, List<Integer>> sampledEntitiesPerClass; // Size == O(T*entityThreshold)
    Map<Integer, Integer> reservoirCapacityPerClass; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    
    Map<Integer, Integer> propCount;
    
    public ReservoirSamplingParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate, Integer entitySamplingThreshold) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.sampledEntitiesPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMapContainer = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.propCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
        this.maxEntityThreshold = entitySamplingThreshold;
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        //standardReservoirSampling();
        //bullyReservoirSampling();
        dynamicBullyReservoirSampling();
        //prepareStatistics();
        //printSampledEntitiesLogs();
        secondPass();
        //printSampledEntitiesLogs();
        computeSupportConfidence();
        extractSHACLShapes(true);
    }
    
    protected void standardReservoirSampling() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        StandardReservoirSampling srs = new StandardReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, nodeEncoder, encoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        sampledEntitiesPerClass.putIfAbsent(objID, new ArrayList<>(maxEntityThreshold));
                        int numberOfSampledEntities = sampledEntitiesPerClass.get(objID).size();
                        
                        if (numberOfSampledEntities < maxEntityThreshold) { // Initializing entityDataMapContainer with first k = entityThreshold elements for each class
                            srs.sample(nodes);
                        } else // once the reservoirs are filled with entities upto the defined entityThreshold for specific classes, we enter the else block
                        {
                            srs.replace(random.nextInt(lineCounter.get()), nodes);
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
        Utils.logTime("firstPass:StandardReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void bullyReservoirSampling() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        BullyReservoirSampling brs = new BullyReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, nodeEncoder, encoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        sampledEntitiesPerClass.putIfAbsent(objID, new ArrayList<>(maxEntityThreshold));
                        int numberOfSampledEntities = sampledEntitiesPerClass.get(objID).size();
                        
                        // Initializing entityDataMapContainer with first k = entityThreshold elements for each class
                        if (numberOfSampledEntities < maxEntityThreshold) {
                            brs.sample(nodes);
                        } else { // once the reservoir (entityDataMap) is filled with entities upto the defined entityThreshold for specific classes, we enter the else block
                            brs.replace(random.nextInt(lineCounter.get()), nodes);
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
        Utils.logTime("firstPass:bullyReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void dynamicBullyReservoirSampling() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        this.reservoirCapacityPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int minEntityThreshold = 1;
        int samplingPercentage = 25;
        DynamicBullyReservoirSampling drs = new DynamicBullyReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, reservoirCapacityPerClass, nodeEncoder, encoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                   
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = encoder.encode(nodes[2].getLabel());
                        sampledEntitiesPerClass.putIfAbsent(objID, new ArrayList<>(maxEntityThreshold));
                        reservoirCapacityPerClass.putIfAbsent(objID, minEntityThreshold);
                        
                        if (sampledEntitiesPerClass.get(objID).size() < reservoirCapacityPerClass.get(objID)) {
                            drs.sample(nodes);
                        } else {
                            drs.replace(random.nextInt(lineCounter.get()), nodes);
                        }
                        classEntityCount.merge(objID, 1, Integer::sum); // Get the real entity count for current class
                        drs.resizeReservoir(classEntityCount.get(objID), sampledEntitiesPerClass.get(objID).size(), maxEntityThreshold, samplingPercentage, objID);
                    } else {
                        propCount.merge(encoder.encode(nodes[1].getLabel()), 1, Integer::sum); // Get the
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
        Utils.logTime("firstPass:dynamicBullyReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logSamplingStats("dynamicBullyReservoirSampling", samplingPercentage, minEntityThreshold, maxEntityThreshold, entityDataMapContainer.size());
    }
    
    private void prepareStatistics() {
        classEntityCount.forEach((classIRI, entityCount) -> {
            String log = "LOG:: " + classIRI + "," + encoder.decode(classIRI) + "," + entityCount + "," + sampledEntitiesPerClass.get(classIRI).size() + "," + reservoirCapacityPerClass.get(classIRI);
            Utils.writeLineToFile(log, Constants.THE_LOGS);
        });
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
                    if (nodeEncoder.isNodeExists(nodes[0])) {
                        int subjID = nodeEncoder.getEncodedNode(nodes[0]);
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
                    }
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass:cs.qse.sampling.ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    //A utility method to add property constraints of each entity in the 2nd pass
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
    
    //Computing support and confidence using the metadata extracted in the 2nd pass for shape constraints
    @Override
    public void computeSupportConfidence() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.statsComputer = new StatsComputer();
        statsComputer.setShapeTripletSupport(shapeTripletSupport);
        statsComputer.setSampledEntityCount(sampledEntitiesPerClass);
        statsComputer.setSamplingOn(true);
        statsComputer.computeSupportConfidenceWithEncodedEntities(entityDataMapContainer, classEntityCount);
        watch.stop();
        Utils.logTime("computeSupportConfidence:cs.qse.sampling.ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    @Override
    protected void extractSHACLShapes(Boolean performPruning) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:cs.qse.sampling.ReservoirSampling: No Pruning";
        ShapesExtractor se = new ShapesExtractor(encoder, shapeTripletSupport, classEntityCount);
        se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        se.setPropCount(propCount);
        se.setSampledEntitiesPerClass(sampledEntitiesPerClass);
        se.setSamplingOn(true);
        if (performPruning) {
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                });
            });
            methodName = "extractSHACLShapes:cs.qse.sampling.ReservoirSampling";
        }
        
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void printSampledEntitiesLogs() {
        System.out.println("entityDataMapContainer.size(): " + NumberFormat.getInstance().format(entityDataMapContainer.size()));
        System.out.println("nodeEncoder.getTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getTable().size()));
        System.out.println("nodeEncoder.getReverseTable().size(): " + NumberFormat.getInstance().format(nodeEncoder.getReverseTable().size()));
        System.out.println("nodeEncoder.counter: " + NumberFormat.getInstance().format(nodeEncoder.counter));
    }
}

