package cs.qse;

import cs.Main;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.Tuple3;
import cs.utils.encoders.Encoder;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ShapesExtractor {
    Model model = null;
    ModelBuilder builder = null;
    Encoder encoder;
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    HashMap<Integer, Integer> classInstanceCount;
    ValueFactory factory = SimpleValueFactory.getInstance();
    String logfileAddress = Constants.EXPERIMENTS_RESULT;
    
    public ShapesExtractor(Encoder encoder, HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport, HashMap<Integer, Integer> classInstanceCount) {
        this.encoder = encoder;
        this.builder = new ModelBuilder();
        this.shapeTripletSupport = shapeTripletSupport;
        this.classInstanceCount = classInstanceCount;
        builder.setNamespace("shape", Constants.SHAPES_NAMESPACE);
    }
    
    public void constructDefaultShapes(HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes) {
        this.model = null;
        this.builder = new ModelBuilder();
        this.model = builder.build();
        this.model.addAll(constructShapeWithoutPruning(classToPropWithObjTypes));
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
    
    public void constructPrunedShapes(HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
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
    private Model constructShapeWithoutPruning(HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes) {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        classToPropWithObjTypes.forEach((classEncodedLabel, propToObjectType) -> {
            IRI subj = factory.createIRI(encoder.decode(classEncodedLabel));
            
            String nodeShape = "shape:" + subj.getLocalName() + "Shape";
            b.subject(nodeShape)
                    .add(RDF.TYPE, SHACL.NODE_SHAPE)
                    .add(SHACL.TARGET_CLASS, subj)
                    .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                    .add(SHACL.CLOSED, false);
            
            if (propToObjectType != null) {
                constructNodePropertyShapes(b, subj, nodeShape, propToObjectType);
            }
        });
        m = b.build();
        return m;
    }
    
    private Model constructShapesWithPruning(HashMap<Integer, HashMap<Integer, HashSet<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        classToPropWithObjTypes.forEach((classEncodedLabel, propToObjectType) -> {
            IRI subj = factory.createIRI(encoder.decode(classEncodedLabel));
            //NODE SHAPES PRUNING
            if (classInstanceCount.get(encoder.encode(subj.stringValue())) > support) {
                String nodeShape = "shape:" + subj.getLocalName() + "Shape";
                b.subject(nodeShape)
                        .add(RDF.TYPE, SHACL.NODE_SHAPE)
                        .add(SHACL.TARGET_CLASS, subj)
                        .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                        .add(SHACL.CLOSED, false);
                
                if (propToObjectType != null) {
                    HashMap<Integer, HashSet<Integer>> propToObjectTypesLocal = performNodeShapePropPruning(classEncodedLabel, propToObjectType, confidence, support);
                    constructNodePropertyShapes(b, subj, nodeShape, propToObjectTypesLocal);
                }
            }
            
        });
        m = b.build();
        return m;
    }
    
    private void constructNodePropertyShapes(ModelBuilder b, IRI subj, String nodeShape, HashMap<Integer, HashSet<Integer>> propToObjectTypesLocal) {
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
                if(shapeTripletSupport.containsKey(tuple3)){
                    if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                        b.subject(propShape).add(SHACL.MIN_COUNT, 1);
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
                        IRI objectTypeIri = factory.createIRI(objectType);
                        b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                        b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                    }
                } else {
                    // in case the type is null, we set it default as string
                    b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                }
            });
        });
    }
    
    private HashMap<Integer, HashSet<Integer>> performNodeShapePropPruning(Integer classEncodedLabel, HashMap<Integer, HashSet<Integer>> propToObjectType, Double confidence, Integer support) {
        HashMap<Integer, HashSet<Integer>> propToObjectTypesLocal = new HashMap<>();
        propToObjectType.forEach((prop, propObjectTypes) -> {
            HashSet<Integer> objTypesSet = new HashSet<>();
            propObjectTypes.forEach(encodedObjectType -> {
                Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(classEncodedLabel, prop, encodedObjectType);
                if (shapeTripletSupport.containsKey(tuple3)) {
                    SC sc = shapeTripletSupport.get(tuple3);
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
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getAverageHeader().get(i), literalCount.stringValue());
                }
                //MAX STATS
                type = "max";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMaxHeader().get(i), literalCount.stringValue());
                }
                //MIN STATS
                type = "min";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(ExperimentsUtil.getMinHeader().get(i), literalCount.stringValue());
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
    
}
