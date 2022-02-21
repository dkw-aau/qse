package cs.qse;

import cs.Main;
import cs.qse.experiments.ExperimentsUtil;
import cs.utils.*;
import cs.utils.encoders.Encoder;
import cs.utils.tries.Trie;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MemoryTest {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    //StatsComputer statsComputer;
    String typePredicate;
    Map<Integer, EntityData> entityDataHashMap; // Size == N For every entity we save a number of summary information
    Trie trie;
    Map<Integer, Integer> classEntityCount; // Size == T
    //Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    //Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    
    public MemoryTest(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        //this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.encoder = new Encoder();
        this.trie = new Trie();
    }
    
    public void run() {
        System.out.println("Memory Test");
        runParser();
    }
    
    private void runParser() {
        firstPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classEntityCount.size());
    }
    
    public void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        // Track classes per entity
                        int objID = encoder.encode(nodes[2].getLabel());
                        int subjID = trie.encode(nodes[0].getLabel());
                        EntityData entityData = entityDataHashMap.get(subjID);
                        if (entityData == null) {
                            entityData = new EntityData();
                        }
                        entityData.getClassTypes().add(objID);
                        entityDataHashMap.put(subjID, entityData);
                        classEntityCount.merge(objID, 1, Integer::sum);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}
