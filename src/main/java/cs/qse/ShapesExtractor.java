package cs.qse;

import cs.Main;
import cs.qse.experiments.ExperimentsUtil;
import cs.utils.*;
import cs.utils.encoders.Encoder;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.eclipse.rdf4j.model.util.Values.bnode;

/**
 * This class is used to extract/construct shapes (default and pruned) using all the information/metadata collected in Parser
 */
public class ShapesExtractor {
    Model model = null;
    ModelBuilder builder;
    Encoder encoder;
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport;
    Map<Integer, Integer> classInstanceCount;
    Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne;
    ValueFactory factory = SimpleValueFactory.getInstance();
    String logfileAddress = Constants.EXPERIMENTS_RESULT;
    IRI confidenceIRI = Values.iri(Constants.SHACL_CONFIDENCE);
    IRI supportIRI = Values.iri(Constants.SHACL_SUPPORT);
    IRI reificationIRI;
    
    public ShapesExtractor(Encoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, Integer> classInstanceCount) {
        this.encoder = encoder;
        this.builder = new ModelBuilder();
        this.shapeTripletSupport = shapeTripletSupport;
        this.classInstanceCount = classInstanceCount;
        builder.setNamespace("shape", Constants.SHAPES_NAMESPACE);
    }
    
    
    //Shapes :: NO PRUNING
    
