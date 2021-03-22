import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// find the classes
// find all the instances of the class
// find all the triples whose subject is one of its instances
// track instances

public class Parser {
    public String rdfFile = "";
    public HashSet<String> classesHashSet = new HashSet<>();
    public HashMap<String, Integer> classInstanceCount = new HashMap<>();
    HashMap<String, BloomFilter<String>> classInstanceBloomFilters = new HashMap<>();
    HashMap<String, HashSet<String>> classInstances = new HashMap<>();
    HashMap<String, HashSet<String>> classProperties = new HashMap<>();
    HashMap<String, HashMap<String, HashSet<String>>> classPropWithType = new HashMap<>();
    
    Parser(String filePath) {
        this.rdfFile = filePath;
    }
    
    public void parseClasses() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileInputStream rdf = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(rdf);
            nxp.iterator().forEachRemaining(nodes -> {
                //filter RDF.type triples
                if (nodes[1].toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                    classesHashSet.add(nodes[2].toString());
                    classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                }
            });
            
            System.out.println(classesHashSet.size());
            System.out.println(classesHashSet);
            //In case you want to sort the results
            //this.classInstanceCount = classInstanceCount.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            classInstanceCount.forEach((s, integer) -> {
                System.out.println(s + " -> " + integer);
            });
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        watch.stop();
        System.out.println("Time Elapsed parseClasses: " + watch.getTime());
    }
    
    public void initHasMap() {
        StopWatch watch = new StopWatch();
        watch.start();
        classInstanceCount.forEach((s, integer) -> {
            classInstances.put(s, new HashSet<>());
            classProperties.put(s, new HashSet<>());
            classPropWithType.put(s, new HashMap<>());
        });
        watch.stop();
        System.out.println("Time Elapsed initHasMap: " + watch.getTime());
        
    }
    
    public void generateClassInstancesHashMaps() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileInputStream file = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(file);
            nxp.iterator().forEachRemaining(nodes -> {
                //filter class instances
                if (classesHashSet.contains(nodes[2].toString())) {
                    HashSet<String> h = classInstances.get(nodes[2].toString());
                    h.add(nodes[0].toString());
                    classInstances.put(nodes[2].toString(), h);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed generateClassInstancesHashMaps: " + watch.getTime());
    }
    
    /**
     * This method extracts properties of each class and store in a HashSet
     * LUBM - 91 Million triples, 23 classes - 4.3 minutes on local mac
     */
    
    public void extractClassProperties() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileInputStream file = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(file);
            //HashMap<String, List<String>> instances = new HashMap<>();
            nxp.iterator().forEachRemaining(nodes -> {
                classInstances.forEach((c, instancesHashSet) -> {
                    if (instancesHashSet.contains(nodes[0].toString())) {
                        if (classPropWithType.get(c).get(nodes[1].toString()) != null) {
                            HashSet<String> temp = classPropWithType.get(c).get(nodes[1].toString());
                            temp.addAll(findObjectType(nodes[2].toString()));
                            classPropWithType.get(c).put(nodes[1].toString(), temp);
                        } else {
                            classPropWithType.get(c).put(nodes[1].toString(), findObjectType(nodes[2].toString()));
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed extractClassProperties: " + watch.getTime());
    }
    
    public void initBloomFilters() {
        StopWatch watch = new StopWatch();
        watch.start();
        classInstanceCount.forEach((s, integer) -> {
            System.out.println(s + " -> " + integer);
            classInstanceBloomFilters.put(s, BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), integer, 0.01));
        });
    }
    
    public void generateBFs() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileInputStream file = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(file);
            nxp.iterator().forEachRemaining(nodes -> {
                //filter class instances
                if (classesHashSet.contains(nodes[2].toString())) {
                    //nodes[0] is the instance
                    classInstanceBloomFilters.get(nodes[2].toString()).put(nodes[0].toString());
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed generateBFs: " + watch.getTime());
    }
    
    private HashSet<String> findObjectType(String object) {
        HashSet<String> objectTypes = new HashSet<>();
        classInstanceBloomFilters.forEach((c, bf) -> {
            // check for type of property, i.e., object
            if (bf.mightContain(object)) {
                //System.out.println("Type detected for " + object + " -> " + c);
                objectTypes.add(c);
            }
        });
        return objectTypes;
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        Parser parser = new Parser(filePath);
        parser.parseClasses();
        parser.initBloomFilters();
        parser.generateBFs();
        parser.initHasMap();
        parser.generateClassInstancesHashMaps();
        parser.extractClassProperties();
    
        System.out.println(" +++++++++++++++++++++++++++++++++++++");
        
        parser.classPropWithType.forEach( (c, p) -> {
            System.out.println("__________________________________________________________________________");
            System.out.println(c + " -> ");
            p.forEach((prop, t)-> {
                System.out.println(prop + " type: " + t);
            });
        });
        
    } // main ends here
}
