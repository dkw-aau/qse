import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;


public class BaselineParser {
    public String rdfFile = "";
    public final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    public final String OntologyClass = "<http://www.w3.org/2002/07/owl#Ontology>";
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>();
    HashMap<String, HashSet<String>> classToProperties = new HashMap<>();
    HashSet<String> properties = new HashSet<>();
    BloomFilter<CharSequence> subjObjBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100_000_000, 0.01);
    
    BaselineParser(String filePath) {
        this.rdfFile = filePath;
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            // Java Stream is an implementation of map/filter/reduce in JDK
            Files.lines(Path.of(rdfFile))                           // stream : does not carry any data
                    .filter(line -> line.contains(RDFType))         // An intermediate operation
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // parse a <subject> <predicate> <object> string
                        
                        //Track instances
                        if (classToInstances.containsKey(nodes[2])) {
                            classToInstances.get(nodes[2]).add(nodes[0]);
                            
                        } else {
                            HashSet<String> h = new HashSet<String>() {{ add(nodes[0]); }};
                            classToInstances.put(nodes[2], h);
                        }
                    });
            
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
            Files.lines(Path.of(rdfFile))                           // stream : does not carry any data
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // parse a <subject> <predicate> <object> string
                        subjObjBloomFilter.put(nodes[0] + nodes[1]);
                        properties.add(nodes[1]);
                    });
            properties.remove(RDFType);
            
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
            System.out.println(classInstances.getKey() + "-> \n");
            
            classInstances.getValue().forEach(instance -> {
                //instance is a <subject> string, the key is its type like subj rdf:type key
                properties.forEach(p -> {
                    if (subjObjBloomFilter.mightContain(instance + p)) {
                        if (classToProperties.containsKey(classInstances.getKey())) {
                            classToProperties.get(classInstances.getKey()).add(p);
        
                        } else {
                            HashSet<String> h = new HashSet<String>() {{ add(p); }};
                            classToProperties.put(classInstances.getKey(), h);
                        }
                    }
                });
            });
        }));
        watch.stop();
        System.out.println("Time Elapsed propsExtractor: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        BaselineParser parser = new BaselineParser(filePath);
        parser.firstPass();
        parser.prioritizeClasses();
        
        System.out.println(parser.classToInstances.size());
        
        parser.classToInstances.forEach((k, v) -> {
            System.out.println(k + " " + v.size());
        });
        
        parser.secondPass();
        
        System.out.println(parser.properties.size());
        
        System.out.println(parser.properties);
        
        parser.propsExtractor();
        parser.classToProperties.forEach((k,v) -> {System.out.println(k + " -> " + v.size());});
    }
}
