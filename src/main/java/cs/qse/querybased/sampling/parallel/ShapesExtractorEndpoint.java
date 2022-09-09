package cs.qse.querybased.sampling.parallel;

import cs.Main;
import cs.qse.filebased.SupportConfidence;
import cs.qse.common.ExperimentsUtil;
import cs.utils.*;
import cs.qse.common.encoders.ConcurrentEncoder;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class is used to extract/construct shapes (default and pruned) using all the information/metadata collected in Parser
 * This uses concurrent encoder as compared to others (only difference)
 */
public class ShapesExtractorEndpoint {
    Model model = null;
    ModelBuilder builder;
    ConcurrentEncoder encoder;
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport;
    Map<Integer, Integer> classInstanceCount;
    Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne;
    ValueFactory factory = SimpleValueFactory.getInstance();
    String logfileAddress = Constants.EXPERIMENTS_RESULT;
    Boolean isSamplingOn = false;
    Map<Integer, Integer> propCount;
    Map<Integer, Integer> sampledPropCount;
    Map<Integer, List<Integer>> sampledEntitiesPerClass; // Size == O(T*entityThreshold)
    Map<Integer, List<Double>> supportToRelativeSupport = new HashMap<>();
    
    public ShapesExtractorEndpoint(ConcurrentEncoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, Integer> classInstanceCount) {
        this.encoder = encoder;
        this.builder = new ModelBuilder();
        this.shapeTripletSupport = shapeTripletSupport;
        this.classInstanceCount = classInstanceCount;
        builder.setNamespace("shape", Constants.SHAPES_NAMESPACE);
    }
    
