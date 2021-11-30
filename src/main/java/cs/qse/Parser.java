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
public class Parser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    NodeEncoder nodeEncoder;
    StatsComputer statsComputer;
    String typePredicate;
    
    
    HashMap<Integer, EntityData> encodedEntityDataHashMap;
    
    HashMap<Integer, Integer> classEntityCount;
    HashMap<Integer, Integer> classEntityCountForSampling;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes;
    
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    //Not used yet
    //HashMap<Node, BloomFilter<String>> cteBf;
    //HashMap<Node, EntityData> entityDataHashMap;
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
        
        this.encodedEntityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.classEntityCountForSampling = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        
        //this.cteBf = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        //this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        samplingBasedFirstPass(10000);
        secondPass();
        computeSupportConfidence();
        extractSHACLShapes();
    }
    
    /**
     * Currently this method performs the following tasks
     * 1. Perform reservoir sampling on typed entities for the given threshold for maximum number of entities
     * 2. Collect class to entity count
     *
     * @param threshold for number of max entities to sample for each class
     */
    private void samplingBasedFirstPass(int threshold) {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    Node[] nodes = NxParser.parseNodes(line);
                    //for typed triples
                    if (nodes[1].toString().equals(typePredicate)) {
                        int type = encoder.encode(nodes[2].getLabel());
                        classEntityCountForSampling.putIfAbsent(type, 0);
                        int entityCount = classEntityCountForSampling.get(type);
                        if (entityCount < threshold) {
                            //System.out.println("EntityCount: " + entityCount + " , threshold: " + threshold);
                            // Track classes per entity
                            if (encodedEntityDataHashMap.containsKey(nodeEncoder.encode(nodes[0]))) {
                                encodedEntityDataHashMap.get(nodeEncoder.encode(nodes[0])).getClassTypes().add(encoder.encode(nodes[2].getLabel()));
                            } else {
                                HashSet<Integer> hashSet = new HashSet<>();
                                hashSet.add(encoder.encode(nodes[2].getLabel()));
                                EntityData entityData = new EntityData();
                                entityData.getClassTypes().addAll(hashSet);
                                encodedEntityDataHashMap.put(nodeEncoder.encode(nodes[0]), entityData);
                            }
                            entityCount = entityCount + 1;
                            classEntityCountForSampling.put(type, entityCount);
                        }
                        Random random = new Random();
                        int randomIndex = random.nextInt(entityCount + 1);
                        
                        if (randomIndex < threshold && randomIndex < entityCount) {
                            //randomly pick a number , replace the key
                            //System.out.println("randomIndex: " + randomIndex + ", entityCount: " + entityCount + " , threshold: " + threshold);
                            encodedEntityDataHashMap.get(randomIndex).getClassTypes().add(encoder.encode(nodes[2].getLabel()));
                        }
                        
                        if (classEntityCount.containsKey(encoder.encode(nodes[2].getLabel()))) {
                            Integer val = classEntityCount.get(encoder.encode(nodes[2].getLabel()));
                            classEntityCount.put(encoder.encode(nodes[2].getLabel()), val + 1);
                        } else {
                            classEntityCount.put(encoder.encode(nodes[2].getLabel()), 1);
                        }
                    }
                    //for non-typed triples
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed samplingBasedFirstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     */
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).filter(line -> !line.contains(typePredicate)).forEach(line -> {
                try {
                    //Declaring required HashSets
                    HashSet<Integer> objTypes = new HashSet<>();
                    HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                    Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                    Node entity = nodes[0];
                    //only doing the work for sampled entities
                    if (encodedEntityDataHashMap.containsKey(nodeEncoder.encode(entity))) {
                        String objectType = extractObjectType(nodes[2].toString());
                        
                        if (objectType.equals("IRI")) { // // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                            if (encodedEntityDataHashMap.containsKey(nodeEncoder.encode(nodes[2]))) {
                                for (Integer node : encodedEntityDataHashMap.get(nodeEncoder.encode(nodes[2])).getClassTypes()) {
                                    objTypes.add(node);
                                    prop2objTypeTuples.add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), node));
                                }
                                addEntityToPropertyConstraints(prop2objTypeTuples, entity);
                            }
                        } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                            objTypes.add(encoder.encode(objectType));
                            prop2objTypeTuples = new HashSet<>() {{
                                add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), encoder.encode(objectType)));
                            }};
                            addEntityToPropertyConstraints(prop2objTypeTuples, entity);
                        }
                        // Keep track of each property of the node
                        if (encodedEntityDataHashMap.get(nodeEncoder.encode(entity)) != null) {
                            encodedEntityDataHashMap.get(nodeEncoder.encode(nodes[0])).getProperties().add(encoder.encode(nodes[1].getLabel()));
                        } else {
                            EntityData entityData = new EntityData();
                            entityData.getProperties().add(encoder.encode(nodes[1].getLabel()));
                            encodedEntityDataHashMap.put(nodeEncoder.encode(nodes[0]), entityData);
                        }
                        HashSet<Integer> entityClasses = encodedEntityDataHashMap.get(nodeEncoder.encode(nodes[0])).getClassTypes();
                        if (entityClasses != null) {
                            for (Integer entityClass : entityClasses) {
                                HashMap<Integer, HashSet<Integer>> propToObjTypes;
                                if (classToPropWithObjTypes.containsKey(entityClass)) {
                                    propToObjTypes = classToPropWithObjTypes.get(entityClass);
                                    int prop = encoder.encode(nodes[1].getLabel());
                                    if (propToObjTypes.containsKey(prop))
                                        propToObjTypes.get(prop).addAll(objTypes);
                                    else {
                                        propToObjTypes.put(prop, objTypes);
                                    }
                                } else {
                                    propToObjTypes = new HashMap<>();
                                    propToObjTypes.put(encoder.encode(nodes[1].getLabel()), objTypes);
                                }
                                classToPropWithObjTypes.put(entityClass, propToObjTypes);
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
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * A utility method to add property constraints of each entity in the 2nd pass
     *
     * @param prop2objTypeTuples : Tuples containing property and its object type, e.g., Tuple2<livesIn, :City>, Tuple2<livesIn, :Capital>
     * @param entity             : Entity such as :Paris
     */
    private void addEntityToPropertyConstraints(HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples, Node entity) {
        if (encodedEntityDataHashMap.containsKey(nodeEncoder.encode(entity))) {
            encodedEntityDataHashMap.get(nodeEncoder.encode(entity)).getPropertyConstraints().addAll(prop2objTypeTuples);
        } else {
            EntityData entityData = new EntityData();
            entityData.getPropertyConstraints().addAll(prop2objTypeTuples);
            encodedEntityDataHashMap.put(nodeEncoder.encode(entity), entityData);
        }
    }
    
    /**
     * A utility method to extract the literal object type
     *
     * @param literalIri : IRI for the literal object
     * @return String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
     */
    private String extractObjectType(String literalIri) {
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
    
    /**
     * Computing support and confidence using the metadata extracted in the 2nd pass for shape constraints
     */
    public void computeSupportConfidence() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        statsComputer = new StatsComputer(shapeTripletSupport);
        //statsComputer.compute(entityDataHashMap, classEntityCount);
        statsComputer.computeEncoded(encodedEntityDataHashMap, classEntityCount); //new method created to use encoded version of the map where entities are encoded
        shapeTripletSupport = statsComputer.getShapeTripletSupport();
        watch.stop();
        System.out.println("Time Elapsed computeSupportConfidence: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * Extracting shapes in SHACL syntax using various values for support and confidence thresholds
     */
    private void extractSHACLShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        ShapesExtractor shapesExtractor = new ShapesExtractor(encoder, shapeTripletSupport, classEntityCount);
        shapesExtractor.setMaxCountSupport(statsComputer.propToClassesHavingMaxCountGreaterThanOne);
        shapesExtractor.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
            supportRange.forEach(supp -> {
                shapesExtractor.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
            });
        });
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * Assigning cardinality constraints  using various values for support and confidence thresholds
     */
    private void assignCardinalityConstraints() {
        StopWatch watch = new StopWatch();
        watch.start();
        MinCardinalityExperiment minCardinalityExperiment = new MinCardinalityExperiment(encoder, shapeTripletSupport, classEntityCount);
        minCardinalityExperiment.constructDefaultShapes(classToPropWithObjTypes);
        ExperimentsUtil.getMinCardinalitySupportConfRange().forEach((conf, supportRange) -> {
            supportRange.forEach(supp -> {
                minCardinalityExperiment.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
            });
        });
        watch.stop();
        System.out.println("Time Elapsed minCardinalityExperiment: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}