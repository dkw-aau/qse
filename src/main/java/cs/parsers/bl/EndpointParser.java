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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EndpointParser {
    private final GraphDBUtils graphDBUtils;
    HashMap<String, HashSet<Integer>> instanceToClass;
    HashMap<Integer, Integer> classInstanceCount;
    
    HashMap<String, HashMap<Node, HashSet<String>>> classToPropWithObjTypes;
    HashMap<Tuple3<Integer, Integer, Integer>, Integer> shapeTripletSupport;
    HashSet<Integer> classes;
    Encoder encoder;
    SHACLER shacler = new SHACLER();
    
    public EndpointParser() {
        this.graphDBUtils = new GraphDBUtils();
        int nol = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines"));
        int expectedNumberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        this.instanceToClass = new HashMap<>((int) ((nol) / 0.75 + 1));
        this.classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        classes = new HashSet<>();
        this.encoder = new Encoder();
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    }
    
    private void collectAllClasses() {
        StopWatch watch = new StopWatch();
        watch.start();
        graphDBUtils.runSelectQuery(readQuery("query3")).forEach(result -> {  //Query will return a table having one column class: IRI of the class
            String classIri = result.getValue("class").stringValue();
            classes.add(encoder.encode(classIri));
        });
        watch.stop();
        System.out.println("Time Elapsed collectAllClasses: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void collectShapesAndComputeSuppport() {
        StopWatch watch = new StopWatch();
        watch.start();
        for (Integer classIri : this.classes) {
            String query = readQuery("query4").replace(":Class", " <" + encoder.decode(classIri) + "> ");
            HashSet<String> props = new HashSet<>();
            
            graphDBUtils.runSelectQuery(query).forEach(result -> {  //Query will return a table having one column prop: IRI of the properties of a given class
                String propIri = result.getValue("prop").stringValue();
                props.add(propIri);
            });
            
            HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
            for (String property : props) {
                HashSet<String> objectTypes = new HashSet<>();
                query = (readQuery("query5")
                        .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                        .replace(":Prop", " <" + property + "> ");
                
                if (graphDBUtils.runAskQuery(query)) { // Literal Type Object
                    //Query to get Literal Type
                    query = (readQuery("query6")
                            .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                            .replace(":Prop", " <" + property + "> ");
                    
                    graphDBUtils.runSelectQuery(query).forEach(row -> {
                        String objectType = row.getValue("objDataType").stringValue();
                        objectTypes.add(objectType);
                        
                        String supportQuery = (readQuery("query8")
                                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                                .replace(":Prop", " <" + property + "> ")
                                .replace(":ObjectType", " <" + objectType + "> ");
                        
                        graphDBUtils.runSelectQuery(supportQuery).forEach(countRow -> {
                            if (countRow.getBinding("count").getValue().isLiteral()) {
                                Literal literalCount = (Literal) countRow.getBinding("count").getValue();
                                int count = literalCount.intValue();
                                shapeTripletSupport.put(new Tuple3<>(classIri, encoder.encode(property), encoder.encode(objectType)), count);
                            }
                        });
                    });
                    
                } else {
                    //query to get non-literal data type
                    query = (readQuery("query7")
                            .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                            .replace(":Prop", " <" + property + "> ");
                    graphDBUtils.runSelectQuery(query).forEach(row -> {
                        String objectType = row.getValue("objDataType").stringValue();
                        objectTypes.add(objectType);
                        String supportQuery = (readQuery("query9")
                                .replace(":Class", " <" + encoder.decode(classIri) + "> "))
                                .replace(":Prop", " <" + property + "> ")
                                .replace(":ObjectType", " <" + objectType + "> ");
                        
                        graphDBUtils.runSelectQuery(supportQuery).forEach(countRow -> {
                            if (countRow.getBinding("count").getValue().isLiteral()) {
                                Literal literalCount = (Literal) countRow.getBinding("count").getValue();
                                int count = literalCount.intValue();
                                shapeTripletSupport.put(new Tuple3<>(classIri, encoder.encode(property), encoder.encode(objectType)), count);
                            }
                        });
                    });
                    
                    
                }
                propToObjTypes.put(Utils.IriToNode(property), objectTypes);
            }
            classToPropWithObjTypes.put(encoder.decode(classIri), propToObjTypes);
        }
        watch.stop();
        System.out.println("Time Elapsed collectPropertiesWithObjectTypesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    private void collectInstanceToClassInfo() {
        //Query will return a table having two columns s: Instance, o: Class of instance,
        graphDBUtils.runSelectQuery(readQuery("query1")).forEach(result -> {
            String s = result.getValue("s").stringValue();
            String o = result.getValue("o").stringValue();
            if (instanceToClass.containsKey(s)) {
                HashSet<Integer> classes = instanceToClass.get(s);
                classes.add(encoder.encode(o));
                instanceToClass.put(s, classes);
            } else {
                HashSet<Integer> classes = new HashSet<>();
                classes.add(encoder.encode(o));
                instanceToClass.put(s, classes);
            }
        });
    }
    
    public void writeSupportToFile() {
        System.out.println("Writing to File ...");
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
    
    private void collectClassInstanceCountInfo() {
        StopWatch watch = new StopWatch();
        watch.start();
        graphDBUtils.runSelectQuery(readQuery("query2")).forEach(result -> { //Query will return a table having two columns class: IRI of the class, classCount: number of instances of class
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
    
    public String readQuery(String query) {
        String q = null;
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/queries/";
            q = new String(Files.readAllBytes(Paths.get(queriesDirectory + query + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void runParser() {
        collectClassInstanceCountInfo();
        collectAllClasses();
        collectShapesAndComputeSuppport();
        populateShapes();
        shacler.writeModelToFile();
        writeSupportToFile();
        System.out.println("Size: classToPropWithObjTypes :: " + classToPropWithObjTypes.size() + " , Size: shapeTripletSupport :: " + shapeTripletSupport.size());
        /*classToPropWithObjTypes.forEach((k, v) -> {
            System.out.println(k + " :> ");
            v.forEach((p, o) -> {
                System.out.println(p + " :> " + o.toString());
            });
        });*/
    }
    
    public void run() {
        runParser();
    }
}
