import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

public class BaselineParser {
    public String rdfFile = "";
    public final String RDFtype = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>();
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
                    .filter(line -> line.contains(RDFtype))         // An intermediate operation
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // parse a <subject> <predicate> <object> string
                        
                        //Track instances
                        if (classToInstances.containsKey(nodes[2])) {
                            classToInstances.get(nodes[2]).add(line);
        
                        } else {
                            HashSet<String> h = new HashSet<String>() {{ add(line); }};
                            classToInstances.put(nodes[2], h);
                        }
                    });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + watch.getTime());
    }
   
    public void secondPass(){
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // stream : does not carry any data
                    .forEach(line -> {                              // - A terminal operation
                        String[] nodes = line.split(" ");     // parse a <subject> <predicate> <object> string
                        subjObjBloomFilter.put(nodes[0] + nodes[1]);
                        properties.add(nodes[1]);
                    });
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + watch.getTime());
    }
    
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        BaselineParser parser = new BaselineParser(filePath);
        parser.firstPass();
        
        
        System.out.println(parser.classToInstances.size());
        
        parser.classToInstances.forEach((k, v) -> {
            System.out.println(k + " " + v.size());
        });
        
        parser.secondPass();
        System.out.println(parser.properties.size());
        System.out.println(parser.properties);
    }
}