    public void constructDefaultShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.addAll(constructShapesWithoutPruning(classToPropWithObjTypes));
        System.out.println("MODEL:: DEFAULT - SIZE: " + this.model.size());
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        //System.out.println(currentShapesModelStats);
        StringBuilder header = new StringBuilder("DATASET,Confidence,Support,");
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + 1.0 + "%, > " + 1.0 + ",");
        for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
            String v = entry.getValue();
            log = new StringBuilder(log.append(v) + ",");
            header = new StringBuilder(header.append(entry.getKey()) + ",");
        }
        FilesUtil.writeToFileInAppendMode(header.toString(), logfileAddress);
        FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
        this.writeModelToFile("DEFAULT");
    }
    
    public void constructDefaultShapesWithSupportConfidence(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.addAll(constructShapesWithoutPruningWithSupportConfidence(classToPropWithObjTypes));
        System.out.println("MODEL:: DEFAULT - SIZE: " + this.model.size());
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        //System.out.println(currentShapesModelStats);
        StringBuilder header = new StringBuilder("DATASET,Confidence,Support,");
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + 1.0 + "%, > " + 1.0 + ",");
        for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
            String v = entry.getValue();
            log = new StringBuilder(log.append(v) + ",");
            header = new StringBuilder(header.append(entry.getKey()) + ",");
        }
        FilesUtil.writeToFileInAppendMode(header.toString(), logfileAddress);
        FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
        //this.writeModelToFileInRdfStar("RDF_STAR_SUPP_CONF");
        //this.writeModelToFile("REIFIED_SUPP_CONF");
        this.writeModelToFileWithPrettyFormatting("LATEST");
    }
    
    private Model constructShapesWithoutPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        classToPropWithObjTypes.forEach((encodedClassIRI, propToObjectType) -> {
            if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
                IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
                String nodeShape = "shape:" + subj.getLocalName() + "Shape";
                b.subject(nodeShape)
                        .add(RDF.TYPE, SHACL.NODE_SHAPE)
                        .add(SHACL.TARGET_CLASS, subj)
                        .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                        .add(SHACL.CLOSED, false);
                
                if (propToObjectType != null) {
                    constructNodePropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectType);
                }
            } else {
                System.out.println("constructShapeWithoutPruning::INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
            }
        });
        m = b.build();
        return m;
    }
    
    /**
     * Adding Support in the Node Shapes in the form of triples
     */
    private Model constructShapesWithoutPruningWithSupportConfidence(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        classToPropWithObjTypes.forEach((encodedClassIRI, propToObjectType) -> {
            if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
                IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
                String nodeShape = "shape:" + subj.getLocalName() + "Shape";
                b.subject(nodeShape)
                        .add(RDF.TYPE, SHACL.NODE_SHAPE)
                        .add(SHACL.TARGET_CLASS, subj)
                        .add(VOID.ENTITIES, classInstanceCount.get(encodedClassIRI))
                        //.add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                        .add(SHACL.CLOSED, false);
                
                if (propToObjectType != null) {
                    constructNodePropertyShapesWithSupportConfidence(b, subj, encodedClassIRI, nodeShape, propToObjectType);
                }
            } else {
                System.out.println("constructShapeWithoutPruning::INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
            }
        });
        m = b.build();
        return m;
    }
    
    private void constructNodePropertyShapes(ModelBuilder b, IRI subj, Integer subjEncoded, String nodeShape, Map<Integer, Set<Integer>> propToObjectTypesLocal) {
        propToObjectTypesLocal.forEach((prop, propObjectTypes) -> {
            IRI property = factory.createIRI(encoder.decode(prop));
            IRI propShape = factory.createIRI("sh:" + property.getLocalName() + subj.getLocalName() + "ShapeProperty");
            b.subject(nodeShape)
                    .add(SHACL.PROPERTY, propShape);
            b.subject(propShape)
                    .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                    .add(SHACL.PATH, property);
            
            
            propObjectTypes.forEach(encodedObjectType -> {
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                        b.subject(propShape).add(SHACL.MIN_COUNT, 1);
                    }
                    if (Main.extractMaxCardConstraints) {
                        if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                            b.subject(propShape).add(SHACL.MAX_COUNT, 1);
                        }
                    }
                }
                String objectType = encoder.decode(encodedObjectType);
                if (objectType != null) {
                    if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                        if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                        IRI objectTypeIri = factory.createIRI(objectType);
                        b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                        b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                    } else {
                        //objectType = objectType.replace("<", "").replace(">", "");
                        if (Utils.isValidIRI(objectType)) {
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                        } else {
                            //IRI objectTypeIri = factory.createIRI(objectType);
                            //b.subject(propShape).add(SHACL.CLASS, objectType);
                            System.out.println("INVALID Object Type IRI: " + objectType);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                        }
                    }
                } else {
                    // in case the type is null, we set it default as string
                    b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                }
            });
        });
    }
    
    /**
     * Adding Support and Confidence in the Property Shapes in the form of triples
     */
    private void constructNodePropertyShapesWithSupportConfidence(ModelBuilder b, IRI subj, Integer subjEncoded, String nodeShape, Map<Integer, Set<Integer>> propToObjectTypesLocal) {
        
        propToObjectTypesLocal.forEach((prop, propObjectTypes) -> {
            IRI property = factory.createIRI(encoder.decode(prop));
            
            IRI propShape = factory.createIRI("sh:" + property.getLocalName() + subj.getLocalName() + "ShapeProperty");
            reificationIRI = factory.createIRI(Constants.SHAPES_NAMESPACE + subj.getLocalName() + "/" + property.getLocalName());
            b.subject(nodeShape).add(SHACL.PROPERTY, propShape);
            b.subject(propShape).add(RDF.TYPE, SHACL.PROPERTY_SHAPE).add(SHACL.PATH, property);
            
            int numberOfObjectTypes = propObjectTypes.size();
            
            if (numberOfObjectTypes == 1) {
                int encodedObjectType = propObjectTypes.iterator().next();
                String objectType = encoder.decode(encodedObjectType);
                
                //Adding Cardinality Constraints
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                        b.subject(propShape).add(SHACL.MIN_COUNT, 1);
                    }
                    if (Main.extractMaxCardConstraints) {
                        if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                            b.subject(propShape).add(SHACL.MAX_COUNT, 1);
                        }
                    }
                }
                // Adding other constraints
                if (objectType != null) {
                    if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                        if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                        IRI objectTypeIri = factory.createIRI(objectType);
                        b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                        b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                        
                    } else {
                        //objectType = objectType.replace("<", "").replace(">", "");
                        if (Utils.isValidIRI(objectType)) {
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                            
                        } else {
                            //IRI objectTypeIri = factory.createIRI(objectType);
                            //b.subject(propShape).add(SHACL.CLASS, objectType);
                            System.out.println("INVALID Object Type IRI: " + objectType);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                        }
                    }
                } else {
                    // in case the type is null, we set it default as string
                    b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                }
            }
            
            if (numberOfObjectTypes > 1) {
                List<Resource> members = new ArrayList<>();
                Resource headMember = bnode();
                ModelBuilder localBuilder = new ModelBuilder();
                
                for (Integer encodedObjectType : propObjectTypes) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    String objectType = encoder.decode(encodedObjectType);
                    Resource currentMember = bnode();
                    //Cardinality Constraints
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, 1);
                        }
                        if (Main.extractMaxCardConstraints) {
                            if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                b.subject(propShape).add(SHACL.MAX_COUNT, 1);
                            }
                        }
                    }
                    
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                            IRI objectTypeIri = factory.createIRI(objectType);
                            
                            localBuilder.subject(currentMember).add(SHACL.DATATYPE, objectTypeIri);
                            localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            
                            if (shapeTripletSupport.containsKey(tuple3)) {
                                Literal entities = Values.literal(shapeTripletSupport.get(tuple3).getSupport()); // support value
                                localBuilder.subject(currentMember).add(VOID.ENTITIES, entities);
                            }
                            
                        } else {
                            if (Utils.isValidIRI(objectType)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                localBuilder.subject(currentMember).add(SHACL.CLASS, objectTypeIri);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                if (shapeTripletSupport.containsKey(tuple3)) {
                                    Literal entities = Values.literal(shapeTripletSupport.get(tuple3).getSupport()); // support value
                                    localBuilder.subject(currentMember).add(VOID.ENTITIES, entities);
                                }
                            } else {
                                System.out.println("INVALID Object Type IRI: " + objectType);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                            }
                        }
                    } else {
                        // in case the type is null, we set it default as string
                        //b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                        localBuilder.subject(currentMember).add(SHACL.DATATYPE, XSD.STRING);
                    }
                    members.add(currentMember);
                }
                Model localModel = RDFCollections.asRDF(members, headMember, new LinkedHashModel());
                localModel.add(propShape, SHACL.OR, headMember);
                localModel.addAll(localBuilder.build());
                b.build().addAll(localModel);
            }
        });
    }
    
    
    //Shapes :: WITH PRUNING
    
    public void constructPrunedShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.addAll(constructShapesWithPruning(classToPropWithObjTypes, confidence, support));
        System.out.println("MODEL:: CUSTOM - SIZE: " + this.model.size() + " | PARAMS: " + confidence * 100 + " - " + support);
        
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + confidence * 100 + "%, > " + support + ",");
        for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
            String v = entry.getValue();
            log = new StringBuilder(log.append(v) + ",");
        }
        FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
        this.writeModelToFile("CUSTOM_" + confidence + "_" + support);
    }
    
    private Model constructShapesWithPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        classToPropWithObjTypes.forEach((encodedClassIRI, propToObjectType) -> {
            if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
                IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
                //NODE SHAPES PRUNING
                if (classInstanceCount.get(encoder.encode(subj.stringValue())) > support) {
                    String nodeShape = "shape:" + subj.getLocalName() + "Shape";
                    b.subject(nodeShape)
                            .add(RDF.TYPE, SHACL.NODE_SHAPE)
                            .add(SHACL.TARGET_CLASS, subj)
                            .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                            .add(SHACL.CLOSED, false);
                    
                    if (propToObjectType != null) {
                        Map<Integer, Set<Integer>> propToObjectTypesLocal = performNodeShapePropPruning(encodedClassIRI, propToObjectType, confidence, support);
                        constructNodePropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectTypesLocal);
                    }
                }
            } else {
                System.out.println("constructShapesWithPruning:: INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
            }
        });
        m = b.build();
        return m;
    }
    
    private Map<Integer, Set<Integer>> performNodeShapePropPruning(Integer classEncodedLabel, Map<Integer, Set<Integer>> propToObjectType, Double confidence, Integer support) {
        Map<Integer, Set<Integer>> propToObjectTypesLocal = new HashMap<>();
        propToObjectType.forEach((prop, propObjectTypes) -> {
            HashSet<Integer> objTypesSet = new HashSet<>();
            propObjectTypes.forEach(encodedObjectType -> {
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(classEncodedLabel, prop, encodedObjectType);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    SupportConfidence sc = shapeTripletSupport.get(tuple3);
                    if (support == 1) {
                        if (sc.getConfidence() > confidence && sc.getSupport() >= support) {
                            objTypesSet.add(encodedObjectType);
                        }
                    } else {
                        if (sc.getConfidence() > confidence && sc.getSupport() > support) {
                            objTypesSet.add(encodedObjectType);
                        }
                    }
                    
                }
            });
            if (objTypesSet.size() != 0) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }
        });
        return propToObjectTypesLocal;
    }
    
    
    // Methods used while constructing Shapes with and without pruning
    
    public HashMap<String, String> computeShapeStatistics(Model currentModel) {
        HashMap<String, String> shapesStats = new HashMap<>();
        
        SailRepository db = new SailRepository(new MemoryStore());
        db.init();
        try (RepositoryConnection conn = db.getConnection()) {
            conn.add(currentModel); // You need to load the model in the repo to query
            
            //COUNT STATS
            for (int i = 1; i <= 5; i++) {
                String type = "count";
                TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                Value queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getCsvHeader().get(i), literalCount.stringValue());
                }
            }
            //AVERAGE STATS
            for (int i = 1; i <= 4; i++) {
                String type = "avg";
                TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                Value queryOutput = executeQuery(query, type);
                if (queryOutput != null && queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getAverageHeader().get(i), literalCount.stringValue());
                }
                //MAX STATS
                type = "max";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput != null && queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMaxHeader().get(i), literalCount.stringValue());
                }
                //MIN STATS
                type = "min";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput != null && queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMinHeader().get(i), literalCount.stringValue());
                }
            }
        } finally {
            db.shutDown();
        }
        return shapesStats;
    }
    
    public void writeModelToFileInRdfStar(String fileIdentifier) {
        Path path = Paths.get(Main.datasetPath);
        String fileName = FilenameUtils.removeExtension(path.getFileName().toString()) + "_" + fileIdentifier + "_SHACL.ttls";
        System.out.println("::: SHACLER ~ WRITING MODEL TO FILE: " + fileName);
        try {
            FileWriter fileWriter = new FileWriter(ConfigManager.getProperty("output_file_path") + fileName, false);
            Rio.write(model, fileWriter, RDFFormat.TURTLESTAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void writeModelToFile(String fileIdentifier) {
        Path path = Paths.get(Main.datasetPath);
        String fileName = FilenameUtils.removeExtension(path.getFileName().toString()) + "_" + fileIdentifier + "_SHACL.ttl";
        System.out.println("::: SHACLER ~ WRITING MODEL TO FILE: " + fileName);
        try {
            FileWriter fileWriter = new FileWriter(ConfigManager.getProperty("output_file_path") + fileName, false);
            Rio.write(model, fileWriter, RDFFormat.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void writeModelToFileWithPrettyFormatting(String fileIdentifier) {
        Path path = Paths.get(Main.datasetPath);
        String fileName = FilenameUtils.removeExtension(path.getFileName().toString()) + "_" + fileIdentifier + "_SHACL.ttl";
        String fileAddress = ConfigManager.getProperty("output_file_path") + fileName;
        System.out.println("::: SHACLER ~ WRITING MODEL TO FILE: " + fileName);
        try {
            FileWriter fileWriter = new FileWriter(fileAddress, false);
            Rio.write(model, fileWriter, RDFFormat.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String prettyFileAddress = ConfigManager.getProperty("output_file_path") + FilenameUtils.removeExtension(path.getFileName().toString()) + "_" + fileIdentifier + "_SHACL_PRETTY.ttl";
        TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
        // Build or load a Jena Model
        org.apache.jena.rdf.model.Model model = RDFDataMgr.loadModel(fileAddress);
        System.out.println(model.size());
        // Either create a string...
        String prettyPrintedModel = formatter.apply(model);
        // ...or write directly to an OutputStream
        //formatter.accept(model, System.out);
        
        try {
            formatter.accept(model, new FileOutputStream(prettyFileAddress));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //System.out.println(prettyPrintedModel);
        
        
    }
    
    private Value executeQuery(TupleQuery query, String bindingName) {
        Value queryOutput = null;
        // A QueryResult is also an AutoCloseable resource, so make sure it gets closed when done.
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet solution = result.next();
                //System.out.println("?count = " + solution.getValue(bindingName));
                queryOutput = solution.getValue(bindingName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queryOutput;
    }
    
    public void setPropWithClassesHavingMaxCountOne(Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne) {
        this.propWithClassesHavingMaxCountOne = propWithClassesHavingMaxCountOne;
    }
}