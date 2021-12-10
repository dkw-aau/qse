package cs.qse;

import cs.qse.experiments.ExperimentsUtil;
import cs.qse.experiments.MinCardinalityExperiment;
import cs.utils.Constants;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.encoders.Encoder;
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
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;

    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates

    Map<Node, EntityData> entityDataHashMap; // Size == N For every entity we save a number of summary information -- > for every node we store 1 integer for every node edge --> entityDataHashMap stores the entire graph in memory!
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport; // Size O(T*P*T)
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        firstPass();
        secondPass();
        computeSupportConfidence();
        extractSHACLShapes();
        assignCardinalityConstraints();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to extract set of entity types and frequency of each entity.
     */
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath))
                    .forEach(line -> {
                        try {
                            // Get [S,P,O] as Node from triple
                            Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                            if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                                // Track classes per entity

                                int objID = encoder.encode(nodes[2].getLabel());

                                if (entityDataHashMap.containsKey(nodes[0])) { // we check if we have seen the subject earlier
                                    entityDataHashMap.get(nodes[0]).getClassTypes().add(objID); // add the type to the list of types of this node
                                } else {
                                    HashSet<Integer> hashSet = new HashSet<>();
                                    hashSet.add(objID);
                                    EntityData entityData = new EntityData();
                                    entityData.getClassTypes().addAll(hashSet);
                                    // if entityDataHashMap.contains(nodes[0]) || random <0.1
                                    entityDataHashMap.put(nodes[0], entityData);
                                }

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
                            Set<Integer> objTypes ;
                            Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>();
                            
                            Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                            Node subject = nodes[0];
                            String objectType = extractObjectType(nodes[2].toString());
                            
                            if (objectType.equals("IRI")) { // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                                if (entityDataHashMap.containsKey(nodes[2])) {
                                    objTypes = entityDataHashMap.get(nodes[2]).getClassTypes();
                                    for (Integer node : objTypes ) { // get classes of node2
                                        prop2objTypeTuples.add(new Tuple2<>(encoder.encode(nodes[1].getLabel()), node));
                                    }
                                    addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                                } else {
                                    // If we do not have data this is an unlabelled IRI
                                    objTypes = Collections.emptySet();
                                }
                            } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                                int objID =  encoder.encode(objectType);
                                objTypes = Collections.singleton(objID);
                                prop2objTypeTuples = Collections.singleton(new Tuple2<>(encoder.encode(nodes[1].getLabel()), objID));
                                addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                            }
                            // Keep track of each property of the node

                            EntityData entityData=  entityDataHashMap.get(subject);
                            if ( entityData == null) {
                                entityData = new EntityData();
                                entityDataHashMap.put(subject, entityData);
                            }
                            entityData.getProperties().add(encoder.encode(nodes[1].getLabel()));
                            
                            Set<Integer> entityClasses = entityData.getClassTypes();
                            if (entityClasses != null) {
                                for (Integer entityClass : entityClasses) {
                                    Map<Integer, Set<Integer>> propToObjTypes;//todo optimize
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
    private void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Node entity) {
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