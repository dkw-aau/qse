import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.util.NxUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

public class BaselineParser {
    public String rdfFile = "";
    public final Integer expectedNumberOfClasses = 25;
    public final String RDFtype = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    
    //HashMaps declaration
    HashMap<String, Integer> classToInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor https://sites.google.com/site/markussprunck/blog-1/howtoinitializeajavahashmapwithreasonablevalues
    //HashMap<String, HashSet<String>> classToInstances = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<String, HashSet<String>> classToInstances = new HashMap<>();
    
    BaselineParser(String filePath) {
        this.rdfFile = filePath;
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Stream<String> lines = Files.lines(Path.of(rdfFile));
            lines.forEach(s -> {
                String[] nodes = s.split(" ");
                
                if (nodes[1].equals(RDFtype)) {
                    classToInstanceCount.put(nodes[2], (classToInstanceCount.getOrDefault(nodes[2], 0)) + 1);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + watch.getTime());
    }
    
    
    public void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            
            Files.lines(Path.of(rdfFile))
                    .parallel()
                    .filter(line -> line.contains(RDFtype))
                    .forEach(line -> {
                        //System.out.println(line);
                        String[] nodes = line.split(" ");
                        
                        //Track instances
                        if (classToInstances.containsKey(nodes[2])) {
                            HashSet<String> h = classToInstances.get(nodes[2]);
                            h.add(nodes[0]);
                            classToInstances.put(nodes[2], h);
                        } else {
                            HashSet<String> h = new HashSet<String>() {{ add(nodes[0]); }};
                            classToInstances.put(nodes[2], h);
                        }
                    });
            
          /*  Stream<String> lines = Files.lines(Path.of(rdfFile));
            lines.forEach(s -> {
                String[] nodes = s.split(" ");
                
                //Track instances
                if (classToInstances.containsKey(nodes[2])) {
                    HashSet<String> h = classToInstances.get(nodes[2]);
                    h.add(nodes[0]);
                    classToInstances.put(nodes[2], h);
                } else {
                    HashSet<String> h = new HashSet<String>() {{ add(nodes[0]); }};
                    classToInstances.put(nodes[2], h);
                }
                
            });*/
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + watch.getTime());
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        BaselineParser parser = new BaselineParser(filePath);
        parser.firstPass();
        System.out.println(parser.classToInstanceCount.size());
        System.gc();
        parser.secondPass();
        parser.classToInstances.forEach((k, v) -> {
            System.out.println(k + " " + v.size());
        });
        System.gc();
        
    }
}
