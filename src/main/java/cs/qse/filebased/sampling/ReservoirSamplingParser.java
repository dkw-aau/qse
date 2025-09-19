package cs.qse.filebased.sampling;

import cs.Main;
import cs.qse.common.*;
import cs.qse.filebased.*;
import cs.utils.Constants;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.qse.common.encoders.StringEncoder;
import cs.qse.common.encoders.NodeEncoder;
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
    StringEncoder stringEncoder;
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
    
    Map<Integer, Integer> propCount; // real count of *all (entire graph)* triples having predicate P   // |P| =  |< _, P , _ >| in G
    Map<Integer, Integer> sampledPropCount; // count of triples having predicate P across all entities in all reservoirs  |< _ , P , _ >| (the sampled entities)
    
    public ReservoirSamplingParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate, boolean addExaples, List<String> labelPredicates, Integer entitySamplingThreshold) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.sampledEntitiesPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMapContainer = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.propCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        this.sampledPropCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        this.stringEncoder = new StringEncoder();
        this.nodeEncoder = new NodeEncoder();
        this.maxEntityThreshold = entitySamplingThreshold;
        exampleManager = new ExampleManager(stringEncoder);
        this.addExamples = addExaples;
        this.labelPredicates = labelPredicates;
        random = new Random();
    }
    
    public void run() {
        System.out.println("initiated:ReservoirSamplingParser");
        runParser();
    }
    
    private void runParser() {
        dynamicNbReservoirSampling();
        entityConstraintsExtraction();
        // Store the labels only if example creation is required
        if(addExamples){
            storeLabels();
        }
        computeSupportConfidence();
        extractSHACLShapes(true, Main.qseFromSpecificClasses);
        Utility.writeClassFrequencyInFile(classEntityCount, stringEncoder);
    }
    
    protected void standardReservoirSampling() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        StandardReservoirSampling srs = new StandardReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, nodeEncoder, stringEncoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = stringEncoder.encode(nodes[2].getLabel());
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
    
    private void neighborBasedReservoirSampling() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        NeighborBasedReservoirSampling brs = new NeighborBasedReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, nodeEncoder, stringEncoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = stringEncoder.encode(nodes[2].getLabel());
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
        Utils.logTime("firstPass:neighborBasedReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void dynamicNbReservoirSampling() {
        System.out.println("invoked:dynamicNbReservoirSampling()");
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        this.reservoirCapacityPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int minEntityThreshold = 1;
        int samplingPercentage = Main.entitySamplingTargetPercentage;
        DynamicNeighborBasedReservoirSampling drs = new DynamicNeighborBasedReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, reservoirCapacityPerClass, nodeEncoder, stringEncoder);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                   
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        int objID = stringEncoder.encode(nodes[2].getLabel());
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
                        propCount.merge(stringEncoder.encode(nodes[1].getLabel()), 1, Integer::sum); // Get the
                    }
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (addExamples) {
            // Adding example-nodes for each class selecting only nodes already sampled(only nodes contained in [sampledEntitiesPerClass])
            sampledEntitiesPerClass.forEach((encodedClassIRI, encodedNodes) -> {
                encodedNodes.forEach(encodedNode -> {
                    Node exampleNode = nodeEncoder.decode(encodedNode);

                    // ADDING EXAMPLE NODES USING RESERVOIR SAMPLING
                    // If maximum example-node capacity has not been reached, add the current node to the example-nodes
                    if (exampleManager.exampleNodeCount(encodedClassIRI) < ExampleManager.EXAMPLES_FOR_CLASS) {
                        exampleManager.addExampleNode(encodedClassIRI, exampleNode);
                    }
                    // Otherwise, replace an example-node with the current node (the probability of this to happen is EXAMPLES_FOR_CLASS/totalClassNodeCount)
                    else {
                        int candidateIndex = random.nextInt(exampleManager.totalNodeCount(encodedClassIRI));
                        exampleManager.replaceExampleNode(candidateIndex, encodedClassIRI, exampleNode);
                    }
                });
            });

            // List all the example-nodes
            exampleManager.listExampleNodes();
        }

        watch.stop();
        Utils.logTime("firstPass:dynamicNbReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logSamplingStats("dynamicNbReservoirSampling", samplingPercentage, minEntityThreshold, maxEntityThreshold, entityDataMapContainer.size());
    }
    
    private void prepareStatistics() {
        classEntityCount.forEach((classIRI, entityCount) -> {
            String log = "LOG:: " + classIRI + "," + stringEncoder.decode(classIRI) + "," + entityCount + "," + sampledEntitiesPerClass.get(classIRI).size() + "," + reservoirCapacityPerClass.get(classIRI);
            Utils.writeLineToFile(log, Constants.THE_LOGS);
        });
    }
    
    @Override
    protected void entityConstraintsExtraction() {
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
                            int propID = stringEncoder.encode(nodes[1].getLabel());
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

                                if (addExamples) {
                                    // For each class of the subject node([encodedNode]), set the property type to NON-literal
                                    Integer encodedNode = nodeEncoder.encode(nodes[0]);
                                    Set<Integer> classIDs = entityDataMapContainer.get(encodedNode).getClassTypes();
                                    classIDs.forEach(encodedClassIRI -> {
                                        exampleManager.setPropertyType(encodedClassIRI, propID, stringEncoder.encode(objectType));
                                        exampleManager.setPropertyIsLiteral(encodedClassIRI, propID, false);
                                    });
                                }
                            } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                                int objID = stringEncoder.encode(objectType);
                                objTypes.add(objID);
                                prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                                addEntityToPropertyConstraints(prop2objTypeTuples, subjID);

                                if (addExamples) {
                                    // For each class of the subject node, set the property type to literal
                                    Integer encodedNode = nodeEncoder.encode(nodes[0]);
                                    Set<Integer> classIDs = entityDataMapContainer.get(encodedNode).getClassTypes();
                                    classIDs.forEach(encodedClassIRI -> {
                                        exampleManager.setPropertyType(encodedClassIRI, propID, stringEncoder.encode(objectType));
                                        exampleManager.setPropertyIsLiteral(encodedClassIRI, propID, true);
                                    });
                                }
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
                            sampledPropCount.merge(propID, 1, Integer::sum); // Get the

                            if (addExamples) {
                                Node targetNode = nodes[0];
                                String property = nodes[1].getLabel();
                                String actualValue = nodes[2].getLabel();
                                String rawValue = nodes[2].toString();
                                Integer encodedNode = nodeEncoder.getEncodedNode(targetNode);
                                Integer encodedProperty = stringEncoder.encode(property);
                                Integer encodedActualValue = stringEncoder.encode(actualValue);
                                Integer encodedRawValue = stringEncoder.encode(rawValue);

                                // ADDING VALUES TO PROPERTIES OF EXAMPLE-NODES
                                // If the current node(subject) is an example-node, store his data
                                if (exampleManager.isExampleNode(targetNode)) {
                                    exampleManager.addNodeData(targetNode, encodedProperty, encodedActualValue, encodedRawValue);
                                }

                                // ADDING EXAMPLE VALUES TO PROPERTIES WITH RESERVOIR SAMPLING
                                // For each class of the current node(subject), add the property value as an example value for that class
                                Set<Integer> classIDs = entityDataMapContainer.get(encodedNode).getClassTypes();
                                classIDs.forEach(encodedClassIRI -> {
                                    // If the maximum capacity of example-values has not been reached, add the current property value to the example-values
                                    if (exampleManager.propertyValuesCount(encodedClassIRI, encodedProperty) < ExampleManager.EXAMPLES_FOR_PROPERTY) {
                                        exampleManager.addPropertyExample(encodedClassIRI, encodedProperty, encodedActualValue, encodedRawValue);
                                    }
                                    // Otherwise, replace an example-value of the property with the current value (the probability of this to happen is EXAMPLES_FOR_PROPERTY/totalValuesCount)
                                    else {
                                        int candidateIndex = random.nextInt(exampleManager.totalPropertyValueCount(encodedClassIRI, encodedProperty));
                                        exampleManager.replacePropertyExample(candidateIndex, encodedClassIRI, encodedProperty, encodedActualValue, encodedRawValue);
                                    }
                                });
                            }
                        } // if condition for presence of node in the reservoir ends here
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (addExamples) {
                // List all the example-values of all properties
                exampleManager.listPropertiesValues();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass:cs.qse.filebased.sampling.ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }


    /**
     * Iterating a third time on the RDF to store the labels
     * Given the triple (s,p,o) where s=IRI, p=rdf:label, o=label, store the label for the subject s.
     * N.B. ONLY labels useful in example creation will be stored. So, a label is stored only if the subject is:
     *  -a class, or
     *  -a property, or
     *  -an example-node, or
     *  -a value stored as example-value of a property
     */
    protected void storeLabels() {
        System.out.println("Storing labels");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (labelPredicates.contains(nodes[1].toString())) { // Check if predicate is rdf:label or equivalent
                        Node subject = nodes[0];
                        Node object = nodes[2];
                        Integer encodedSubject = stringEncoder.encode(subject.getLabel());
                        Integer encodedRawSubject = stringEncoder.encode(subject.toString());
                        Integer encodedLabel = stringEncoder.encode(object.getLabel());

                        /* Check the type of the label subject:
                            if the subject is a class, a property(predicate), an example-node or a value stored as example-value,
                            then store the label
                         */
                        if (exampleManager.isClass(encodedSubject) || exampleManager.isProperty(encodedSubject) || exampleManager.isExampleNode(subject) ||exampleManager.isPropertyValue(encodedSubject, encodedRawSubject)){
                            exampleManager.storeLabel(encodedSubject, encodedLabel);
                        }
                        // Otherwise DON'T store the label
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("Label storing", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
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
        Utils.logTime("computeSupportConfidence:cs.qse.filebased.sampling.ReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    @Override
    protected void extractSHACLShapes(Boolean performPruning, Boolean qseFromSpecificClasses) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:cs.qse.filebased.sampling.ReservoirSampling: No Pruning";
        ShapesExtractor se = new ShapesExtractor(stringEncoder, shapeTripletSupport, classEntityCount, typePredicate);
        se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        //====================== Enable shapes extraction for specific classes ======================
        if (qseFromSpecificClasses)
            classToPropWithObjTypes = Utility.extractShapesForSpecificClasses(classToPropWithObjTypes, classEntityCount, stringEncoder);
        se.constructDefaultShapes(classToPropWithObjTypes, exampleManager); // SHAPES without performing pruning based on confidence and support thresholds
        se.setPropCount(propCount);
        se.setSampledPropCount(sampledPropCount);
        se.setSampledEntitiesPerClass(sampledEntitiesPerClass);
        se.setSamplingOn(true);
        if (performPruning) {
            StopWatch watchForPruning = new StopWatch();
            watchForPruning.start();
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    se.constructPrunedShapes(classToPropWithObjTypes, exampleManager, conf, supp);
                });
            });
            methodName = "extractSHACLShapes:cs.qse.filebased.sampling.ReservoirSampling";
            watchForPruning.stop();
            Utils.logTime(methodName+"-Time.For.Pruning.Only", TimeUnit.MILLISECONDS.toSeconds(watchForPruning.getTime()), TimeUnit.MILLISECONDS.toMinutes(watchForPruning.getTime()));
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

