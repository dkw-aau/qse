//package extras;
//
//import com.google.common.hash.BloomFilter;
//import gr.james.sampling.ChaoSampling;
//import gr.james.sampling.WeightedRandomSamplingCollector;
//import org.apache.commons.lang3.time.StopWatch;
//import org.semanticweb.yars.nx.Node;
//import org.semanticweb.yars.nx.parser.NxParser;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class SamplingParser {
//    public String rdfFile = "";
//    public HashSet<String> classesHashSet = new HashSet<>();
//    public HashMap<String, Integer> classInstanceCount = new HashMap<>();
//    HashMap<String, HashSet<String>> classInstances = new HashMap<>();
//    HashMap<String, Double> instanceFrequency = new HashMap<>();
//    HashMap<String, HashMap<String, Double>> classInstanceWithFrequency = new HashMap<>();
//    HashMap<String, HashSet<String>> classProperties = new HashMap<>();
//    HashMap<String, ArrayList<String>> sampledClassInstances = new HashMap<>();
//    HashMap<String, HashSet<String>> instanceProps = new HashMap<>();
//
//    HashMap<String, BloomFilter<String>> classInstanceBloomFilters = new HashMap<>();
//
//    //HashMap<String, HashMap<String, HashSet<String>>> classPropWithType = new HashMap<>();
//    SamplingParser(String filePath) {
//        this.rdfFile = filePath;
//    }
//
//    public void firstPass() {
//        StopWatch watch = new StopWatch();
//        watch.start();
//        try {
//            FileInputStream rdf = new FileInputStream(rdfFile);
//            NxParser nxp = new NxParser(rdf);
//
//            while (nxp.iterator().hasNext()) {
//                Node[] nodes = nxp.next();
//
//
//                //filter RDF.type triples
//                if (nodes[1].toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
//                    classesHashSet.add(nodes[2].toString());
//                    classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
//
//                    //Track instances
//                    if (!classInstances.containsKey(nodes[2].toString())) {
//                        HashSet<String> h = new HashSet<>();
//                        h.add(nodes[0].toString());
//                        classInstances.put(nodes[2].toString(), h);
//                    } else {
//                        HashSet<String> h = classInstances.get(nodes[2].toString());
//                        h.add(nodes[0].toString());
//                        classInstances.put(nodes[2].toString(), h);
//                    }
//                }
//                // Track frequency of instances
//                if (instanceFrequency.containsKey(nodes[0].toString())) {
//                    instanceFrequency.put(nodes[0].toString(), instanceFrequency.get(nodes[0].toString()) + 1);
//                } else {
//                    instanceFrequency.put(nodes[0].toString(), 1.0);
//                }
//            }
//
//            //In case you want to sort the results
//            this.classInstanceCount = classInstanceCount.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
//
//            classInstanceCount.forEach((s, integer) -> {
//                System.out.println(s + " -> " + integer);
//            });
//            System.out.println("Class Count: " + classesHashSet.size());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        watch.stop();
//        System.out.println("Time Elapsed parseClasses: " + watch.getTime());
//    }
//
//    public void parseInstanceProps() {
//        StopWatch watch = new StopWatch();
//        watch.start();
//        try {
//            FileInputStream rdf = new FileInputStream(rdfFile);
//            NxParser nxp = new NxParser(rdf);
//
//            while (nxp.iterator().hasNext()) {
//                Node[] nodes = nxp.next();
//
//                if (instanceProps.containsKey(nodes[0].toString())) {
//                    instanceProps.get(nodes[0].toString()).add(nodes[1].toString());
//                } else {
//                    HashSet<String> prop = new HashSet<>();
//                    prop.add(nodes[1].toString());
//                    instanceProps.put(nodes[0].toString(), prop);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        watch.stop();
//        System.out.println("Time Elapsed parseInstanceProps: " + watch.getTime());
//    }
//
//    public void mapInstanceFrequency() {
//        // Map the value of class Instances to their frequency in instanceFrequency
//        StopWatch watch = new StopWatch();
//        watch.start();
//        classInstances.forEach((k, v) -> {
//            v.forEach(val -> {
//                if (instanceFrequency.containsKey(val)) {
//                    if (classInstanceWithFrequency.containsKey(k)) {
//                        HashMap<String, Double> x = classInstanceWithFrequency.get(k);
//                        x.put(val, instanceFrequency.get(val));
//                    } else {
//                        HashMap<String, Double> h = new HashMap<>();
//                        h.put(val, instanceFrequency.get(val));
//                        classInstanceWithFrequency.put(k, h);
//                    }
//                }
//            });
//        });
//
//        // sort them based on out degree in descending order
//        /* classInstanceWithFrequency.entrySet().stream().forEach(hm -> {
//            hm.setValue(hm.getValue().entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
//        });*/
//        /* classInstanceWithFrequency.entrySet().stream().forEach(hm -> {
//            System.out.println(hm.getKey() + " -> \n" + hm.getValue());
//        });*/
//        watch.stop();
//        System.out.println("Time Elapsed mapInstanceFrequency: " + watch.getTime());
//    }
//
//    public void performSampling() {
//        StopWatch watch = new StopWatch();
//        watch.start();
//        classInstanceWithFrequency.entrySet().parallelStream().forEach(hashmap -> {
//            if (hashmap.getValue().size() > 1) {
//                WeightedRandomSamplingCollector<String> collector = ChaoSampling.weightedCollector((hashmap.getValue().size() * 2) / 100, new Random());
//                ArrayList<String> sample = (ArrayList<String>) hashmap.getValue().entrySet().stream().collect(collector);
//                sampledClassInstances.put(hashmap.getKey(), sample);
//            } else {
//                System.out.println("SKIP: Sample Size for " + hashmap.getKey() + " is less than 1.");
//            }
//        });
//        watch.stop();
//        System.out.println("Time Elapsed performSampling: " + watch.getTime());
//    }
//
//
//    public static void main(String[] args) throws Exception {
//        String filePath = args[0];
//        SamplingParser parser = new SamplingParser(filePath);
//        parser.firstPass();
//        //parser.parseInstanceProps();
//        parser.mapInstanceFrequency();
//        parser.performSampling();
//
//        System.out.println("______________________________________________________________________");
//        parser.classInstances.forEach((k, v) -> {
//            System.out.println(k + " -> " + v.size());
//        });
//
//        System.out.println("______________________________________________________________________");
//        parser.sampledClassInstances.forEach((k, v) -> {
//            System.out.println(k + " -> " + v.size());
//        });
//
//        //parser.extractClassProperties();
//
//    }
//}
