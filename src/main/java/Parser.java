import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
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
    
    Parser(String filePath){
        this.rdfFile = filePath;
    }
    
    public void parseClasses() {
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
            //this.classInstanceCount = classInstanceCount.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            classInstanceCount.forEach((s, integer) -> {
                System.out.println(s + " -> " + integer);
            });
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void initBloomFilters() {
        classInstanceCount.forEach((s, integer) -> {
            System.out.println(s + " -> " + integer);
            classInstanceBloomFilters.put(s, BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), integer, 0.01));
        });
        
    }
    
    public void generateBFs() {
        try {
            FileInputStream file = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(file);
            //HashMap<String, List<String>> instances = new HashMap<>();
            nxp.iterator().forEachRemaining(nodes -> {
                //filter class instances
                if (classesHashSet.contains(nodes[2].toString())) {
                    //System.out.println(nodes[0] + " " + nodes[1] + " " + nodes[2]);
                    //classInstanceCount.put(nodes[2].toString(), classInstanceCount.get(nodes[2].toString()) + 1);
                    //nodes[0] is the instance
                    //instances.computeIfAbsent(nodes[2].toString(), k -> new ArrayList<>()).add(nodes[0].toString());
                    classInstanceBloomFilters.get(nodes[2].toString()).put(nodes[0].toString());
                }
            });
            
            /*classInstanceCount.forEach((s, integer) -> {
                System.out.println(s + " -> " + integer);
            });*/
            
            //System.out.println(instances.get("<http://swat.cse.lehigh.edu/onto/univ-bench.owl#AssociateProfessor>"));
            //serializeHashMapToFIle(instances, "classInstances");
            //serializeHashMapToFIle(classInstanceCount, "classInstanceCount");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    
    
    private void serializeHashMapToFIle(HashMap hashMap, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName + ".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hashMap);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        Parser parser = new Parser(filePath);
        parser.parseClasses();
        parser.initBloomFilters();
        parser.generateBFs();
    } // main ends here
}
