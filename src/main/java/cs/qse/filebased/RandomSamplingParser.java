package cs.qse.filebased;

import cs.Main;
import cs.qse.common.EntityData;
import cs.utils.Tuple2;
import cs.utils.Utils;
import org.apache.commons.lang3.time.StopWatch;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RandomSamplingParser extends Parser {
    public int randomSamplingThreshold; // Random sampling threshold like 10 means: 10%
    Map<Integer, Integer> sampledClassEntityCount; // Size == T (number of distinct types) having count of entities sampled randomly based on defined threshold
    
    public RandomSamplingParser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate, int entitySamplingThreshold) {
        super(filePath, expNoOfClasses, expNoOfInstances, typePredicate);
        this.sampledClassEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.randomSamplingThreshold = entitySamplingThreshold;
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        System.out.println("Entity Sampling Threshold : " + randomSamplingThreshold);
        entityExtraction();
        entityConstraintsExtraction();
        computeSupportConfidence();
        extractSHACLShapes(false, Main.qseFromSpecificClasses);
        //assignCardinalityConstraints();
    }
    
    @Override
    public void entityExtraction() {
        StopWatch watch = new StopWatch();
        watch.start();
        Random random = new Random(100);
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        
                        int randomNumber = random.nextInt(100); //The nextInt(int n) is used to get a random number between 0 (inclusive) and the number passed in this argument(n), exclusive.
                        int objID = stringEncoder.encode(nodes[2].getLabel());
                        //If the generated random number is less than randomSamplingThreshold, then we sample the entity
                        if (randomNumber < randomSamplingThreshold) {
                            // Track classes per entity
                            EntityData entityData = entityDataHashMap.get(nodes[0]);
                            if (entityData == null) {
                                entityData = new EntityData();
                            }
                            entityData.getClassTypes().add(objID);
                            entityDataHashMap.put(nodes[0], entityData);
                            sampledClassEntityCount.merge(objID, 1, Integer::sum);
                        }
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
        Utils.logTime("firstPass:RandomSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        //logging some statistics
        int sum = classEntityCount.values().stream().reduce(0, Integer::sum);
        int sumSampled = sampledClassEntityCount.values().stream().reduce(0, Integer::sum);
        System.out.println("No. of Classes: Total: " + NumberFormat.getInstance().format(classEntityCount.size()));
        System.out.println("No. of Classes Sampled: " + NumberFormat.getInstance().format(sampledClassEntityCount.size()));
        System.out.println("Sum of Entities: " + NumberFormat.getInstance().format(sum) + " \n Sum of Sampled Entities : " + NumberFormat.getInstance().format(sumSampled));
    }
    
    @Override
    public void entityConstraintsExtraction() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).filter(line -> !line.contains(typePredicate)).forEach(line -> {
                try {
                    //Declaring required sets
                    Set<Integer> objTypes = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                    Node subject = nodes[0];
                    // if the entity is sampled, we go for it
                    if (entityDataHashMap.get(subject) != null) {
                        String objectType = extractObjectType(nodes[2].toString());
                        int propID = stringEncoder.encode(nodes[1].getLabel());
                        if (objectType.equals("IRI")) { // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                            EntityData currEntityData = entityDataHashMap.get(nodes[2]);
                            if (currEntityData != null) {
                                objTypes = currEntityData.getClassTypes();
                                for (Integer node : objTypes) { // get classes of node2
                                    prop2objTypeTuples.add(new Tuple2<>(propID, node));
                                }
                                addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                            }
                            /*else { // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*/
                            
                        } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                            int objID = stringEncoder.encode(objectType);
                            //objTypes = Collections.singleton(objID); Removed because the set throws an UnsupportedOperationException if modification operation (add) is performed on it later in the loop
                            objTypes.add(objID);
                            prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                        }
                        
                        EntityData entityData = entityDataHashMap.get(subject);
                        if (entityData != null) {
                            for (Integer entityClass : entityData.getClassTypes()) {
                                Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.get(entityClass);
                                if (propToObjTypes == null) {
                                    propToObjTypes = new HashMap<>();
                                    classToPropWithObjTypes.put(entityClass, propToObjTypes);
                                }
                                
                                Set<Integer> classObjTypes = propToObjTypes.get(propID);
                                if (classObjTypes == null) {
                                    classObjTypes = new HashSet<>();
                                    propToObjTypes.put(propID, classObjTypes);
                                }
                                
                                classObjTypes.addAll(objTypes);
                            }
                        }
                    } // if condition ends here
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass:RandomSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}
