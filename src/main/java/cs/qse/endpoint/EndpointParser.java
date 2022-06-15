package cs.qse.endpoint;

import cs.utils.*;
import cs.utils.encoders.Encoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.yars.nx.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class queries and endpoint to extract SHACL shapes, compute the confidence/support for shape constraints,
 * to perform node and property shape constraints pruning based on defined threshold for confidence and support
 */
public class EndpointParser {
    private final GraphDBUtils graphDBUtils;
    HashMap<Integer, Integer> classInstanceCount;
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, Integer> shapeTripletSupport;
    Set<Integer> classes;
    Encoder encoder;
    
    public EndpointParser() {
        this.graphDBUtils = new GraphDBUtils();
        int expectedNumberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.encoder = new Encoder();
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        getNumberOfInstancesOfEachClass();
        getDistinctClasses();
        getShapesInfoAndComputeSupport();
        populateShapes();
        writeSupportToFile();
        System.out.println("Size: classToPropWithObjTypes :: " + classToPropWithObjTypes.size() + " , Size: shapeTripletSupport :: " + shapeTripletSupport.size());
    }
    
    private void getNumberOfInstancesOfEachClass() {
        StopWatch watch = new StopWatch();
        watch.start();
        //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
        graphDBUtils.runSelectQuery(FilesUtil.readQuery("query2")).forEach(result -> {
            String c = result.getValue("class").stringValue();
            int classCount = 0;
            if (result.getBinding("classCount").getValue().isLiteral()) {
                Literal literalClassCount = (Literal) result.getBinding("classCount").getValue();
                classCount = literalClassCount.intValue();
            }
            classInstanceCount.put(encoder.encode(c), classCount);
        });
        watch.stop();
        System.out.println("Time Elapsed getNumberOfInstancesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getNumberOfInstancesOfEachClass ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void getDistinctClasses() {
        classes = classInstanceCount.keySet();
    }
    
    private void getShapesInfoAndComputeSupport() {
        StopWatch watch = new StopWatch();
        watch.start();
        for (Integer classIri : this.classes) {
            String queryToGetProperties = FilesUtil.readQuery("query4").replace(":Class", " <" + encoder.decode(classIri) + "> ");
            HashSet<String> props = getPropertiesOfClass(queryToGetProperties);
            
            HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
            for (String property : props) {
                HashSet<String> objectTypes = new HashSet<>();
                String queryToVerifyLiteralObjectType = buildQuery(classIri, property, "query5");
                
                //Literal Type Object
                if (graphDBUtils.runAskQuery(queryToVerifyLiteralObjectType)) {
                    String queryToGetDataTypeOfLiteralObject = buildQuery(classIri, property, "query6");
                    List<BindingSet> results = graphDBUtils.runSelectQuery(queryToGetDataTypeOfLiteralObject);
                    if (results != null) {
                        results.forEach(row -> {
                            if (row.getValue("objDataType") != null) {
                                String objectType = row.getValue("objDataType").stringValue();
                                objectTypes.add(objectType);
                                //FIXME: It should run for all object types
                                String queryToComputeSupportForLiteralTypeObjects = buildQuery(classIri, property, objectType, "query8");
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
                    List<BindingSet> results = graphDBUtils.runSelectQuery(queryToGetDataTypeOfNonLiteralObjects);
                    if (results != null) {
                        results.forEach(row -> {
                            if (row.getValue("objDataType") != null) {
                                String objectType = row.getValue("objDataType").stringValue();
                                objectTypes.add(objectType);
                                //FIXME: It should run for all object types
                                String queryToComputeSupportForNonLiteralTypeObjects = buildQuery(classIri, property, objectType, "query9");
                                computeSupport(classIri, property, objectType, queryToComputeSupportForNonLiteralTypeObjects);
                            } else {
                                System.out.println("WARNING:: it's null for Non-Literal type object .. " + classIri + " - " + property);
                            }
                            
                        });
                    }
                }
                propToObjTypes.put(Utils.IriToNode(property), objectTypes);
            }
            classToPropWithObjTypes.put(encoder.decode(classIri), propToObjTypes);
        }
        watch.stop();
        System.out.println("Time Elapsed getShapesInfoAndComputeSupport: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getShapesInfoAndComputeSupport ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void computeSupport(Integer classIri, String property, String objectType, String supportQuery) {
        graphDBUtils.runSelectQuery(supportQuery).forEach(countRow -> {
            if (countRow.getBinding("count").getValue().isLiteral()) {
                Literal literalCount = (Literal) countRow.getBinding("count").getValue();
                int count = literalCount.intValue();
                shapeTripletSupport.put(new Tuple3<>(classIri, encoder.encode(property), encoder.encode(objectType)), count);
            }
        });
    }
    
    private void populateShapes() {
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
    
    public void writeSupportToFile() {
        System.out.println("Writing Support to File ...");
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, Integer> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" +
                        encoder.decode(tupl3._3) + "|" + count + "|" + classInstanceCount.get(tupl3._1);
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
                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                .replace(":Prop", " <" + property + "> ");
    }
    
    @NotNull
    private String buildQuery(Integer classIri, String property, String objectType, String queryFile) {
        return (FilesUtil.readQuery(queryFile)
                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
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
}
