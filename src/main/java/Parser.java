import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import gr.james.sampling.ChaoSampling;
import gr.james.sampling.WeightedRandomSamplingCollector;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    
    HashMap<String, Double> instanceFrequency = new HashMap<>();
    HashMap<String, HashMap<String, Double>> classInstanceWithFrequency = new HashMap<>();
    HashMap<String, HashMap<String, HashSet<String>>> classPropWithType = new HashMap<>();
    
    HashMap<String, ArrayList<String>> sampledClassInstances = new HashMap<>();
    
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
                    
                    //Track instances
                    if (!classInstances.containsKey(nodes[2].toString())) {
                        HashSet<String> h = new HashSet<>();
                        h.add(nodes[0].toString());
                        classInstances.put(nodes[2].toString(), h);
                    } else {
                        HashSet<String> h = classInstances.get(nodes[2].toString());
                        h.add(nodes[0].toString());
                        classInstances.put(nodes[2].toString(), h);
                    }
                }
                // Track frequency of instances
                if (instanceFrequency.containsKey(nodes[0].toString())) {
                    instanceFrequency.put(nodes[0].toString(), instanceFrequency.get(nodes[0].toString()) + 1);
                } else {
                    instanceFrequency.put(nodes[0].toString(), 1.0);
                }
            });
            
            //In case you want to sort the results
            this.classInstanceCount = classInstanceCount.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            //this.instanceFrequency = instanceFrequency.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            
            classInstanceCount.forEach((s, integer) -> {
                System.out.println(s + " -> " + integer);
            });
            System.out.println("Size: ");
            System.out.println(classesHashSet.size());
            //System.out.println(classesHashSet);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        watch.stop();
        System.out.println("Time Elapsed parseClasses: " + watch.getTime());
    }
    
    public void mapInstanceFrequency() {
        // Map the value of class Instances to their frequency in instanceFrequency
        StopWatch watch = new StopWatch();
        watch.start();
        classInstances.forEach((k, v) -> {
            v.forEach(val -> {
                if (instanceFrequency.containsKey(val)) {
                    if (classInstanceWithFrequency.containsKey(k)) {
                        HashMap<String, Double> x = classInstanceWithFrequency.get(k);
                        x.put(val, instanceFrequency.get(val));
                    } else {
                        HashMap<String, Double> h = new HashMap<>();
                        h.put(val, instanceFrequency.get(val));
                        classInstanceWithFrequency.put(k, h);
                    }
                }
            });
        });
        watch.stop();
        System.out.println("Time Elapsed mapInstanceFrequency: " + watch.getTime());
    }
    
    public void performSampling() {
        classInstanceWithFrequency.forEach( (k, v) -> {
            WeightedRandomSamplingCollector<String> collector = ChaoSampling.weightedCollector( (v.size() * 20)/100, new Random());
            ArrayList<String> sample = (ArrayList<String>) instanceFrequency.entrySet().stream().collect(collector);
            sampledClassInstances.put(k, sample);
        });
    }
    
    public void initHasMap() {
        StopWatch watch = new StopWatch();
        watch.start();
        classInstanceCount.forEach((s, integer) -> {
            //classInstances.put(s, new HashSet<>());
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
        int counter = 0;
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
        parser.mapInstanceFrequency();
        parser.performSampling();
        
        System.out.println("______________________________________________________________________");
        parser.classInstances.forEach((k,v) -> {
            System.out.println(k + " -> " + v.size());
        });
        
        System.out.println("______________________________________________________________________");
        parser.sampledClassInstances.forEach((k,v) -> {
            System.out.println(k + " -> " + v.size());
        });
        
        
    } // main ends here
}
