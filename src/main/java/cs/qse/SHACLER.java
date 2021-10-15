package cs.qse;

import cs.utils.*;
import cs.utils.encoders.Encoder;
import org.apache.commons.io.FilenameUtils;
import cs.Main;
import org.apache.solr.common.util.Hash;
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
import org.semanticweb.yars.nx.Node;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SHACLER {
    String classIRI;
    public final String SHAPES_NAMESPACE = "http://shaclshapes.org/";
    HashMap<Node, HashSet<String>> propToType = null;
    ValueFactory factory = SimpleValueFactory.getInstance();
    Model model = null;
    ModelBuilder builder = null;
    Encoder encoder;
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    String logfileAddress = ConfigManager.getProperty("output_file_path") + ConfigManager.getProperty("dataset_name") + ".csv";
    
    public SHACLER() {
        this.builder = new ModelBuilder();
        builder.setNamespace("shape", SHAPES_NAMESPACE);
    }
    
    public SHACLER(Encoder encoder, HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport) {
        this.encoder = encoder;
        this.builder = new ModelBuilder();
        this.shapeTripletSupport = shapeTripletSupport;
        builder.setNamespace("shape", SHAPES_NAMESPACE);
    }
    
    public void setParams(Node classNode, HashMap<Node, HashSet<String>> propToType) {
        this.classIRI = classNode.getLabel();
        this.propToType = propToType;
    }
    
    public void setParams(String classLabel, HashMap<Node, HashSet<String>> propToType) {
        this.classIRI = classLabel;
        this.propToType = propToType;
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
        System.out.println("MODEL:: CUSTOM - SIZE: " + this.model.size() + " | PARAMS: " + confidence*100 + " - " + support);
        
        HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(this.model);
        StringBuilder log = new StringBuilder(ConfigManager.getProperty("dataset_name") + ", > " + confidence*100 + "%, > " + support + ",");
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
            
            String nodeShape = "shape:" + subj.getLocalName() + "Shape";
            b.subject(nodeShape)
                    .add(RDF.TYPE, SHACL.NODE_SHAPE)
                    .add(SHACL.TARGET_CLASS, subj)
                    .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                    .add(SHACL.CLOSED, false);
            
            if (propToObjectType != null) {
                HashMap<Integer, HashSet<Integer>> propToObjectTypesLocal = performPruning(classEncodedLabel, propToObjectType, confidence, support);
                constructNodePropertyShapes(b, subj, nodeShape, propToObjectTypesLocal);
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
                    .add(SHACL.PATH, property)
                    .add(SHACL.MIN_COUNT, 1);
            //.add(SHACL.MAX_COUNT, 1);
            
            propObjectTypes.forEach(encodedObjectType -> {
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
    
    private HashMap<Integer, HashSet<Integer>> performPruning(Integer classEncodedLabel, HashMap<Integer, HashSet<Integer>> propToObjectType, Double confidence, Integer support) {
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
    
            HashMap<Integer, String> header = new HashMap<>();
            header.put(1, "COUNT_NS");
            header.put(2, "COUNT_NSP");
            header.put(3, "COUNT_CC");
            header.put(4, "COUNT_LC");
            //COUNT STATS
            for (int i = 1; i <= 4; i++) {
                String type = "count";
                TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                Value queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(header.get(i), literalCount.stringValue());
                }
            }
    
            HashMap<Integer, String>  avgHeader = new HashMap<>();
            avgHeader.put(1, "AVG_NSP");
            avgHeader.put(2, "AVG_CC");
            avgHeader.put(3, "AVG_LC");
    
    
            HashMap<Integer, String>  minHeader = new HashMap<>();
            minHeader.put(1, "MIN_NSP");
            minHeader.put(2, "MIN_CC");
            minHeader.put(3, "MIN_LC");
    
    
            HashMap<Integer, String>  maxHeader = new HashMap<>();
            maxHeader.put(1, "MAX_NSP");
            maxHeader.put(2, "MAX_CC");
            maxHeader.put(3, "MAX_LC");
            
            //AVERAGE STATS
            for (int i = 1; i <= 3; i++) {
                String type = "avg";
                TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                Value queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(avgHeader.get(i), literalCount.stringValue());
                }
                //MAX STATS
                type = "max";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(maxHeader.get(i), literalCount.stringValue());
                }
                //MIN STATS
                type = "min";
                query = conn.prepareTupleQuery(FilesUtil.readShaclStatsQuery("query" + i, type));
                queryOutput = executeQuery(query, type);
                if (queryOutput.isLiteral()) {
                    Literal literalCount = (Literal) queryOutput;
                    shapesStats.put(minHeader.get(i), literalCount.stringValue());
                }
            }
        } finally {
            db.shutDown();
        }
        return shapesStats;
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
    
    /**
     * FIXME: This method is being used by Endpoint Parser for now, remove it once you update Endpoint Parser to support pruning like above methods.
     */
    public void constructShape() {
        Model m = null;
        ModelBuilder b = new ModelBuilder();
        IRI subj = factory.createIRI(this.classIRI);
        
        String nodeShape = "shape:" + subj.getLocalName() + "Shape";
        b.subject(nodeShape)
                .add(RDF.TYPE, SHACL.NODE_SHAPE)
                .add(SHACL.TARGET_CLASS, subj)
                .add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
                .add(SHACL.CLOSED, false);
        
        if (propToType != null) {
            propToType.forEach((prop, propObjectTypes) -> {
                IRI property = factory.createIRI(prop.getLabel());
                IRI propShape = factory.createIRI("sh:" + property.getLocalName() + subj.getLocalName() + "ShapeProperty");
                b.subject(nodeShape)
                        .add(SHACL.PROPERTY, propShape);
                b.subject(propShape)
                        .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                        .add(SHACL.PATH, property)
                        .add(SHACL.MIN_COUNT, 1);
                //.add(SHACL.MAX_COUNT, 1);
                
                propObjectTypes.forEach(objectType -> {
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
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
        
        m = b.build();
        //printModel(m);
        model = builder.build();
        model.addAll(m);
    }
    
    public Statement createStatement(IRI s, IRI p, IRI o) {
        return factory.createStatement(s, p, o);
    }
    
    public void printModel() {
        Rio.write(model, System.out, RDFFormat.TURTLE);
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
    
    public void writeModelToFile() {
        Path path = Paths.get(Main.datasetPath);
        String fileName = FilenameUtils.removeExtension(path.getFileName().toString()) + "_" + "SHACL.ttl";
        System.out.println("::: SHACLER ~ WRITING MODEL TO FILE: " + fileName);
        try {
            FileWriter fileWriter = new FileWriter(ConfigManager.getProperty("output_file_path") + fileName, false);
            Rio.write(model, fileWriter, RDFFormat.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void printModel(Model m) {
        Rio.write(m, System.out, RDFFormat.TURTLE);
    }
    

    
    
    /*    public void loadModelInRepo() {
        db.init(); // Create a new Repository
        try (RepositoryConnection conn = db.getConnection()) {
            conn.add(model);
            int count = 0;
            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    System.out.println("db contains: " + st);
                    count++;
                }
            }
            System.out.println("DB COUNT: " + count);
            System.out.println("Model COUNT: " + model.size());
        } finally {
            db.shutDown();
        }
    }*/
}