    public void constructDefaultShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.setNamespace("shape", Constants.SHAPES_NAMESPACE);
        this.model.addAll(constructShapeWithoutPruning(classToPropWithObjTypes));
        System.out.println("MODEL:: DEFAULT - SIZE: " + this.model.size());
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        
        StringBuilder header = new StringBuilder("DATASET,Confidence,Support,");
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + 1.0 + "%, > " + 1.0 + ",");
        for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
            String v = entry.getValue();
            log = new StringBuilder(log.append(v).append(","));
            header = new StringBuilder(header.append(entry.getKey()) + ",");
        }
        
        FilesUtil.writeToFileInAppendMode(header.toString(), logfileAddress);
        FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
        this.writeModelToFile("DEFAULT");
    }
    
    public void constructPrunedShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.setNamespace("shape", Constants.SHAPES_NAMESPACE);
        this.model.addAll(constructShapesWithPruning(classToPropWithObjTypes, confidence, support));
        System.out.println("MODEL:: CUSTOM - SIZE: " + this.model.size() + " | PARAMS: " + confidence * 100 + " - " + support);
        
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + confidence * 100 + "%, > " + support + ",");
        for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
            String v = entry.getValue();
            log = new StringBuilder(log.append(v).append(","));
        }
        FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
        this.writeModelToFile("CUSTOM_" + confidence + "_" + support);
        //System.out.println("RelativeSupportMap::");
        //supportToRelativeSupport.forEach((k, v) -> {System.out.println(k + " -> " + v);});
    }
    
    
    private Model constructShapeWithoutPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
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
    
    //Also include computation of relative support if isSampling is true
    private Model constructShapesWithPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
        System.out.println("Invoked::constructShapesWithPruning()");
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> entry : classToPropWithObjTypes.entrySet()) {
            Integer encodedClassIRI = entry.getKey();
            Map<Integer, Set<Integer>> propToObjectType = entry.getValue();
            if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
                IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
                int classId = encoder.encode(subj.stringValue());
                int classInstances = classInstanceCount.get(classId);
                
                //NODE SHAPES PRUNING based on support
                if (support == 1) {
                    if (classInstances >= support) {
                        prepareNodeAndPropertyShapes(confidence, support, b, encodedClassIRI, propToObjectType, subj);
                    }
                } else {
                    if (classInstances > support) {
                        prepareNodeAndPropertyShapes(confidence, support, b, encodedClassIRI, propToObjectType, subj);
                    }
                }
            } else {
                System.out.println("constructShapesWithPruning:: INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
            }
        }
        m = b.build();
        return m;
    }
    
    private void prepareNodeAndPropertyShapes(Double confidence, Integer support, ModelBuilder b, Integer encodedClassIRI, Map<Integer, Set<Integer>> propToObjectType, IRI subj) {
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
    
    private Map<Integer, Set<Integer>> performNodeShapePropPruning(Integer classEncodedLabel, Map<Integer, Set<Integer>> propToObjectType, Double confidence, Integer support) {
        Map<Integer, Set<Integer>> propToObjectTypesLocal = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : propToObjectType.entrySet()) {
            Integer prop = entry.getKey();
            Set<Integer> propObjectTypes = entry.getValue();
            HashSet<Integer> objTypesSet = new HashSet<>();
    
            //compute Relative Support if sampling is on
            double relativeSupport = 0;
            if (isSamplingOn) {
                relativeSupport = (support * Math.min(((double) sampledPropCount.get(prop) / (double) propCount.get(prop)), ((double) sampledEntitiesPerClass.get(classEncodedLabel).size() / (double) classInstanceCount.get(classEncodedLabel))));
                if (supportToRelativeSupport.get(support) != null) {
                    supportToRelativeSupport.get(support).add(relativeSupport);
                } else {
                    List<Double> list = new ArrayList<>();
                    list.add(relativeSupport);
                    supportToRelativeSupport.put(support, list);
                }
            }
            
            for (Integer encodedObjectType : propObjectTypes) {
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(classEncodedLabel, prop, encodedObjectType);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    SupportConfidence sc = shapeTripletSupport.get(tuple3);
                    
                    if (support == 1) {
                        if (sc.getConfidence() > confidence && sc.getSupport() >= support) {
                            objTypesSet.add(encodedObjectType);
                        }
                    }
                    
                    if (isSamplingOn && support != 1) {
                        //support = (int) relativeSupport;
                        if (sc.getConfidence() > confidence && sc.getSupport() > relativeSupport) {
                            objTypesSet.add(encodedObjectType);
                        }
                    } else {
                        if (sc.getConfidence() > confidence && sc.getSupport() > support) {
                            objTypesSet.add(encodedObjectType);
                        }
                    }
                }
            }
            if (objTypesSet.size() != 0) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }
        }
        return propToObjectTypesLocal;
    }
    
    public HashMap<String, String> computeShapeStatistics(Model currentModel) {
        HashMap<String, String> shapesStats = new HashMap<>();
        Repository db = new SailRepository(new MemoryStore());
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
                } else {
                    shapesStats.put(ExperimentsUtil.getAverageHeader().get(i), "-999");
                }
                //MAX STATS
                type = "max";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput != null && queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMaxHeader().get(i), literalCount.stringValue());
                } else {
                    shapesStats.put(ExperimentsUtil.getMaxHeader().get(i), "-999");
                }
                //MIN STATS
                type = "min";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput != null && queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMinHeader().get(i), literalCount.stringValue());
                } else {
                    shapesStats.put(ExperimentsUtil.getMinHeader().get(i), "-999");
                }
            }
        } finally {
            db.shutDown();
        }
        return shapesStats;
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
    
    public void setSampledEntitiesPerClass(Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        this.sampledEntitiesPerClass = sampledEntitiesPerClass;
    }
    
    
    public Boolean isSamplingOn() {
        return isSamplingOn;
    }
    
    public void setSamplingOn(Boolean samplingOn) {
        isSamplingOn = samplingOn;
    }
    
    public void setPropCount(Map<Integer, Integer> propCount) {
        this.propCount = propCount;
    }
    
    public void setSampledPropCount(Map<Integer, Integer> sampledPropCount) {
        this.sampledPropCount = propCount;
    }
}
