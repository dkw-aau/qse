package cs.parsers;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import cs.extras.RDFVault;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.ehcache.sizeof.SizeOf;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BaselineWithRdfVault {
    String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<Node, HashMap<Node, Integer>> classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    //HashMap<Node, List<Node>> instanceToClass = new HashMap<>((int) (1000000 / 0.75 + 1));
    HashMap<Node, List<Node>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    
    RDFVault rdfVault = new RDFVault();
    HashMap<Object, Object> hashMap = new HashMap<>();
    NavigableMap<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.forString());
    
    
    // Constructor
    BaselineWithRdfVault(String filePath) {
        this.rdfFile = filePath;
    }
    
    

    
    public BaselineWithRdfVault(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> line.contains(RDFType))
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                            // Track classes per instance
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).add(nodes[2]);
                            } else {
                                List<Node> list = new ArrayList<>();
                                list.add(nodes[2]);
                                instanceToClass.put(nodes[0], list);
                            }
                            
                            if(hashMap.containsKey(rdfVault.encode(nodes[0].toString()))){
                                String x = rdfVault.decode(hashMap.get(rdfVault.encode(nodes[0].toString())));
                                String x1 = x.concat(",");
                                String x2 = x1.concat(nodes[2].toString());
                                hashMap.replace(rdfVault.encode(nodes[0].toString()),rdfVault.encode(x2));
                                
                            } else {
                                hashMap.put(rdfVault.encode(nodes[0].toString()), rdfVault.encode(nodes[2].toString()));
                            }
                            
                            if(art.containsKey(nodes[0].getLabel())){
                                art.replace(art.get(nodes[0].getLabel()),art.get(nodes[0].getLabel()) + "," + nodes[2].getLabel());
                            } else {
                                art.put(nodes[0].getLabel(), nodes[2].getLabel());
                            }
                            
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                instanceToClass.get(nodes[0]).forEach(c -> {
                                    if (classToPropWithObjTypes.containsKey(c)) {
                                        HashMap<Node, HashSet<String>> propToObjTypes = classToPropWithObjTypes.get(c);
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (instanceToClass.containsKey(nodes[2])) // object is an instance of some class e.g., :Paris is an instance of :City.
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else {
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        }
                                        if (propToObjTypes.containsKey(nodes[1]))
                                            propToObjTypes.get(nodes[1]).addAll(objTypes);
                                        else {
                                            propToObjTypes.put(nodes[1], objTypes);
                                        }
                                        classToPropWithObjTypes.put(c, propToObjTypes);
                                        
                                        HashMap<Node, Integer> propToCount = classToPropWithCount.get(c);
                                        if(propToCount.containsKey(nodes[1])){
                                            propToCount.replace(nodes[1], propToCount.get(nodes[1]) + 1);
                                        } else {
                                            propToCount.put(nodes[1],1);
                                        }
                                        classToPropWithCount.put(c, propToCount);
                                        
                                    } else {
                                        HashSet<String> objTypes = new HashSet<String>();
                                        if (instanceToClass.containsKey(nodes[2]))  // object is an instance of some class e.g., :Paris is an instance of :City.
                                            instanceToClass.get(nodes[2]).forEach(node -> {
                                                objTypes.add(node.toString());
                                            });
                                        else {
                                            objTypes.add(getType(nodes[2].toString())); // Object is literal https://www.w3.org/TR/turtle/#abbrev
                                        }
                                        HashMap<Node, HashSet<String>> propToObjTypes = new HashMap<>();
                                        propToObjTypes.put(nodes[1], objTypes);
                                        
                                        
                                        //Add Count of Props
                                        HashMap<Node, Integer> propToCount = new HashMap<>();
                                        propToCount.put(nodes[1],1);
                                        
                                        instanceToClass.get(nodes[0]).forEach(cl -> {
                                            classToPropWithObjTypes.put(cl, propToObjTypes);
                                            classToPropWithCount.put(cl, propToCount);
                                        });
                                    }
                                });
                            }
                            properties.add(nodes[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private String getType(String value) {
        String theType = "<http://www.w3.org/2001/XMLSchema#string>"; //default type is XSD:string
        
        if (value.contains("\"^^")) {
            //value.split("\\^\\^")[0] is the literal value, and value.split("\\^\\^")[1]  the type of the value ;
            if (value.split("\\^\\^").length > 1) {
                theType = value.split("\\^\\^")[1];
            }
        } else if (value.contains("\"@")) {
            //value.split("\"@")[0] is the literal value and value.split("\"@")[1] is the language tag
            theType = RDF.LANGSTRING.toString();  //rdf:langString
        }
        return theType;
    }
    
    private void runParser() {
        firstPass();
        //secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        //populateShapes();
        //shacler.writeModelToFile();
        /*hashMap.forEach((k,v) -> {
            System.out.println(rdfVault.decode(k) + " -> " + rdfVault.decode(v));
        });*/
    }
    
    private void measureMemoryUsage() {
        SizeOf sizeOf = SizeOf.newInstance();
        System.out.println("Size - Parser HashMap<String, Integer> classInstanceCount: " + sizeOf.deepSizeOf(classInstanceCount));
        System.out.println("Size - Parser HashMap<Node, List<Node>> instanceToClass: " + sizeOf.deepSizeOf(instanceToClass));
        System.out.println("Size - Parser hashMap: " + sizeOf.deepSizeOf(hashMap));
        System.out.println("Size - Parser art: " + sizeOf.deepSizeOf(art));
        System.out.println("Size - Parser HashSet<String> properties: " + sizeOf.deepSizeOf(properties));
        System.out.println("Size - Parser HashMap<String, HashMap<String, HashSet<String>>> classToPropWithObjTypes: " + sizeOf.deepSizeOf(classToPropWithObjTypes));
    }
    
    public void run() {
        runParser();
        measureMemoryUsage();
    }
}