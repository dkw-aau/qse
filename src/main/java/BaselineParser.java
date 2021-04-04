import org.apache.commons.lang3.time.StopWatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

public class BaselineParser {
    public String rdfFile = "";
    public final String RDFtype = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    
    HashMap<String, Integer> classToInstanceCount = new HashMap<>();
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>();
    
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
                        String[] nodes = line.split(" ");
                        classToInstanceCount.put(nodes[2], (classToInstanceCount.getOrDefault(nodes[2], 0)) + 1);
    
                        //Track instances
                        if (classToInstances.containsKey(nodes[2])) {
                            classToInstances.get(nodes[2]).add(line);
        
                        } else {
                            HashSet<String> h = new HashSet<>() {{ add(line); }};
                            classToInstances.put(nodes[2], h);
                        }
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
        System.out.println(parser.classToInstanceCount.size());
        parser.classToInstances.forEach((k, v) -> {
            System.out.println(k + " " + v.size());
        });
    }
}
