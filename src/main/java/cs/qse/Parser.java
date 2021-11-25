package cs.qse;

import cs.qse.experiments.ExperimentsUtil;
import cs.qse.experiments.MinCardinalityExperiment;
import cs.utils.Constants;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.encoders.Encoder;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;
    
    HashMap<Node, EntityData> entityDataHashMap;
    HashMap<String, EntityData> entityDataHashMapLite;
    HashMap<Integer, Integer> classEntityCount;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    
    HashMap<Node, BloomFilter<String>> cteBf;
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        //this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
        this.entityDataHashMapLite = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        
        this.cteBf = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        collectClassEntityCount();
        collectEntitiesInBloomFilter();
        //iterateOverBloomFilters();
        //collectClassEntityCount();
        //collectEntities();
        //secondPass();
        //computeSupportConfidence();
        //extractSHACLShapes();
        //assignCardinalityConstraints();
        //System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    
    private void shortEntityStrings() {
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
    }
    
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
    
    private void collectEntitiesInBloomFilter() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("invoked collectEntitiesInBloomFilter() ");
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                if (cteBf.containsKey(nodes[2])) {
                                    cteBf.get(nodes[2]).add(nodes[0].getLabel());
                                } else {
                                    BloomFilter<String> bf = new FilterBuilder(classEntityCount.get(encoder.encode(nodes[2].getLabel())), 0.000001).buildBloomFilter();
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
        System.out.println("Time Elapsed collectEntitiesInBloomFilter: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void iterateOverBloomFilters() {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("invoked iterateOverBloomFilters() ");
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            List<String> types = new ArrayList<>();
                            StopWatch innerWatch = new StopWatch();
                            innerWatch.start();
                            for (Map.Entry<Node, BloomFilter<String>> entry : cteBf.entrySet()) {
                                BloomFilter<String> v = entry.getValue();
                                if (v.contains(nodes[0].getLabel())) {
                                    types.add(entry.getKey().getLabel());
                                }
                            }
                            System.out.println("Types of given entity: " + types.size());
                            innerWatch.stop();
                            System.out.println("Time Elapsed iterateOverBloomFilters: MilliSeconds:" + innerWatch.getTime() + " , Seconds: " + TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed iterateOverBloomFilters: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    
    private void collectEntities() {
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
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     */
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .filter(line -> !line.contains(typePredicate))
                    .forEach(line -> {
                        try {
                            //Declaring required HashSets
                            HashSet<Integer> objTypes = new HashSet<>();
                            HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                            
                            Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                            Node entity = nodes[0];
                            String objectType = extractObjectType(nodes[2].toString());
                            
                            if (objectType.equals("IRI")) { // // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                                if (entityDataHashMap.containsKey(nodes[2])) {
                                    for (Integer node : entityDataHashMap.get(nodes[2]).getClassTypes()) {
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
                            if (entityDataHashMap.get(entity) != null) {
                                entityDataHashMap.get(nodes[0]).getProperties().add(encoder.encode(nodes[1].getLabel()));
                            } else {
                                EntityData entityData = new EntityData();
                                entityData.getProperties().add(encoder.encode(nodes[1].getLabel()));
                                entityDataHashMap.put(nodes[0], entityData);
                            }
                            
                            HashSet<Integer> entityClasses = entityDataHashMap.get(nodes[0]).getClassTypes();
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
        if (entityDataHashMap.containsKey(entity)) {
            entityDataHashMap.get(entity).getPropertyConstraints().addAll(prop2objTypeTuples);
        } else {
            EntityData entityData = new EntityData();
            entityData.getPropertyConstraints().addAll(prop2objTypeTuples);
            entityDataHashMap.put(entity, entityData);
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
        statsComputer.compute(entityDataHashMap, classEntityCount);
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