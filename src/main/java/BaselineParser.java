import org.apache.commons.lang3.time.StopWatch;
import org.ehcache.sizeof.SizeOf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;

public class BaselineParser {
    public String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    public final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<String, String> instanceToClass = new HashMap<>();
    HashSet<String> properties = new HashSet<>();
    
    // Constructor
    BaselineParser(String filePath) {
        this.rdfFile = filePath;
    }
    
    BaselineParser(String filePath, String expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = Integer.parseInt(expSizeOfClasses);
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // - Parse a <subject> <predicate> <object> string
                        if (nodes[1].contains(RDFType)) {
                            //Track instances per class
                            if (classToInstances.containsKey(nodes[2])) {
                                classToInstances.get(nodes[2]).add(nodes[0]);
                            } else {
                                HashSet<String> cti = new HashSet<String>() {{ add(nodes[0]); }};
                                classToInstances.put(nodes[2], cti);
                            }
                            // Track classes per instance
                            instanceToClass.put(nodes[0], nodes[2]);
                        }
                        properties.add(nodes[1]);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // - Parse a <subject> <predicate> <object> string
                        
                        if (classToPropWithObjTypes.containsKey(instanceToClass.get(nodes[0]))) {
                            HashMap<String, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(instanceToClass.get(nodes[0]));
                            if (propToObjTypes.containsKey(nodes[1])) {
                                propToObjTypes.get(nodes[1]).add(instanceToClass.get(nodes[2]));
                            } else {
                                HashSet<String> objTypes = new HashSet<String>() {{
                                    add(instanceToClass.get(nodes[2]));
                                }};
                                propToObjTypes.put(nodes[1], objTypes);
                            }
                            if (instanceToClass.get(nodes[0]) != null) {
                                classToPropWithObjTypes.put(instanceToClass.get(nodes[0]), propToObjTypes);
                            }
                        } else {
                            HashSet<String> objTypes = new HashSet<String>() {{add(instanceToClass.get(nodes[2]));}};
                            HashMap<String, HashSet<String>> propToObjTypes = new HashMap<>();
                            propToObjTypes.put(nodes[1], objTypes);
                            if (instanceToClass.get(nodes[0]) != null) {
                                classToPropWithObjTypes.put(instanceToClass.get(nodes[0]), propToObjTypes);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public void prioritizeClasses() {
        StopWatch watch = new StopWatch();
        watch.start();
        this.classToInstances = classToInstances.entrySet()
                .stream()
                .sorted(comparingInt(e -> e.getValue().size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
        watch.stop();
        System.out.println("Time Elapsed prioritizeClasses: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        String expectedNumberOfClasses = args[1];
        BaselineParser parser = new BaselineParser(filePath, expectedNumberOfClasses);
        parser.firstPass();
        
        parser.secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + parser.classToInstances.size() + "\n\t" + "No. of distinct Properties: " + parser.properties.size());
        
        parser.prioritizeClasses();
        parser.populateShapes();
        System.out.println("*****");
        //parser.shacler.printModel();
        
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, HashSet<String>> classToInstances: " + sizeOf.deepSizeOf(parser.classToInstances));
        System.out.println("Size - Parser HashMap<String, String> instanceToClass: " + sizeOf.deepSizeOf(parser.instanceToClass));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(parser.properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(parser.classToPropWithObjTypes));
    }
}