package cs.qse;

import cs.Main;
import cs.qse.experiments.ExperimentsUtil;
import cs.utils.*;
import cs.utils.encoders.Encoder;
import cs.utils.tries.Trie;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MemoryTest {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    int prefixCounter = 0;
    Map<String, Integer> prefix;
    
    //StatsComputer statsComputer;
    String typePredicate;
    Map<Pair<Integer, String>, EntityData> entityDataHashMap; // Size == N For every entity we save a number of summary information
    //Trie trie;
    Map<Integer, Integer> classEntityCount; // Size == T
    //Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    //Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    
    public MemoryTest(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        //this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.encoder = new Encoder();
        //this.trie = new Trie();
        this.prefix = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
    }
    
    public void run() {
        System.out.println("Memory Test");
        runParser();
    }
    
    private void runParser() {
        firstPass();
        //secondPass();
        //computeSupportConfidence();
        //extractSHACLShapes(false);
        System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        // Track classes per entity
                        int objID = encoder.encode(nodes[2].getLabel());
                        String entity = nodes[0].getLabel();
                        String prefixIRI = entity.substring(0, entity.lastIndexOf("/"));
                        int prefixIRIid;
                        if (prefix.get(prefixIRI) != null) {
                            prefixIRIid = prefix.get(prefixIRI);
                        } else {
                            prefix.put(prefixIRI, prefixCounter);
                            prefixIRIid = prefixCounter;
                            prefixCounter++;
                        }
                        
                        Pair<Integer, String> pair = new ImmutablePair<>(prefixIRIid, entity.substring(entity.lastIndexOf("/")));
                        
                        EntityData entityData = entityDataHashMap.get(pair);
                        if (entityData == null) {
                            entityData = new EntityData();
                        }
                        entityData.getClassTypes().add(objID);
                        entityDataHashMap.put(pair, entityData);
                        classEntityCount.merge(objID, 1, Integer::sum);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    /*
     *//**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     *//*
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
                    int subject = trie.getEncodedInteger(nodes[0].getLabel());
                    *//*if(subject == -99999){
                        System.out.println("WARNING:: STOP");
                    }*//*
                    String objectType = extractObjectType(nodes[2].toString());
                    int propID = encoder.encode(nodes[1].getLabel());
                    
                    if (objectType.equals("IRI")) { // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                        EntityData currEntityData = entityDataHashMap.get(trie.getEncodedInteger(nodes[2].getLabel()));
                        if (currEntityData != null) {
                            objTypes = currEntityData.getClassTypes();
                            for (Integer node : objTypes) { // get classes of node2
                                prop2objTypeTuples.add(new Tuple2<>(propID, node));
                            }
                            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                        }
                        *//*else { // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*//*
                        
                    } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                        int objID = encoder.encode(objectType);
                        //objTypes = Collections.singleton(objID); Removed because the set throws an UnsupportedOperationException if modification operation (add) is performed on it later in the loop
                        objTypes.add(objID);
                        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                        addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                    }
                    
                    EntityData entityData = entityDataHashMap.get(subject);
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
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    *//**
     * A utility method to add property constraints of each entity in the 2nd pass
     *
     * @param prop2objTypeTuples : Tuples containing property and its object type, e.g., Tuple2<livesIn, :City>, Tuple2<livesIn, :Capital>
     * @param subject            : Subject entity such as :Paris
     *//*
    protected void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Integer subject) {
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
    
    *//**
     * A utility method to extract the literal object type
     *
     * @param literalIri : IRI for the literal object
     * @return String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
     *//*
    protected String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    *//**
     * Computing support and confidence using the metadata extracted in the 2nd pass for shape constraints
     *//*
    public void computeSupportConfidence() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        statsComputer = new StatsComputer();
        statsComputer.setShapeTripletSupport(shapeTripletSupport);
        statsComputer.computeSupportConfidenceWithEncodedEntities(entityDataHashMap, classEntityCount);
        watch.stop();
        Utils.logTime("computeSupportConfidence", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    *//**
     * Extracting shapes in SHACL syntax using various values for support and confidence thresholds
     *//*
    protected void extractSHACLShapes(Boolean performPruning) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        ShapesExtractor se = new ShapesExtractor(encoder, shapeTripletSupport, classEntityCount);
        se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                });
            });
            methodName = "extractSHACLShapes";
        }
        
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }*/
}
