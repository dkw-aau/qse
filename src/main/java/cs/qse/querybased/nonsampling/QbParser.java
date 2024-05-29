package cs.qse.querybased.nonsampling;

import cs.Main;
import cs.qse.common.ExperimentsUtil;
import cs.qse.common.Utility;
import cs.qse.common.ShapesExtractor;
import cs.qse.filebased.SupportConfidence;
import cs.utils.*;
import cs.qse.common.encoders.StringEncoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Qb (query-based) Parser
 * This class queries and endpoint to extract SHACL shapes, compute the confidence/support for shape constraints,
 * to perform node and property shape constraints pruning based on defined threshold for confidence and support
 */
public class QbParser {
    private final GraphDBUtils graphDBUtils;
    HashMap<Integer, Integer> classEntityCount;
    //HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport;

    Set<Integer> classes;
    StringEncoder stringEncoder;
    String instantiationProperty;
    Boolean qseFromSpecificClasses;

    long globalComputeSupportMethodTime = 0L;
    public ShapesExtractor shapesExtractor;

    public QbParser(String typeProperty) {
        this.graphDBUtils = new GraphDBUtils();
        int expectedNumberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.stringEncoder = new StringEncoder();
        this.instantiationProperty = typeProperty;
        this.qseFromSpecificClasses = Main.qseFromSpecificClasses;
    }

    // This constructor will be called from SHACTOR DEMO
    public QbParser(int expectedNumberOfClasses, String typePredicate, String graphdbUrl, String graphdbRepository) {
        this.graphDBUtils = new GraphDBUtils(graphdbUrl, graphdbRepository);
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.stringEncoder = new StringEncoder();
        this.instantiationProperty = typePredicate;
        this.qseFromSpecificClasses = false;
    }

    public void run() {
        runParser();
    }

    private void runParser() {
        deleteOutputDir();
        getNumberOfInstancesOfEachClass();
        getDistinctClasses();
        getShapesInfoAndComputeSupport();
        System.out.println("globalComputeSupportMethodTime: " + globalComputeSupportMethodTime);
        extractSHACLShapes(true);
        writeSupportToFile();
        Utility.writeClassFrequencyInFile(classEntityCount, stringEncoder);
        System.out.println("Size: classToPropWithObjTypes :: " + classToPropWithObjTypes.size() + " , Size: shapeTripletSupport :: " + shapeTripletSupport.size());
    }

    /*
        Bugfix evapuermayr:
        Delete output directory, otherwise shapes get duplicated after every run
     */
    private void deleteOutputDir() {
        try {
            Files.walk(Paths.get(Main.outputFilePath))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        } catch (IOException e) {
        }
    }


