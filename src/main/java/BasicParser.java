import com.google.common.hash.BloomFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BasicParser {
    public String rdfFile = "";
    /*
    public HashSet<String> classesHashSet = new HashSet<>();
    public HashMap<String, Integer> classInstanceCount = new HashMap<>();
    HashMap<String, HashSet<String>> classInstances = new HashMap<>();
    HashMap<String, Double> instanceFrequency = new HashMap<>();
    */
    
    BasicParser(String filePath) {
        this.rdfFile = filePath;
    }
    
    public void singlePassIterator() {
        StopWatch watch = new StopWatch();
        
        try {
            FileInputStream rdf = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(rdf);
            Iterator<Node[]> iterator = nxp.iterator();
            watch.start();
            while (iterator.hasNext()) {
                Node[] nodes = nxp.next();
                
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed singlePassIterator: " + watch.getTime());
    }
    
    /**
     * convert iterator to stream - not proven to be efficient: Java Heap Space issue on Server
     */
    public void singlePassIteratorStreamer() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileInputStream rdf = new FileInputStream(rdfFile);
            NxParser nxp = new NxParser(rdf);
            
            Iterator<Node[]> iterator = nxp.iterator();
            
            Stream<Node[]> stream = StreamSupport.stream(nxp.spliterator(), true);
            System.out.println(StreamSupport.stream(nxp.spliterator(), true).count());
            stream.parallel().forEach(System.out::println);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed singlePassIteratorStreamer: " + watch.getTime());
    }
    
    public void singlePassNioStreamFileReaderParallel() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            try (Stream<String> lines = Files.lines(Path.of(rdfFile)).parallel()) {
                lines.forEach(s -> {
                    try {
                        Node[] x = NxParser.parseNodes(s);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed singlePassNioFileReaderParallel: " + watch.getTime());
    }
    
    public void singlePassNioStreamFileReader() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Stream<String> lines = Files.lines(Path.of(rdfFile));
            lines.forEach(s -> {
                try {
                    Node[] x = NxParser.parseNodes(s);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed singlePassNioFileReader: " + watch.getTime());
    }
    
    public static void main(String[] args) throws Exception {
        String filePath = args[0];
        
        BasicParser parser = new BasicParser(filePath);
        
        for (int i = 0; i < 10; i++) {
            System.out.println("Iteration " + i);
            parser.singlePassIterator();
            System.gc();
            parser.singlePassNioStreamFileReader();
            System.gc();
            parser.singlePassNioStreamFileReaderParallel();
            System.gc();
        }
    }
}
