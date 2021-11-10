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
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * This class parses RDF NT files to extract SHACL shapes and compute the confidence/support for shape constraints
 */
public class Parser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Encoder encoder;
    String typePredicate;
    
    HashMap<Node, HashSet<Integer>> entityToClassTypes;
    HashMap<Node, HashSet<Tuple2<Integer, Integer>>> entityToPropertyConstraints;
    
    HashMap<Integer, Integer> classEntityCount;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityToClassTypes = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
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
                            Node[] nodes = NxParser.parseNodes(line);
                            if (nodes[1].toString().equals(typePredicate)) {
                                // Track classes per entity
                                if (entityToClassTypes.containsKey(nodes[0])) {
                                    entityToClassTypes.get(nodes[0]).add(encoder.encode(nodes[2].getLabel()));
                                } else {
                                    HashSet<Integer> list = new HashSet<>(); // initialize 5, 10, 15
                                    list.add(encoder.encode(nodes[2].getLabel()));
                                    entityToClassTypes.put(nodes[0], list);
                                }
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
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     * to compute the support and confidence of each candidate shape.
     */
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        this.entityToPropertyConstraints = new HashMap<>((int) ((classEntityCount.size() / 0.75 + 1)));
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
                                if (entityToClassTypes.containsKey(nodes[2])) {
                                    for (Integer node : entityToClassTypes.get(nodes[2])) {
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
                            HashSet<Integer> entityClasses = entityToClassTypes.get(nodes[0]);
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
    
    private void addEntityToPropertyConstraints(HashSet<Tuple2<Integer, Integer>> prop2objTypeTuples, Node entity) {
        entityToPropertyConstraints.putIfAbsent(entity, prop2objTypeTuples);
        HashSet<Tuple2<Integer, Integer>> prop2objTypeTupleSet = entityToPropertyConstraints.get(entity);
        prop2objTypeTupleSet.addAll(prop2objTypeTuples);
        entityToPropertyConstraints.put(entity, prop2objTypeTupleSet);
    }
    
    private String extractObjectType(String node) {
        Literal theLiteral = new Literal(node, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(node)) {
                if (SimpleValueFactory.getInstance().createIRI(node).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    public void computeShapeStatistics() {
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        ComputeStatistics cs = new ComputeStatistics(shapeTripletSupport);
        cs.compute(this.entityToPropertyConstraints, entityToClassTypes, classEntityCount);
        this.shapeTripletSupport = cs.getShapeTripletSupport();
        System.out.println("Done");
    }
    
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        ShapesExtractor shapesExtractor = new ShapesExtractor(encoder, shapeTripletSupport, classEntityCount);
        shapesExtractor.constructDefaultShapes(classToPropWithObjTypes);
        ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
            supportRange.forEach(supp -> {
                shapesExtractor.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
            });
        });
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void minCardinalityExperiment() {
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
    
    private void runParser() {
        firstPass();
        secondPass();
        computeShapeStatistics();
        populateShapes();
        minCardinalityExperiment();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    
    public void run() {
        runParser();
    }
}