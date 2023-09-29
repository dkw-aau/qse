package cs.qse.common;

import com.google.common.collect.Lists;
import cs.Main;
import cs.qse.common.ExperimentsUtil;
import cs.qse.common.TurtlePrettyFormatter;
import cs.qse.common.encoders.Encoder;
import cs.qse.filebased.SupportConfidence;
import cs.utils.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.eclipse.rdf4j.model.util.Values.bnode;

/**
 * This class is used to extract/construct shapes (default and pruned) using all the information/metadata collected in Parser
 */
public class ShapesExtractor {
    //Model model = null;
    ModelBuilder builder;
    Encoder encoder;
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
    String typePredicate;

    /**
     * ============================================= Constructor =======================================================
     */

    public ShapesExtractor() {
    }

    public ShapesExtractor(Encoder encoder, Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport, Map<Integer, Integer> classInstanceCount, String typePredicate) {
        this.encoder = encoder;
        this.builder = new ModelBuilder();
        this.shapeTripletSupport = shapeTripletSupport;
        this.classInstanceCount = classInstanceCount;
        this.typePredicate = typePredicate;
        builder.setNamespace("shape", Constants.SHAPES_NAMESPACE);
    }


    /**
     * ==================================== QSE-Default Shapes Construction ============================================
     */

    public void constructDefaultShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes) {
        File dbDir = new File(Main.outputFilePath + "db_default");
        performDirCheck(dbDir);
        Repository db = new SailRepository(new NativeStore(new File(dbDir.getAbsolutePath()))); // Create a new Repository.

        try (RepositoryConnection conn = db.getConnection()) { // Open a connection to the database

            constructShapeWithoutPruning(classToPropWithObjTypes, conn);
            conn.setNamespace("shape", Constants.SHAPES_NAMESPACE);
            conn.setNamespace("shape", Constants.SHACL_NAMESPACE);

            PostConstraintsAnnotator pca = new PostConstraintsAnnotator(conn);
            pca.addShNodeConstraint();

            // Compute Statistics and prepare logs
            System.out.println("MODEL:: DEFAULT - SIZE: " + conn.size());
            HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(conn);

            StringBuilder header = new StringBuilder("DATASET,Confidence,Support,");
            StringBuilder log = new StringBuilder(Main.datasetName + ", > " + 1.0 + "%, > " + 1.0 + ",");
            for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
                String v = entry.getValue();
                log = new StringBuilder(log.append(v).append(","));
                header = new StringBuilder(header.append(entry.getKey()) + ",");
            }

            FilesUtil.writeToFileInAppendMode(header.toString(), logfileAddress);
            FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);

            String outputFilePath = this.writeModelToFile("QSE_FULL", conn);
            this.prettyFormatTurtle(outputFilePath);
            FilesUtil.deleteFile(outputFilePath);
        } finally {
            db.shutDown();// before our program exits, make sure the database is properly shut down.
        }
    }

    /**
     * QSE-Default sub-method to construct Node and Property shapes without pruning
     */
    private void constructShapeWithoutPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, RepositoryConnection conn) {
        if (classToPropWithObjTypes.size() > 10000) { // partition to reduce memory consumption by Model creation library
            List<Integer> classesList = new ArrayList<>(classToPropWithObjTypes.keySet());
            List<List<Integer>> classesPartition = Lists.partition(classesList, classToPropWithObjTypes.size() / 4);
            classesPartition.forEach(partition -> {
                Model m = null;
                ModelBuilder b = new ModelBuilder();
                partition.forEach(encodedClassIRI -> {
                    Map<Integer, Set<Integer>> propToObjectType = classToPropWithObjTypes.get(encodedClassIRI);
                    buildShapes(b, encodedClassIRI, propToObjectType);
                });
                m = b.build();
                conn.add(m);
                //System.out.println("Partition size" + partition.size() + " conn.size() = " + conn.size());
            });
        } else {
            Model m = null;
            ModelBuilder b = new ModelBuilder();
            classToPropWithObjTypes.forEach((encodedClassIRI, propToObjectType) -> {
                buildShapes(b, encodedClassIRI, propToObjectType);
            });
            m = b.build();
            conn.add(m);
        }
    }

    private void buildShapes(ModelBuilder b, Integer encodedClassIRI, Map<Integer, Set<Integer>> propToObjectType) {
        if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
            IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
            String nodeShape = Constants.SHAPES_NAMESPACE + subj.getLocalName() + "Shape";
            b.subject(nodeShape)
                    .add(RDF.TYPE, SHACL.NODE_SHAPE)
                    .add(SHACL.TARGET_CLASS, subj);
            //.add(Constants.SUPPORT, classInstanceCount.get(encodedClassIRI));
            //.add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
            //.add(SHACL.CLOSED, true);

            if (propToObjectType != null) {
                constructPropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectType); // Property Shapes
            }
        } else {
            System.out.println("constructShapeWithoutPruning::INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
        }
    }


    /**
     * ====================================  QSE-Pruned Shapes Construction ============================================
     */
    public void constructPrunedShapes(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support) {

        File dbDir = new File(Main.outputFilePath + "db_" + confidence + "_" + support);
        performDirCheck(dbDir);
        Repository db = new SailRepository(new NativeStore(new File(dbDir.getAbsolutePath()))); // Create a new Repository.

        try (RepositoryConnection conn = db.getConnection()) { // Open a connection to the database
            constructShapesWithPruning(classToPropWithObjTypes, confidence, support, conn);
            conn.setNamespace("shape", Constants.SHAPES_NAMESPACE);
            conn.setNamespace("shape", Constants.SHACL_NAMESPACE);

            PostConstraintsAnnotator pca_pruned = new PostConstraintsAnnotator(conn);
            pca_pruned.addShNodeConstraint();

            System.out.println("MODEL:: CUSTOM - SIZE: " + conn.size() + " | PARAMS: " + confidence * 100 + " - " + support);

            HashMap<String, String> currentShapesModelStats = this.computeShapeStatistics(conn);
            StringBuilder log = new StringBuilder(Main.datasetName + ", > " + confidence * 100 + "%, > " + support + ",");
            for (Map.Entry<String, String> entry : currentShapesModelStats.entrySet()) {
                String v = entry.getValue();
                log = new StringBuilder(log.append(v).append(","));
            }
            FilesUtil.writeToFileInAppendMode(log.toString(), logfileAddress);
            String outputFilePath = this.writeModelToFile("QSE_" + confidence + "_" + support, conn);
            this.prettyFormatTurtle(outputFilePath);
            FilesUtil.deleteFile(outputFilePath);
        } finally {
            db.shutDown(); // before our program exits, make sure the database is properly shut down.
        }
    }

    /**
     * QSE-Pruned sub-method Construct Node and Property Shapes with specified support and confidence
     * Also include computation of relative support if QSE-Approximate (isSampling) is true
     */
    private void constructShapesWithPruning(Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes, Double confidence, Integer support, RepositoryConnection conn) {
        if (classToPropWithObjTypes.size() > 10000) { // partition to reduce memory consumption by Model creation library
            List<Integer> classesList = new ArrayList<>(classToPropWithObjTypes.keySet());
            List<List<Integer>> classesPartition = Lists.partition(classesList, classToPropWithObjTypes.size() / 4);

            classesPartition.forEach(partition -> {
                Model m = null;
                ModelBuilder b = new ModelBuilder();
                partition.forEach(encodedClassIRI -> {
                    Map<Integer, Set<Integer>> propToObjectType = classToPropWithObjTypes.get(encodedClassIRI);
                    buildAndPruneShapes(confidence, support, b, encodedClassIRI, propToObjectType);
                });
                m = b.build();
                conn.add(m);
            });
        } else {
            Model m = null;
            ModelBuilder b = new ModelBuilder();
            for (Map.Entry<Integer, Map<Integer, Set<Integer>>> entry : classToPropWithObjTypes.entrySet()) {
                Integer encodedClassIRI = entry.getKey();
                Map<Integer, Set<Integer>> propToObjectType = entry.getValue();
                buildAndPruneShapes(confidence, support, b, encodedClassIRI, propToObjectType);
            }
            m = b.build();
            conn.add(m);
        }
    }

    private void buildAndPruneShapes(Double confidence, Integer support, ModelBuilder b, Integer encodedClassIRI, Map<Integer, Set<Integer>> propToObjectType) {
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

    /**
     * QSE-Pruned sub-method
     */
    private void prepareNodeAndPropertyShapes(Double confidence, Integer support, ModelBuilder b, Integer encodedClassIRI, Map<Integer, Set<Integer>> propToObjectType, IRI subj) {
        //String nodeShape = "shape:" + subj.getLocalName() + "Shape";
        String nodeShape = Constants.SHAPES_NAMESPACE + subj.getLocalName() + "Shape";
        b.subject(nodeShape)
                .add(RDF.TYPE, SHACL.NODE_SHAPE)
                .add(SHACL.TARGET_CLASS, subj);
        //.add(SHACL.IGNORED_PROPERTIES, RDF.TYPE)
        //.add(SHACL.CLOSED, true);

        if (propToObjectType != null) {
            Map<Integer, Set<Integer>> propToObjectTypesLocalPositive = performPropShapePruningPositive(encodedClassIRI, propToObjectType, confidence, support);

            System.out.println("-----------------------");
            supportToRelativeSupport.forEach((k, v) -> System.out.println("supportToRelativeSupport:: " + k + " - " + v));
            System.out.println("-----------------------");
            if (ConfigManager.getProperty("qse_validation_with_shNot") != null) {
                if (Boolean.parseBoolean(ConfigManager.getProperty("qse_validation_with_shNot"))) {
                    Map<Integer, Set<Integer>> propToObjectTypesLocalNegative = performPropShapePruningNegative(encodedClassIRI, propToObjectType, confidence, support);
                    constructPropertyShapesWithShNot(b, subj, encodedClassIRI, nodeShape, propToObjectTypesLocalPositive, propToObjectTypesLocalNegative); // call this to capture negative as sh:not
                } else {
                    constructPropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectTypesLocalPositive);// call this for positive only
                }
            } else {
                constructPropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectTypesLocalPositive);// call this for positive only
            }
        }
    }

    /**
     * QSE-Pruned sub-method
     */
    private Map<Integer, Set<Integer>> performPropShapePruningPositive(Integer classEncodedLabel, Map<Integer, Set<Integer>> propToObjectType, Double confidence, Integer support) {
        Map<Integer, Set<Integer>> propToObjectTypesLocal = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : propToObjectType.entrySet()) {
            Integer prop = entry.getKey();
            Set<Integer> propObjectTypes = entry.getValue();
            HashSet<Integer> objTypesSet = new HashSet<>();

            //Make sure instant type (either rdf:type or wdt:P31 of WikiData) is not pruned
            IRI property = factory.createIRI(encoder.decode(prop));
            boolean isInstantTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstantTypeProperty) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }

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

            positivePruning(classEncodedLabel, confidence, support, prop, propObjectTypes, objTypesSet, relativeSupport);
            if (objTypesSet.size() != 0) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }
        }
        return propToObjectTypesLocal;
    }

    private Map<Integer, Set<Integer>> performPropShapePruningNegative(Integer classEncodedLabel, Map<Integer, Set<Integer>> propToObjectType, Double confidence, Integer support) {
        Map<Integer, Set<Integer>> propToObjectTypesLocal = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : propToObjectType.entrySet()) {
            Integer prop = entry.getKey();
            Set<Integer> propObjectTypes = entry.getValue();
            HashSet<Integer> objTypesSet = new HashSet<>();

            //Make sure instant type (either rdf:type or wdt:P31 of WikiData) is not pruned
            IRI property = factory.createIRI(encoder.decode(prop));
            boolean isInstantTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstantTypeProperty) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }

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

            negativePruning(classEncodedLabel, confidence, support, prop, propObjectTypes, objTypesSet, relativeSupport);

            if (objTypesSet.size() != 0) {
                propToObjectTypesLocal.put(prop, objTypesSet);
            }
        }
        return propToObjectTypesLocal;
    }

    private void positivePruning(Integer classEncodedLabel, Double confidence, Integer support, Integer prop, Set<Integer> propObjectTypes, HashSet<Integer> objTypesSet, double relativeSupport) {
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
    }

    private void negativePruning(Integer classEncodedLabel, Double confidence, Integer support, Integer prop, Set<Integer> propObjectTypes, HashSet<Integer> objTypesSet, double relativeSupport) {
        for (Integer encodedObjectType : propObjectTypes) {
            Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(classEncodedLabel, prop, encodedObjectType);


            if (shapeTripletSupport.containsKey(tuple3)) {
                SupportConfidence sc = shapeTripletSupport.get(tuple3);

                if (support == 1) {
                    if (sc.getConfidence() <= confidence && sc.getSupport() < support) {
                        objTypesSet.add(encodedObjectType);
                    }
                }

                if (isSamplingOn && support != 1) {
                    //support = (int) relativeSupport;
                    if (sc.getConfidence() <= confidence && sc.getSupport() < relativeSupport) {
                        objTypesSet.add(encodedObjectType);
                    }
                } else {
                    if (sc.getConfidence() <= confidence && sc.getSupport() < support) {
                        objTypesSet.add(encodedObjectType);
                    }
                }
            }
        }
    }


    //==================================== SHARED Methods between QSE-Pruned and QSE-Default===========================

    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to build node and property shapes
     */
    private void constructPropertyShapes(ModelBuilder b, IRI subj, Integer subjEncoded, String nodeShape, Map<Integer, Set<Integer>> propToObjectTypesLocal) {
        Map<String, Integer> propDuplicateDetector = new HashMap<>();

        propToObjectTypesLocal.forEach((prop, propObjectTypes) -> {
            ModelBuilder localBuilder = new ModelBuilder();
            IRI property = factory.createIRI(encoder.decode(prop));
            String localName = property.getLocalName();

            boolean isInstanceTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstanceTypeProperty) {
                localName = "instanceType";
            }

            if (propDuplicateDetector.containsKey(localName)) {
                int freq = propDuplicateDetector.get(localName);
                propDuplicateDetector.put(localName, freq + 1);
                localName = localName + "_" + freq;
            }
            propDuplicateDetector.putIfAbsent(localName, 1);

            IRI propShape = factory.createIRI(Constants.SHAPES_NAMESPACE + localName + subj.getLocalName() + "ShapeProperty");

            b.subject(nodeShape)
                    .add(SHACL.PROPERTY, propShape);
            b.subject(propShape)
                    .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                    .add(SHACL.PATH, property);

            if (isInstanceTypeProperty) {
                Resource head = bnode();
                List<Resource> members = Arrays.asList(new Resource[]{subj});
                Model tempModel = RDFCollections.asRDF(members, head, new LinkedHashModel());
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                });
                tempModel.add(propShape, SHACL.IN, head);
                b.build().addAll(tempModel);
                b.build().addAll(localBuilder.build());
            }

            int numberOfObjectTypes = propObjectTypes.size();

            if (numberOfObjectTypes == 1 && !isInstanceTypeProperty) {
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));

                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }
                    String objectType = encoder.decode(encodedObjectType);
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                        } else {
                            //objectType = objectType.replace("<", "").replace(">", "");
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                            } else {
                                //IRI objectTypeIri = factory.createIRI(objectType);
                                //b.subject(propShape).add(SHACL.CLASS, objectType);
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                                if (objectType.equals(Constants.OBJECT_UNDEFINED_TYPE))
                                    b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            }
                        }
                    } else {
                        // in case the type is null, we set it default as string
                        b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                    }
                });

                b.build().addAll(localBuilder.build());
            }
            if (numberOfObjectTypes > 1) {
                List<Resource> members = new ArrayList<>();
                Resource headMember = bnode();


                for (Integer encodedObjectType : propObjectTypes) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    String objectType = encoder.decode(encodedObjectType);
                    Resource currentMember = bnode();
                    //Cardinality Constraints
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }

                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            localBuilder.subject(currentMember).add(SHACL.DATATYPE, objectTypeIri);
                            localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.LITERAL);

                            annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);

                        } else {
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                localBuilder.subject(currentMember).add(SHACL.CLASS, objectTypeIri);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                            } else {
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
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


    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to build node and property shapes with sh:not
     */
    private void constructPropertyShapesWithShNot(ModelBuilder b, IRI subj, Integer subjEncoded, String nodeShape, Map<Integer, Set<Integer>> PropToObjectTypesPositive, Map<Integer, Set<Integer>> propToObjectTypesNegative) {
        Map<String, Integer> propDuplicateDetector = new HashMap<>();

        //handle positive property shapes
        PropToObjectTypesPositive.forEach((prop, propObjectTypes) -> {
            ModelBuilder localBuilder = new ModelBuilder();
            IRI property = factory.createIRI(encoder.decode(prop));
            String localName = property.getLocalName();

            boolean isInstanceTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstanceTypeProperty) {
                localName = "instanceType";
            }

            if (propDuplicateDetector.containsKey(localName)) {
                int freq = propDuplicateDetector.get(localName);
                propDuplicateDetector.put(localName, freq + 1);
                localName = localName + "_" + freq;
            }
            propDuplicateDetector.putIfAbsent(localName, 1);

            IRI propShape = factory.createIRI(Constants.SHAPES_NAMESPACE + localName + subj.getLocalName() + "ShapeProperty");

            b.subject(nodeShape)
                    .add(SHACL.PROPERTY, propShape);
            b.subject(propShape)
                    .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                    .add(SHACL.PATH, property);

            if (isInstanceTypeProperty) {
                Resource head = bnode();
                List<Resource> members = Arrays.asList(new Resource[]{subj});
                Model tempModel = RDFCollections.asRDF(members, head, new LinkedHashModel());
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                });
                tempModel.add(propShape, SHACL.IN, head);
                b.build().addAll(tempModel);
                b.build().addAll(localBuilder.build());
            }

            int numberOfObjectTypes = propObjectTypes.size();

            if (numberOfObjectTypes == 1 && !isInstanceTypeProperty) {
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }
                    String objectType = encoder.decode(encodedObjectType);
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                        } else {
                            //objectType = objectType.replace("<", "").replace(">", "");
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                            } else {
                                //IRI objectTypeIri = factory.createIRI(objectType);
                                //b.subject(propShape).add(SHACL.CLASS, objectType);
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                                if (objectType.equals(Constants.OBJECT_UNDEFINED_TYPE))
                                    b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            }
                        }
                    } else {
                        // in case the type is null, we set it default as string
                        b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                    }
                });

                b.build().addAll(localBuilder.build());
            }
            if (numberOfObjectTypes > 1) {
                List<Resource> members = new ArrayList<>();
                Resource headMember = bnode();


                for (Integer encodedObjectType : propObjectTypes) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    String objectType = encoder.decode(encodedObjectType);
                    Resource currentMember = bnode();
                    //Cardinality Constraints
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }

                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            localBuilder.subject(currentMember).add(SHACL.DATATYPE, objectTypeIri);
                            localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.LITERAL);

                            annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);

                        } else {
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                localBuilder.subject(currentMember).add(SHACL.CLASS, objectTypeIri);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                            } else {
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
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

        //handle negative property shapes to annotate with sh:not
        propToObjectTypesNegative.forEach((prop, propObjectTypes) -> {
            ModelBuilder localBuilder = new ModelBuilder();
            IRI property = factory.createIRI(encoder.decode(prop));
            String localName = property.getLocalName();

            boolean isInstanceTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstanceTypeProperty) {
                localName = "instanceType";
            }

            if (propDuplicateDetector.containsKey(localName)) {
                int freq = propDuplicateDetector.get(localName);
                propDuplicateDetector.put(localName, freq + 1);
                localName = localName + "_" + freq;
            }
            propDuplicateDetector.putIfAbsent(localName, 1);

            //create node shape IRI for each property shape
            IRI shNotNodeShapeIriForPropShape = factory.createIRI(Constants.SHAPES_NAMESPACE + localName + subj.getLocalName() + "_PS_NotShape");

            // add sh:not to current node shape
            b.subject(nodeShape).add(SHACL.NOT, shNotNodeShapeIriForPropShape);

            // create node shape
            b.subject(shNotNodeShapeIriForPropShape).add(RDF.TYPE, SHACL.NODE_SHAPE);

            //add property shape to current sh:not node shape -> shNotNodeShapeIriForPropShape
            IRI propShape = factory.createIRI(Constants.SHAPES_NAMESPACE + localName + subj.getLocalName() + "ShapeProperty");
            b.subject(shNotNodeShapeIriForPropShape).add(SHACL.PROPERTY, propShape);


            //Add constraints of the property shape

            b.subject(propShape)
                    .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                    .add(SHACL.PATH, property);

            if (isInstanceTypeProperty) {
                Resource head = bnode();
                List<Resource> members = Arrays.asList(new Resource[]{subj});
                Model tempModel = RDFCollections.asRDF(members, head, new LinkedHashModel());
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                });
                tempModel.add(propShape, SHACL.IN, head);
                b.build().addAll(tempModel);
                b.build().addAll(localBuilder.build());
            }

            int numberOfObjectTypes = propObjectTypes.size();

            if (numberOfObjectTypes == 1 && !isInstanceTypeProperty) {
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }
                    String objectType = encoder.decode(encodedObjectType);
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                        } else {
                            //objectType = objectType.replace("<", "").replace(">", "");
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                            } else {
                                //IRI objectTypeIri = factory.createIRI(objectType);
                                //b.subject(propShape).add(SHACL.CLASS, objectType);
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                                if (objectType.equals(Constants.OBJECT_UNDEFINED_TYPE))
                                    b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            }
                        }
                    } else {
                        // in case the type is null, we set it default as string
                        b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                    }
                });

                b.build().addAll(localBuilder.build());
            }
            if (numberOfObjectTypes > 1) {
                List<Resource> members = new ArrayList<>();
                Resource headMember = bnode();


                for (Integer encodedObjectType : propObjectTypes) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    String objectType = encoder.decode(encodedObjectType);
                    Resource currentMember = bnode();
                    //Cardinality Constraints
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            if (Main.extractMaxCardConstraints) {
                                if (propWithClassesHavingMaxCountOne.containsKey(prop) && propWithClassesHavingMaxCountOne.get(prop).contains(subjEncoded)) {
                                    b.subject(propShape).add(SHACL.MAX_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                                }
                            }
                        }
                    }

                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {
                                objectType = objectType.replace("<", "").replace(">", "");
                            }
                            IRI objectTypeIri = factory.createIRI(objectType);
                            localBuilder.subject(currentMember).add(SHACL.DATATYPE, objectTypeIri);
                            localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.LITERAL);

                            annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);

                        } else {
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                localBuilder.subject(currentMember).add(SHACL.CLASS, objectTypeIri);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                            } else {
                                //System.out.println("INVALID Object Type IRI: " + objectType);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
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

    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to annotate shapes with support and confidence
     */
    private void annotateWithSupportAndConfidence(Resource currentMember, ModelBuilder localBuilder, Tuple3<Integer, Integer, Integer> tuple3) {
//        if (shapeTripletSupport.containsKey(tuple3)) {
//            Literal entities = Values.literal(shapeTripletSupport.get(tuple3).getSupport()); // support value
//            localBuilder.subject(currentMember).add(Constants.SUPPORT, entities);
//            Literal confidence = Values.literal(shapeTripletSupport.get(tuple3).getConfidence()); // confidence value
//            localBuilder.subject(currentMember).add(Constants.CONFIDENCE, confidence);
//        }
    }

    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to annotate shapes with support and confidence
     */
    private void annotateWithSupportAndConfidence(IRI propShape, ModelBuilder localBuilder, Tuple3<Integer, Integer, Integer> tuple3) {
//        if (shapeTripletSupport.containsKey(tuple3)) {
//            Literal entities = Values.literal(shapeTripletSupport.get(tuple3).getSupport()); // support value
//            localBuilder.subject(propShape).add(Constants.SUPPORT, entities);
//            Literal confidence = Values.literal(shapeTripletSupport.get(tuple3).getConfidence()); // confidence value
//            localBuilder.subject(propShape).add(Constants.CONFIDENCE, confidence);
//        }
    }

    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to compute statistics of output shapes
     */
    public HashMap<String, String> computeShapeStatistics(RepositoryConnection conn) {
        HashMap<String, String> shapesStats = new HashMap<>();
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

        return shapesStats;
    }

    /**
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to execute a specific SPARQL query
     */
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
     * SHARED METHOD (QSE-Default & QSE-Pruned) : to remove angle brackets from a predicate
     */
    public String remAngBrackets(String typePredicate) {
        return typePredicate.replace("<", "").replace(">", "");
    }


    //============================================= Utility Methods ====================================================

    private static void performDirCheck(File dbDir) {
        if (dbDir.exists()) {
            if (dbDir.delete())
                System.out.println("Deleted already existing directory. This will avoid duplication.");
        }

        if (!dbDir.exists())
            if (dbDir.mkdir())
                System.out.println(dbDir.getAbsoluteFile() + " created successfully.");
            else System.out.println("WARNING::directory creation failed");
    }

    public String writeModelToFile(String fileIdentifier, RepositoryConnection conn) {
        StopWatch watch = new StopWatch();
        watch.start();
        String path = Main.outputFilePath;
        String outputPath = path + Main.datasetName + "_" + fileIdentifier + ".ttl";
        System.out.println("::: ShapesExtractor ~ WRITING MODEL TO FILE: " + outputPath);

        GraphQuery query = conn.prepareGraphQuery("CONSTRUCT WHERE { ?s ?p ?o .}");
        Model model = QueryResults.asModel(query.evaluate());
        try {
            FileWriter fileWriter = new FileWriter(outputPath, false);
            Rio.write(model, fileWriter, RDFFormat.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("writeModelToFile " + " - " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " - " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return outputPath;
    }

    public void prettyFormatTurtle(String inputFilePath) {
        StopWatch watch = new StopWatch();
        watch.start();
        Path path = Paths.get(inputFilePath);
        String fileName = FilenameUtils.removeExtension(path.getFileName().toString()) + "_SHACL.ttl";
        String outputPath = Main.outputFilePath + fileName;
        System.out.println("::: ShapesExtractor ~ PRETTY FORMATTING TURTLE FILE: " + outputPath);
        try {
            new TurtlePrettyFormatter(inputFilePath).format(outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("prettyFormatTurtle " + " - " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " - " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }

    //============================================= Setter Methods =====================================================

    public void setPropWithClassesHavingMaxCountOne(Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne) {
        this.propWithClassesHavingMaxCountOne = propWithClassesHavingMaxCountOne;
    }

    public void setSampledEntitiesPerClass(Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        this.sampledEntitiesPerClass = sampledEntitiesPerClass;
    }


    //============================================= Other Methods ======================================================

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
