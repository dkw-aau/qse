package cs.parsers.bl;

import cs.parsers.SHACLER;
import cs.utils.*;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Literal;
import org.semanticweb.yars.nx.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        this.encoder = new Encoder();
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    private void getNumberOfInstancesOfEachClass() {
        StopWatch watch = new StopWatch();
        watch.start();
        graphDBUtils.runSelectQuery(FilesUtil.readQuery("query2")).forEach(result -> { //Query will return a table having two columns class: IRI of the class, classCount: number of instances of class
            String c = result.getValue("class").stringValue();
            int classCount = 0;
            if (result.getBinding("classCount").getValue().isLiteral()) {
                Literal literalClassCount = (Literal) result.getBinding("classCount").getValue();
                classCount = literalClassCount.intValue();
            }
            classInstanceCount.put(encoder.encode(c), classCount);
        });
        watch.stop();
        System.out.println("Time Elapsed collectClassInstanceCountInfo: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void getDistinctClasses() {
        classes = classInstanceCount.keySet();
    }
    
    private void getShapesInfoAndComputeSupport() {
        StopWatch watch = new StopWatch();
        watch.start();
        for (Integer classIri : this.classes) {
            String query = FilesUtil.readQuery("query4").replace(":Class", " <" + encoder.decode(classIri) + "> ");
            HashSet<String> props = new HashSet<>();
            graphDBUtils.runSelectQuery(query).forEach(result -> {  //Query will return a table having one column prop: IRI of the properties of a given class
                String propIri = result.getValue("prop").stringValue();
                props.add(propIri);
            });
            
            HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
            for (String property : props) {
                HashSet<String> objectTypes = new HashSet<>();
                query = (FilesUtil.readQuery("query5")
                        .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                        .replace(":Prop", " <" + property + "> ");
                
                if (graphDBUtils.runAskQuery(query)) { // Literal Type Object
                    //Query to get Literal Type
                    query = (FilesUtil.readQuery("query6")
                            .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                            .replace(":Prop", " <" + property + "> ");
                    
                    graphDBUtils.runSelectQuery(query).forEach(row -> {
                        String objectType = row.getValue("objDataType").stringValue();
                        objectTypes.add(objectType);
                        
                        String supportQuery = (FilesUtil.readQuery("query8")
                                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                                .replace(":Prop", " <" + property + "> ")
                                .replace(":ObjectType", " <" + objectType + "> ");
                        
                        computeSupport(classIri, property, objectType, supportQuery);
                    });
                } else {  //query to get non-literal data type
                    query = (FilesUtil.readQuery("query7")
                            .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                            .replace(":Prop", " <" + property + "> ");
                    graphDBUtils.runSelectQuery(query).forEach(row -> {
                        String objectType = row.getValue("objDataType").stringValue();
                        objectTypes.add(objectType);
                        
                        String supportQuery = (FilesUtil.readQuery("query9")
                                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                                .replace(":Prop", " <" + property + "> ")
                                .replace(":ObjectType", " <" + objectType + "> ");
                        
                        computeSupport(classIri, property, objectType, supportQuery);
                    });
                }
                propToObjTypes.put(Utils.IriToNode(property), objectTypes);
            }
            classToPropWithObjTypes.put(encoder.decode(classIri), propToObjTypes);
        }
        watch.stop();
        System.out.println("Time Elapsed collectPropertiesWithObjectTypesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
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
    
    private void runParser() {
        getNumberOfInstancesOfEachClass();
        getDistinctClasses();
        getShapesInfoAndComputeSupport();
        populateShapes();
        writeSupportToFile();
        System.out.println("Size: classToPropWithObjTypes :: " + classToPropWithObjTypes.size() + " , Size: shapeTripletSupport :: " + shapeTripletSupport.size());
    }
    
    public void run() {
        runParser();
    }
}
