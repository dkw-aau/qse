package extras;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class Utils {

/*    public void initBloomFilters() {
        StopWatch watch = new StopWatch();
        watch.start();
        classInstanceCount.forEach((s, integer) -> {
            System.out.println(s + " -> " + integer);
            classInstanceBloomFilters.put(s, BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), integer, 0.01));
        });
    }*/
    
/*    public void generateBFs() {
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
    }*/
    
        /*    public void initHasMap() {
        StopWatch watch = new StopWatch();
        watch.start();
        classInstanceCount.forEach((s, integer) -> {
            classProperties.put(s, new HashSet<>());
            classPropWithType.put(s, new HashMap<>());
        });
        watch.stop();
        System.out.println("Time Elapsed initHasMap: " + watch.getTime());
    }*/
    
    /*    public void generateClassInstancesHashMaps() {
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
    }*/
    
    
    /*    private HashSet<String> findObjectType(String object) {
        HashSet<String> objectTypes = new HashSet<>();
        classInstanceBloomFilters.forEach((c, bf) -> {
            // check for type of property, i.e., object
            if (bf.mightContain(object)) {
                //System.out.println("Type detected for " + object + " -> " + c);
                objectTypes.add(c);
            }
        });
        return objectTypes;
    }*/
    
}
