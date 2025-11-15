package cs.qse.filebased;

import cs.Main;
import cs.qse.common.*;
import cs.qse.common.encoders.StringEncoder;
import cs.utils.*;
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
import java.util.concurrent.TimeUnit;

/**
 * This class parses RDF NT file triples to extract SHACL shapes, compute the confidence/support for shape constraints,
 * and perform node and property shape constraints pruning based on defined threshold for confidence and support
 */
public class Parser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    StringEncoder stringEncoder;
    StatsComputer statsComputer;
    String typePredicate;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates

    Map<Node, EntityData> entityDataHashMap; // Size == N For every entity we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence

    // OBJECTS FOR EXAMPLE CREATION
    // If true, add examples to the extracted shapes
    protected boolean addExamples;
    // List of all predicates used to identify labels
    protected List<String> labelPredicates;
    // Random number generator (for Reservoir Sampling)
    protected Random random;
    // This object contains all data and methods used for examples creation
    protected ExampleManager exampleManager;


    public Parser() {}
    
    /**
     * ============================================= Constructor ========================================
     */
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate, boolean addExamples, List<String> labelPredicates) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.stringEncoder = new StringEncoder();
        exampleManager = new ExampleManager(stringEncoder);
        this.addExamples = addExamples;
        this.labelPredicates = labelPredicates;
        random = new Random();
    }
    
    /**
     * ============================================= Run Parser ========================================
     */
    public void run() {
        entityExtraction();
        entityConstraintsExtraction();
        // Store the labels only if example creation is required
        if (addExamples){
            storeLabels();
        }
        computeSupportConfidence();
        extractSHACLShapes(true, Main.qseFromSpecificClasses);
        //assignCardinalityConstraints();
        Utility.writeClassFrequencyInFile(classEntityCount, stringEncoder);
        System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }


    /**
     * ============================================= Phase 1: Entity Extraction ========================================
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to extract set of entity types and frequency of each entity.
     * =================================================================================================================
     */
    protected void entityExtraction() {
        System.out.println("invoked::firstPass()");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line); // Get [S,P,O] as Node from triple
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        // Track classes per entity
                        int objID = stringEncoder.encode(nodes[2].getLabel());
                        EntityData entityData = entityDataHashMap.get(nodes[0]);
                        if (entityData == null) {
                            entityData = new EntityData();
                        }
                        entityData.getClassTypes().add(objID);
                        entityDataHashMap.put(nodes[0], entityData);
                        classEntityCount.merge(objID, 1, Integer::sum);

                        if (addExamples) {
                            Integer encodedClassIRI = objID;
                            Node exampleNode = nodes[0];
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
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (addExamples) {
                // List all the example-nodes
                exampleManager.listExampleNodes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * ============================================= Phase 2: Entity constraints extraction ============================
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     * =================================================================================================================
     */
    protected void entityConstraintsExtraction() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    //Declaring required sets
                    Set<Integer> objTypesIDs = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                    Node[] nodes = NxParser.parseNodes(line);
                    Node entityNode = nodes[0];
                    String objectType = extractObjectType(nodes[2].toString());
                    int propID = stringEncoder.encode(nodes[1].getLabel());
                    
                    // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                    if (objectType.equals("IRI")) {
                        objTypesIDs = parseIriTypeObject(objTypesIDs, prop2objTypeTuples, nodes, entityNode, propID);

                        if (addExamples) {
                            // For each class of the subject node, set the property type to NON-literal
                            Set<Integer> classIDs = entityDataHashMap.get(entityNode).getClassTypes();
                            classIDs.forEach(encodedClassIRI -> {
                                exampleManager.setPropertyType(encodedClassIRI, propID, stringEncoder.encode(objectType));
                                exampleManager.setPropertyIsLiteral(encodedClassIRI, propID, false);
                            });
                        }
                    }
                    // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                    else {
                        parseLiteralTypeObject(objTypesIDs, entityNode, objectType, propID);

                        if (addExamples) {
                            // For each class of the subject node, set the property type to literal
                            Set<Integer> classIDs = entityDataHashMap.get(entityNode).getClassTypes();
                            classIDs.forEach(encodedClassIRI -> {
                                exampleManager.setPropertyType(encodedClassIRI, propID, stringEncoder.encode(objectType));
                                exampleManager.setPropertyIsLiteral(encodedClassIRI, propID, true);
                            });
                        }
                    }
                    // for each type (class) of current entity -> append the property and object type in classToPropWithObjTypes HashMap
                    updateClassToPropWithObjTypesMap(objTypesIDs, entityNode, propID);


                    if (addExamples) {
                        String property = nodes[1].getLabel();
                        String actualValue = nodes[2].getLabel();
                        String rawValue = nodes[2].toString();
                        Integer encodedProperty = stringEncoder.encode(property);
                        Integer encodedActualValue = stringEncoder.encode(actualValue);
                        Integer encodedRawValue = stringEncoder.encode(rawValue);

                        // ADDING VALUES TO PROPERTIES OF EXAMPLE-NODES
                        // If the current node(subject) is an example-node, store his data
                        if (exampleManager.isExampleNode(entityNode)) {
                            exampleManager.addNodeData(entityNode, encodedProperty, encodedActualValue, encodedRawValue);
                        }

                        // ADDING EXAMPLE VALUES TO PROPERTIES WITH RESERVOIR SAMPLING
                        // For each class of the current node(subject), add the property value as an example value for that class
                        Set<Integer> classIDs = entityDataHashMap.get(entityNode).getClassTypes();
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
        Utils.logTime("secondPhase", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }


    /**
     * Iterating a third time on the RDF to store the labels
     * Given the triple (s,p,o) where s=IRI, p=rdf:label, o=label, store the label for the subject s
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
                        if (exampleManager.isClass(encodedSubject) || exampleManager.isProperty(encodedSubject) || exampleManager.isExampleNode(subject) ||exampleManager.isPropertyValue(encodedSubject,encodedRawSubject)){
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
    
    /**
     * ============================================= Phase 3: Support and Confidence Computation =======================
     * Computing support and confidence using the metadata extracted in the 2nd pass for shape constraints
     * =================================================================================================================
     */
    public void computeSupportConfidence() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        statsComputer = new StatsComputer();
        statsComputer.setShapeTripletSupport(shapeTripletSupport);
        statsComputer.computeSupportConfidence(entityDataHashMap, classEntityCount);
        watch.stop();
        Utils.logTime("computeSupportConfidence", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * ============================================= Phase 4: Shapes extraction ========================================
     * Extracting shapes in SHACL syntax using various values for support and confidence thresholds
     * =================================================================================================================
     */
    protected void extractSHACLShapes(Boolean performPruning, Boolean qseFromSpecificClasses) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        ShapesExtractor se = new ShapesExtractor(stringEncoder, shapeTripletSupport, classEntityCount, typePredicate);
        se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        
        //====================== Enable shapes extraction for specific classes ======================
        if (qseFromSpecificClasses)
            classToPropWithObjTypes = Utility.extractShapesForSpecificClasses(classToPropWithObjTypes, classEntityCount, stringEncoder);

        se.constructDefaultShapes(classToPropWithObjTypes, exampleManager); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            StopWatch watchForPruning = new StopWatch();
            watchForPruning.start();
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    StopWatch innerWatch = new StopWatch();
                    innerWatch.start();
                    se.constructPrunedShapes(classToPropWithObjTypes, exampleManager, conf, supp);
                    innerWatch.stop();
                    Utils.logTime(conf + "_" + supp + "", TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()), TimeUnit.MILLISECONDS.toMinutes(innerWatch.getTime()));
                });
            });
            methodName = "extractSHACLShapes";
            watchForPruning.stop();
            Utils.logTime(methodName + "-Time.For.Pruning.Only", TimeUnit.MILLISECONDS.toSeconds(watchForPruning.getTime()), TimeUnit.MILLISECONDS.toMinutes(watchForPruning.getTime()));
        }
        
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    
    //============================================= Utility Methods ====================================================
    
    private Set<Integer> parseIriTypeObject(Set<Integer> objTypesIDs, Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Node[] nodes, Node subject, int propID) {
        EntityData currEntityData = entityDataHashMap.get(nodes[2]);
        if (currEntityData != null && currEntityData.getClassTypes().size() != 0) {
            objTypesIDs = currEntityData.getClassTypes();
            for (Integer node : objTypesIDs) { // get classes of node2
                prop2objTypeTuples.add(new Tuple2<>(propID, node));
            }
            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
        }
        /*else { // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*/
        else {
            int objID = stringEncoder.encode(Constants.OBJECT_UNDEFINED_TYPE);
            objTypesIDs.add(objID);
            prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
        }
        return objTypesIDs;
    }
    
    private void parseLiteralTypeObject(Set<Integer> objTypes, Node subject, String objectType, int propID) {
        Set<Tuple2<Integer, Integer>> prop2objTypeTuples;
        int objID = stringEncoder.encode(objectType);
        //objTypes = Collections.singleton(objID); Removed because the set throws an UnsupportedOperationException if modification operation (add) is performed on it later in the loop
        objTypes.add(objID);
        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
        addEntityToPropertyConstraints(prop2objTypeTuples, subject);
    }
    
    private void updateClassToPropWithObjTypesMap(Set<Integer> objTypesIDs, Node entityNode, int propID) {
        EntityData entityData = entityDataHashMap.get(entityNode);
        if (entityData != null) {
            for (Integer entityTypeID : entityData.getClassTypes()) {
                Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.computeIfAbsent(entityTypeID, k -> new HashMap<>());
                Set<Integer> classObjTypes = propToObjTypes.computeIfAbsent(propID, k -> new HashSet<>());
                classObjTypes.addAll(objTypesIDs);
                propToObjTypes.put(propID, classObjTypes);
                classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
            }
        }
    }
    
    
    /**
     * A utility method to add property constraints of each entity in the 2nd phase
     *
     * @param prop2objTypeTuples : Tuples containing property and its object type, e.g., Tuple2<livesIn, :City>, Tuple2<livesIn, :Capital>
     * @param subject            : Subject entity such as :Paris
     */
    protected void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Node subject) {
        EntityData currentEntityData = entityDataHashMap.get(subject);
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
        entityDataHashMap.put(subject, currentEntityData);
    }
    
    //A utility method to extract the literal object type, returns String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
    protected String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI()) type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    /**
     * Assigning cardinality constraints  using various values for support and confidence thresholds
     */
    protected void assignCardinalityConstraints() {
        StopWatch watch = new StopWatch();
        watch.start();
        MinCardinalityExperiment minCardinalityExperiment = new MinCardinalityExperiment(stringEncoder, shapeTripletSupport, classEntityCount);
        minCardinalityExperiment.constructDefaultShapes(classToPropWithObjTypes);
        ExperimentsUtil.getMinCardinalitySupportConfRange().forEach((conf, supportRange) -> {
            supportRange.forEach(supp -> {
                minCardinalityExperiment.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
            });
        });
        watch.stop();
        Utils.logTime("assignCardinalityConstraints", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }


}