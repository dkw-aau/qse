import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.time.StopWatch;
import org.ehcache.sizeof.SizeOf;

import java.nio.charset.StandardCharsets;
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
    public final String OntologyClass = "<http://www.w3.org/2002/07/owl#Ontology>";
    
    // Classes, instances, properties
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>();
    HashMap<String, HashMap<String, String>> classToPropWithObjType = new HashMap<>();
    
    HashMap<String, String> instanceToClass = new HashMap<>();
    HashSet<String> properties = new HashSet<>();
    HashMap<String, HashSet<String>> propertyToTypes = new HashMap<>();
    
    // Bloom Filters
    BloomFilter<CharSequence> subjObjBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100_000_000, 0.01);
    BloomFilter<CharSequence> objPropTypeBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100_000_000, 0.01);
    
    // Constructor
    BaselineParser(String filePath) {
        this.rdfFile = filePath;
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
                        subjObjBloomFilter.put(nodes[0] + nodes[1]);
                        properties.add(nodes[1]);
                    });
            properties.remove(RDFType);
            classToInstances.remove(OntologyClass);
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
            properties.forEach(p -> {
                propertyToTypes.put(p, new HashSet<>());
            });
            
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // - Parse a <subject> <predicate> <object> string
                        
                        //for this triple, I want to know, given a certain predicate, having its object, what is the type of its object, either it is literal or an IRI to some class
                        propertyToTypes.get(nodes[1]).add(instanceToClass.get(nodes[2]));
                        //  "<instance> <property> <type of the object>"
                        objPropTypeBloomFilter.put(nodes[0] + nodes[1] + instanceToClass.get(nodes[2]));
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
    
    public void propsExtractor() {
        StopWatch watch = new StopWatch();
        watch.start();
        
        classToInstances.entrySet().forEach((classInstances -> {
            System.out.print("Class: " + classInstances.getKey() + ". No. of Instances: " + classInstances.getValue().size() + " ... ");
            StopWatch innerWatch = new StopWatch();
            innerWatch.start();
            
            classInstances.getValue().forEach(instance -> {
                //instance is a <subject> string, the key is its type like subj rdf:type key
                properties.forEach(p -> {
                    if (subjObjBloomFilter.mightContain(instance + p)) {
                        
                        if (classToPropWithObjType.containsKey(classInstances.getKey())) {
                            // in case the property have more than one type of objects
                            if (propertyToTypes.get(p).size() > 1) {
                                List<String> objTypes = new ArrayList<>();
                                //Check existence of property to object reference for this instance"<instance><property><type of the object>"
                                propertyToTypes.get(p).forEach(objType -> {
                                    if (objPropTypeBloomFilter.mightContain(instance + p + objType)) {
                                        objTypes.add(objType);
                                    }
                                });
                                classToPropWithObjType.get(classInstances.getKey()).put(p, objTypes.toString());
                                
                            } else {
                                //only one type
                                classToPropWithObjType.get(classInstances.getKey()).put(p, propertyToTypes.get(p).toString());
                            }
                        } else {
                            HashMap<String, String> propToType = new HashMap<>();
                            propToType.put(p, propertyToTypes.get(p).toString());
                            classToPropWithObjType.put(classInstances.getKey(), propToType);
                        }
                    }
                });
            });
            shacler.setParams(classInstances.getKey(), classToPropWithObjType.get(classInstances.getKey()));
            shacler.constructShape();
            
            System.out.print(" Time Elapsed:" + TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()) + " Sec\n");
        }));
        watch.stop();
        System.out.println("Time Elapsed propsExtractor: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        BaselineParser parser = new BaselineParser(filePath);
        parser.firstPass();
        
        //parser.secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + parser.classToInstances.size() + "\n\t" + "No. of distinct Properties: " + parser.properties.size());
        
        //parser.prioritizeClasses();
        //parser.propsExtractor();
        
        System.out.println("*****");
   /*     parser.classToPropWithObjType.forEach((k, v) -> {
            System.out.println(k + " -> ");
            v.forEach((prop, type) -> {
                System.out.println("\t " + prop + " ---->:" + type);
            });
            System.out.println();
        });*/
        //parser.shacler.printModel();
        
        
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - extras.Parser HashMap<String, HashSet<String>> classToInstances: " + sizeOf.deepSizeOf(parser.classToInstances));
        System.out.println("Size - extras.Parser HashMap<String, HashMap<String, String>> classToPropWithObjType: " + sizeOf.deepSizeOf(parser.classToPropWithObjType));
        System.out.println("Size - extras.Parser HashMap<String, String> instanceToClass: " + sizeOf.deepSizeOf(parser.instanceToClass));
        System.out.println("Size - extras.Parser HashSet<String> properties: " + sizeOf.deepSizeOf(parser.properties));
        System.out.println("Size - extras.Parser HashMap<String, HashSet<String>> propertyToTypes: " + sizeOf.deepSizeOf(parser.propertyToTypes));
        System.out.println("Size - extras.Parser BloomFilter<CharSequence> subjObjBloomFilter: " + sizeOf.deepSizeOf(parser.subjObjBloomFilter));
        System.out.println("Size - extras.Parser BloomFilter<CharSequence> objPropTypeBloomFilter: " + sizeOf.deepSizeOf(parser.objPropTypeBloomFilter));
    }
}
