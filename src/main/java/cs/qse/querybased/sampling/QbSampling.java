package cs.qse.querybased.sampling;

import com.google.common.collect.Lists;
import cs.Main;
import cs.qse.common.EntityData;
import cs.qse.filebased.ShapesExtractorNativeStore;
import cs.qse.filebased.SupportConfidence;
import cs.qse.common.ExperimentsUtil;
import cs.qse.filebased.sampling.DynamicNeighborBasedReservoirSampling;
import cs.utils.*;
import cs.qse.common.encoders.StringEncoder;
import cs.qse.common.encoders.NodeEncoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.query.BindingSet;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static cs.qse.common.Utility.*;

/**
 * * Qb (query-based) Sampling Parser
 */
public class QbSampling {
    private final GraphDBUtils graphDBUtils;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    StringEncoder stringEncoder;
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
    
    /**
     * ============================================= Constructor ========================================
     */
    public QbSampling(int expNoOfClasses, int expNoOfInstances, String typePredicate, Integer entitySamplingThreshold) {
        this.graphDBUtils = new GraphDBUtils();
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
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    /**
     * ============================================= Run Query-based Parser ============================================
     */
    public void run() {
        System.out.println("Started EndpointSampling ...");
        getNumberOfInstancesOfEachClass();
        dynamicNeighborBasedReservoirSampling(); //first pass here is to send a query to the endpoint and get all entities, parse the entities and sample using reservoir sampling
        entityConstraintsExtractionBatch();
        //entityConstraintsExtraction(); //In the 2nd pass you run query for each sampled entity to get the property metadata ...
        writeSupportToFile(stringEncoder, this.shapeTripletSupport, this.sampledEntitiesPerClass);
        extractSHACLShapes(false);
    }
    
    /**
     * ====================================  Compute number of instances of each class by querying (Pre-phase 1) =================
     */
    private void getNumberOfInstancesOfEachClass() {
        StopWatch watch = new StopWatch();
        watch.start();
        String query = FilesUtil.readQuery("query2").replace(":instantiationProperty", typePredicate);
        //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
        graphDBUtils.runSelectQuery(query).forEach(result -> {
            String c = result.getValue("class").stringValue();
            int classCount = 0;
            if (result.getBinding("classCount").getValue().isLiteral()) {
                org.eclipse.rdf4j.model.Literal literalClassCount = (org.eclipse.rdf4j.model.Literal) result.getBinding("classCount").getValue();
                classCount = literalClassCount.intValue();
            }
            classEntityCount.put(stringEncoder.encode(c), classCount);
        });
        watch.stop();
        System.out.println("Time Elapsed getNumberOfInstancesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getNumberOfInstancesOfEachClass ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * ============================ Phase 1: Entity Extraction using Dynamic Reservoir Sampling ========================
     * Streaming over query results <s,a,o> line by line to extract set of entity types
     * =================================================================================================================
     */
    private void dynamicNeighborBasedReservoirSampling() {
        System.out.println("invoked:dynamicNeighborBasedReservoirSampling()");
        StopWatch watch = new StopWatch();
        watch.start();
        String queryToGetWikiDataEntities = "PREFIX onto: <http://www.ontotext.com/>  CONSTRUCT from onto:explicit WHERE { ?s " + typePredicate + " ?o .} ";
        
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        this.reservoirCapacityPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int minEntityThreshold = 1;
        int samplingPercentage = Main.entitySamplingTargetPercentage;
        DynamicNeighborBasedReservoirSampling drs = new DynamicNeighborBasedReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, reservoirCapacityPerClass, nodeEncoder, stringEncoder);
        try {
            graphDBUtils.runConstructQuery(queryToGetWikiDataEntities).forEach(line -> {
                try {
                    String triple = "<" + line.getSubject() + "> <" + line.getPredicate() + "> <" + line.getObject() + "> ."; // prepare triple in N3 format to avoid changing many methods using nodes of type Node
                    Node[] nodes = NxParser.parseNodes(triple); // Get [S,P,O] as Node from triple
                    
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
                    
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass:dynamicNeighborBasedReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logSamplingStats("dynamicNeighborBasedReservoirSampling", samplingPercentage, minEntityThreshold, maxEntityThreshold, entityDataMapContainer.size());
    }
    
    
    /**
     * FIXME =================== Phase 2: BATCH Entity constraints extraction + Phase 3: Confidence & Support Computation ==========
     * Querying to get properties and object types of each entity to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     * =================================================================================================================
     */
    
    
    private void entityConstraintsExtractionBatch() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started secondPhaseBatch()");
        
        HashMap<Set<String>, List<String>> typesToEntities = new HashMap<>();
        
        for (Map.Entry<Integer, EntityData> entry : entityDataMapContainer.entrySet()) {
            Integer entityID = entry.getKey();
            EntityData entityData = entry.getValue();
            Set<String> entityTypes = new HashSet<>();
            
            for (Integer entityTypeID : entityData.getClassTypes()) {
                entityTypes.add(stringEncoder.decode(entityTypeID));
            }
            typesToEntities.computeIfAbsent(entityTypes, k -> new ArrayList<>());
            
            List<String> current = typesToEntities.get(entityTypes);
            current.add(nodeEncoder.decode(entityID).toString());
            typesToEntities.put(entityTypes, current);
        }
        
        typesToEntities.forEach((types, entities) -> {
            if (entities.size() > 100) {
                List<List<String>> subEntities = Lists.partition(entities, 100);
                subEntities.forEach(listOfSubEntities -> {
                    String batchQuery = buildBatchQuery(types, listOfSubEntities, typePredicate); // ?p ?o are binding variables
                    iterateOverEntityTriples(types, batchQuery);
                });
            } else {
                String batchQuery = buildBatchQuery(types, entities, typePredicate); // ?p ?o are binding variables
                iterateOverEntityTriples(types, batchQuery);
            }
        });
        watch.stop();
        Utils.logTime("secondPhaseBatch:cs.qse.endpoint.EndpointSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void iterateOverEntityTriples(Set<String> types, String batchQuery) {
        for (BindingSet row : graphDBUtils.evaluateSelectQuery(batchQuery)) {
            String entity = row.getValue("entity").stringValue();
            Integer entityID = nodeEncoder.encode(Utils.IriToNode(entity));
            String prop = row.getValue("p").stringValue();
            //String propIri = "<" + prop + ">";
            
            int propID = stringEncoder.encode(prop);
            
            String obj = row.getValue("o").stringValue();
            Node objNode = Utils.IriToNode(obj);
            String objType = extractObjectType(obj); // find out if the object is literal or IRI
            
            Set<Integer> objTypesIDs = new HashSet<>(10);
            Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
            
            // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
            if (objType.equals("IRI")) {
                objTypesIDs = parseIriTypeObject(entityID, propID, objNode, objTypesIDs, prop2objTypeTuples);
            }
            // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
            else {
                parseLiteralTypeObject(entityID, propID, objType, objTypesIDs);
            }
            // for each type (class) of current entity -> append the property and object type in classToPropWithObjTypes HashMap
            // Also update shape triplet support map by computing support of triplets
            //updateClassToPropWithObjTypesAndShapeTripletSupportMaps(entityData, propID, objTypesIDs);
            
            for (String entityTypeString : types) {
                Integer entityTypeID = stringEncoder.encode(entityTypeString);
                Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.computeIfAbsent(entityTypeID, k -> new HashMap<>());
                Set<Integer> classObjTypes = propToObjTypes.computeIfAbsent(propID, k -> new HashSet<>());
                
                classObjTypes.addAll(objTypesIDs);
                propToObjTypes.put(propID, classObjTypes);
                classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
                
                //Compute Support - <entityTypeID, propID, objTypesIDs
                objTypesIDs.forEach(objTypeID -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<Integer, Integer, Integer>(entityTypeID, propID, objTypeID);
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        Integer support = shapeTripletSupport.get(tuple3).getSupport();
                        support++;
                        shapeTripletSupport.put(tuple3, new SupportConfidence(support));
                    } else {
                        shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                    }
                });
            }
        }
    }
    
    
    /**
     * =================== Phase 2: Entity constraints extraction + Phase 3: Confidence & Support Computation ==========
     * Querying to get properties and object types of each entity to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     * =================================================================================================================
     */
    private void entityConstraintsExtraction() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started secondPhase()");
        try {
            for (Map.Entry<Integer, EntityData> entry : entityDataMapContainer.entrySet()) {
                Integer entityID = entry.getKey();
                EntityData entityData = entry.getValue();
                Set<String> entityTypes = new HashSet<>();
                for (Integer entityTypeID : entityData.getClassTypes()) {
                    entityTypes.add(stringEncoder.decode(entityTypeID));
                }
                String entity = nodeEncoder.decode(entityID).getLabel();
                String query = buildQuery(entity, entityTypes, typePredicate); // query to get ?p ?o of entity
                
                // ?p ?o are binding variables
                for (BindingSet row : graphDBUtils.evaluateSelectQuery(query)) {
                    String prop = row.getValue("p").stringValue();
                    //String propIri = "<" + prop + ">";
                    
                    int propID = stringEncoder.encode(prop);
                    
                    String obj = row.getValue("o").stringValue();
                    Node objNode = Utils.IriToNode(obj);
                    String objType = extractObjectType(obj); // find out if the object is literal or IRI
                    
                    Set<Integer> objTypesIDs = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                    if (objType.equals("IRI")) {
                        objTypesIDs = parseIriTypeObject(entityID, propID, objNode, objTypesIDs, prop2objTypeTuples);
                    }
                    // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                    else {
                        parseLiteralTypeObject(entityID, propID, objType, objTypesIDs);
                    }
                    // for each type (class) of current entity -> append the property and object type in classToPropWithObjTypes HashMap
                    // Also update shape triplet support map by computing support of triplets
                    updateClassToPropWithObjTypesAndShapeTripletSupportMaps(entityData, propID, objTypesIDs);
                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPhase:cs.qse.endpoint.EndpointSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * ============================================= Phase 4: Shapes extraction ========================================
     * Extracting shapes in SHACL syntax using various values for support and confidence thresholds
     * =================================================================================================================
     */
    protected void extractSHACLShapes(Boolean performPruning) {
        System.out.println("Started extractSHACLShapes()");
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        ShapesExtractorNativeStore se = new ShapesExtractorNativeStore(stringEncoder, shapeTripletSupport, classEntityCount, typePredicate);
        //se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            StopWatch watchForPruning = new StopWatch();
            watchForPruning.start();
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    StopWatch innerWatch = new StopWatch();
                    innerWatch.start();
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
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
    
    private Set<Integer> parseIriTypeObject(Integer entityID, int propID, Node objNode, Set<Integer> objTypesIDs, Set<Tuple2<Integer, Integer>> prop2objTypeTuples) {
        EntityData currEntityData = entityDataMapContainer.get(nodeEncoder.encode(objNode));
        if (currEntityData != null && currEntityData.getClassTypes().size() != 0) {
            objTypesIDs = currEntityData.getClassTypes();
            for (Integer objTypeID : objTypesIDs) { // get classes of node2
                prop2objTypeTuples.add(new Tuple2<>(propID, objTypeID));
            }
            addEntityToPropertyConstraints(prop2objTypeTuples, entityID);
        } else {
            int objID = stringEncoder.encode(Constants.OBJECT_UNDEFINED_TYPE);
            objTypesIDs.add(objID);
            prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
            addEntityToPropertyConstraints(prop2objTypeTuples, entityID);
        }
        /*else { bjTypesIDs.add(-1); // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*/
        return objTypesIDs;
    }
    
    private void parseLiteralTypeObject(Integer entityID, int propID, String objType, Set<Integer> objTypesIDs) {
        Set<Tuple2<Integer, Integer>> prop2objTypeTuples;
        int objID = stringEncoder.encode(objType);
        objTypesIDs.add(objID);
        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
        addEntityToPropertyConstraints(prop2objTypeTuples, entityID);
    }
    
    private void updateClassToPropWithObjTypesAndShapeTripletSupportMaps(EntityData entityData, int propID, Set<Integer> objTypesIDs) {
        for (Integer entityTypeID : entityData.getClassTypes()) {
            Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.computeIfAbsent(entityTypeID, k -> new HashMap<>());
            Set<Integer> classObjTypes = propToObjTypes.computeIfAbsent(propID, k -> new HashSet<>());
            
            classObjTypes.addAll(objTypesIDs);
            propToObjTypes.put(propID, classObjTypes);
            classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
            
            //Compute Support - <entityTypeID, propID, objTypesIDs
            objTypesIDs.forEach(objTypeID -> {
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<Integer, Integer, Integer>(entityTypeID, propID, objTypeID);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    Integer support = shapeTripletSupport.get(tuple3).getSupport();
                    support++;
                    shapeTripletSupport.put(tuple3, new SupportConfidence(support));
                } else {
                    shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                }
            });
        }
    }
    
    //A utility method to add property constraints of each entity in the 2nd phase
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
    
}