    public void getNumberOfInstancesOfEachClass() {
        StopWatch watch = new StopWatch();
        watch.start();
        if (qseFromSpecificClasses) {
            List<String> classes = Utility.getListOfClasses();
            for (String classIri : classes) {
                //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
                graphDBUtils.runSelectQuery(setProperty(FilesUtil.readQuery("query2_")).replace(":Class", "<" + classIri + "> ")).forEach(result -> {
                    int classCount = 0;
                    if (result.getBinding("classCount").getValue().isLiteral()) {
                        Literal literalClassCount = (Literal) result.getBinding("classCount").getValue();
                        classCount = literalClassCount.intValue();
                    }
                    classEntityCount.put(stringEncoder.encode(classIri), classCount);
                });
            }

        } else {
            //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
            graphDBUtils.runSelectQuery(setProperty(FilesUtil.readQuery("query2"))).forEach(result -> {
                String c = result.getValue("class").stringValue();
                int classCount = 0;
                if (result.getBinding("classCount").getValue().isLiteral()) {
                    Literal literalClassCount = (Literal) result.getBinding("classCount").getValue();
                    classCount = literalClassCount.intValue();
                }
                classEntityCount.put(stringEncoder.encode(c), classCount);
            });
        }
        watch.stop();
        System.out.println("Time Elapsed getNumberOfInstancesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getNumberOfInstancesOfEachClass ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }

    public void getDistinctClasses() {
        classes = classEntityCount.keySet();
    }

    public void setClasses(Set<Integer> values) {
        classes = values;
    }

    public void getShapesInfoAndComputeSupport() {
        StopWatch watch = new StopWatch();
        watch.start();
        for (Integer classIri : classes) {
            String queryToGetProperties = FilesUtil.readQuery("query4").replace(":Class", " <" + stringEncoder.decode(classIri) + "> ");
            queryToGetProperties = setProperty(queryToGetProperties);
            HashSet<String> props = getPropertiesOfClass(queryToGetProperties);

            HashMap<Integer, Set<Integer>> propToObjTypes = new HashMap<>();

            for (String property : props) {
                HashSet<Integer> objectTypes = new HashSet<>();
                String queryToVerifyLiteralObjectType = buildQuery(classIri, property, "query5");
                queryToVerifyLiteralObjectType = setProperty(queryToVerifyLiteralObjectType);
                //Literal Type Object
                if (graphDBUtils.runAskQuery(queryToVerifyLiteralObjectType)) {
                    String queryToGetDataTypeOfLiteralObject = buildQuery(classIri, property, "query6");
                    queryToGetDataTypeOfLiteralObject = setProperty(queryToGetDataTypeOfLiteralObject); //BUGFIX evapuermayr
                    List<BindingSet> results = graphDBUtils.runSelectQuery(queryToGetDataTypeOfLiteralObject);
                    if (results != null) {
                        results.forEach(row -> {
                            if (row.getValue("objDataType") != null) {
                                String objectType = row.getValue("objDataType").stringValue();
                                objectTypes.add(stringEncoder.encode(objectType));
                                String queryToComputeSupportForLiteralTypeObjects = buildQuery(classIri, property, objectType, "query8");
                                queryToComputeSupportForLiteralTypeObjects = setProperty(queryToComputeSupportForLiteralTypeObjects);
                                computeSupport(classIri, property, objectType, queryToComputeSupportForLiteralTypeObjects);
                            } else {
                                System.out.println("WARNING:: it's null for literal type object .. " + classIri + " - " + property);
                            }
                        });
                    }

                }
                //Non-Literal Type Object
                else {
                    String queryToGetDataTypeOfNonLiteralObjects = buildQuery(classIri, property, "query7");
                    queryToGetDataTypeOfNonLiteralObjects = setProperty(queryToGetDataTypeOfNonLiteralObjects);
                    List<BindingSet> results = graphDBUtils.runSelectQuery(queryToGetDataTypeOfNonLiteralObjects);
                    if (results != null) {
                        results.forEach(row -> {
                            if (row.getValue("objDataType") != null) {
                                String objectType = row.getValue("objDataType").stringValue();
                                objectTypes.add(stringEncoder.encode(objectType));
                                String queryToComputeSupportForNonLiteralTypeObjects = buildQuery(classIri, property, objectType, "query9");
                                queryToComputeSupportForNonLiteralTypeObjects = setProperty(queryToComputeSupportForNonLiteralTypeObjects);
                                computeSupport(classIri, property, objectType, queryToComputeSupportForNonLiteralTypeObjects);
                            } else {
                                System.out.println("WARNING:: it's null for Non-Literal type object .. " + classIri + " - " + property);
                            }
                        });
                    }

                    String queryToGetUndefinedNonLiteralObjects = buildQuery(classIri, property, "query7_");
                    queryToGetUndefinedNonLiteralObjects = setProperty(queryToGetUndefinedNonLiteralObjects);
                    List<BindingSet> resultsUndefinedTriples = graphDBUtils.runSelectQuery(queryToGetUndefinedNonLiteralObjects);
                    if (resultsUndefinedTriples != null && resultsUndefinedTriples.size() > 0) {
                        String undefinedObjType = "http://shaclshapes.org/undefined";
                        objectTypes.add(stringEncoder.encode(undefinedObjType));
                        String queryToComputeSupportForNonLiteralTypeObjects = buildQuery(classIri, property, undefinedObjType, "query9_");
                        queryToComputeSupportForNonLiteralTypeObjects = setProperty(queryToComputeSupportForNonLiteralTypeObjects);
                        computeSupport(classIri, property, undefinedObjType, queryToComputeSupportForNonLiteralTypeObjects);
                    }
                }
                propToObjTypes.put(stringEncoder.encode(property), objectTypes);
            }
            classToPropWithObjTypes.put(classIri, propToObjTypes);
            System.out.println(".. globalComputeSupportMethodTime: " + globalComputeSupportMethodTime);

        }
        watch.stop();
        System.out.println("Time Elapsed getShapesInfoAndComputeSupport: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getShapesInfoAndComputeSupport ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }

    public void computeSupport(Integer classIri, String property, String objectType, String supportQuery) {
        StopWatch watcher = new StopWatch();
        watcher.start();
        for (BindingSet countRow : graphDBUtils.runSelectQuery(supportQuery)) {
            if (countRow.getBinding("count").getValue().isLiteral()) {
                Literal literalCount = (Literal) countRow.getBinding("count").getValue();
                int support = literalCount.intValue();
                int clasEntityCount = classEntityCount.get(classIri);
                Double confidence = ((double) support / (double) clasEntityCount);
                shapeTripletSupport.put(new Tuple3<>(classIri, stringEncoder.encode(property), stringEncoder.encode(objectType)), new SupportConfidence(support, confidence));
            }
        }
        watcher.stop();
        long time = watcher.getTime();
        globalComputeSupportMethodTime = globalComputeSupportMethodTime + time;
    }

    public void extractSHACLShapes(Boolean performPruning) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        ShapesExtractor se = new ShapesExtractor(stringEncoder, shapeTripletSupport, classEntityCount, instantiationProperty);
        //se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());

        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            StopWatch watchForPruning = new StopWatch();
            watchForPruning.start();
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    StopWatch innerWatch = new StopWatch();
                    innerWatch.start();
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                    innerWatch.stop();
                    Utils.logTime(conf + "_" + supp + "", TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()), TimeUnit.MILLISECONDS.toMinutes(innerWatch.getTime()));
                });
            });
            methodName = "extractSHACLShapes";
            watchForPruning.stop();
            Utils.logTime(methodName + "-Time.For.Pruning.Only", TimeUnit.MILLISECONDS.toSeconds(watchForPruning.getTime()), TimeUnit.MILLISECONDS.toMinutes(watchForPruning.getTime()));
        }

        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }


    public String extractSHACLShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        shapesExtractor = new ShapesExtractor(stringEncoder, shapeTripletSupport, classEntityCount, instantiationProperty);
        shapesExtractor.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return shapesExtractor.getOutputFileAddress();
    }

    /*
        Used in SHACTOR Demo - Invoked from Extraction View (with support and confidence thresholds)
     */
    public String extractSHACLShapesWithPruning(Double conf, Integer supp) {
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:WithPruning";
        StopWatch watchForPruning = new StopWatch();
        watchForPruning.start();
        shapesExtractor.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
        watchForPruning.stop();

        Utils.logTime(conf + "_" + supp + " " + methodName + "-Time.For.Pruning.Only", TimeUnit.MILLISECONDS.toSeconds(watchForPruning.getTime()), TimeUnit.MILLISECONDS.toMinutes(watchForPruning.getTime()));

        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        return shapesExtractor.getOutputFileAddress();
    }

    public void writeSupportToFile() {
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);

            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = stringEncoder.decode(tupl3._1) + "|" + stringEncoder.decode(tupl3._2) + "|" +
                        stringEncoder.decode(tupl3._3) + "|" + count + "|" + classEntityCount.get(tupl3._1);
                printWriter.println(log);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private String buildQuery(Integer classIri, String property, String queryFile) {
        return (FilesUtil.readQuery(queryFile)
                .replace(":Class", " <" + stringEncoder.decode(classIri) + "> "))
                .replace(":Prop", " <" + property + "> ");
    }

    @NotNull
    private String buildQuery(Integer classIri, String property, String objectType, String queryFile) {
        return (FilesUtil.readQuery(queryFile)
                .replace(":Class", " <" + stringEncoder.decode(classIri) + "> "))
                .replace(":Prop", " <" + property + "> ")
                .replace(":ObjectType", " <" + objectType + "> ");
    }

    @NotNull
    private HashSet<String> getPropertiesOfClass(String query) {
        HashSet<String> props = new HashSet<>();
        //This query will return a table having one column named prop: IRI of the properties of a given class
        graphDBUtils.runSelectQuery(query).forEach(result -> {
            String propIri = result.getValue("prop").stringValue();
            props.add(propIri);
        });
        return props;
    }

    private String setProperty(String query) {
        return query.replace(":instantiationProperty", instantiationProperty);
    }

    public StringEncoder getStringEncoder() {
        return stringEncoder;
    }

    public GraphDBUtils getGraphDBUtils() {
        return graphDBUtils;
    }

    public HashMap<Integer, Integer> getClassEntityCount() {
        return classEntityCount;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getClassToPropWithObjTypes() {
        return classToPropWithObjTypes;
    }

    public HashMap<Tuple3<Integer, Integer, Integer>, SupportConfidence> getShapeTripletSupport() {
        return shapeTripletSupport;
    }

    public Set<Integer> getClasses() {
        return classes;
    }

    public String getInstantiationProperty() {
        return instantiationProperty;
    }

    public Boolean getQseFromSpecificClasses() {
        return qseFromSpecificClasses;
    }

    public long getGlobalComputeSupportMethodTime() {
        return globalComputeSupportMethodTime;
    }
}


/*    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        SHACLER shacler = new SHACLER();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        System.out.println("Writing shapes into a file...");
        shacler.writeModelToFile();
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("populateShapes ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    */