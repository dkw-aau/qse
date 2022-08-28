package cs.qse.endpoint;

import cs.Main;
import cs.qse.EntityData;
import cs.qse.StatsComputer;
import cs.qse.SupportConfidence;
import cs.qse.sampling.DynamicBullyReservoirSampling;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EndpointSampling {
    private final GraphDBUtils graphDBUtils;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;
    NodeEncoder nodeEncoder;
    Integer maxEntityThreshold;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Integer, EntityData> entityDataMapContainer; // Size == N For every entity (encoded as integer) we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, List<Integer>> sampledEntitiesPerClass; // Size == O(T*entityThreshold)
    Map<Integer, Integer> reservoirCapacityPerClass; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    
    Map<Integer, Integer> propCount; // real count of *all (entire graph)* triples having predicate P   // |P| =  |< _, P , _ >| in G
    Map<Integer, Integer> sampledPropCount; // count of triples having predicate P across all entities in all reservoirs  |< _ , P , _ >| (the sampled entities)
    
    public EndpointSampling(int expNoOfClasses, int expNoOfInstances, String typePredicate, Integer entitySamplingThreshold) {
        this.graphDBUtils = new GraphDBUtils();
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.sampledEntitiesPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMapContainer = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.propCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        this.sampledPropCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        this.encoder = new Encoder();
        this.nodeEncoder = new NodeEncoder();
        this.maxEntityThreshold = entitySamplingThreshold;
    }
    
    
    public void run() {
        //first pass here is to send a query to the endpoint and get all entities, parse the entities and sample using reservoir sampling
        dynamicBullyReservoirSampling();
        
        //In the 2nd pass you run query for each sampled entity to get the property metadata ...
        workOnSampledEntities();
    }
    
    private void dynamicBullyReservoirSampling() {
        System.out.println("invoked:dynamicBullyReservoirSampling()");
        StopWatch watch = new StopWatch();
        watch.start();
        String queryToGetWikiDataEntities = "PREFIX onto: <http://www.ontotext.com/>  CONSTRUCT from onto:explicit WHERE { ?s " + typePredicate + " ?o .} ";
        
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        this.reservoirCapacityPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int minEntityThreshold = 1;
        int samplingPercentage = Main.entitySamplingTargetPercentage;
        DynamicBullyReservoirSampling drs = new DynamicBullyReservoirSampling(entityDataMapContainer, sampledEntitiesPerClass, reservoirCapacityPerClass, nodeEncoder, encoder);
        try {
            graphDBUtils.runConstructQuery(queryToGetWikiDataEntities).forEach(line -> {
                try {
                    String triple = "<" + line.getSubject() + "> <" + line.getPredicate() + "> <" + line.getObject() + "> ."; // prepare triple in N3 format to avoid changing many methods using nodes of type Node
                    Node[] nodes = NxParser.parseNodes(triple); // Get [S,P,O] as Node from triple
                    
                    int objID = encoder.encode(nodes[2].getLabel());
                    sampledEntitiesPerClass.putIfAbsent(objID, new ArrayList<>(maxEntityThreshold));
                    reservoirCapacityPerClass.putIfAbsent(objID, minEntityThreshold);
                    
                    if (sampledEntitiesPerClass.get(objID).size() < reservoirCapacityPerClass.get(objID)) {
                        drs.sample(nodes);
                    } else {
                        drs.replace(random.nextInt(lineCounter.get()), nodes);
                    }
                    classEntityCount.merge(objID, 1, Integer::sum); // Get the real entity count for current class
                    drs.resizeReservoir(classEntityCount.get(objID), sampledEntitiesPerClass.get(objID).size(), maxEntityThreshold, samplingPercentage, objID);
                    
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass:dynamicBullyReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logSamplingStats("dynamicBullyReservoirSampling", samplingPercentage, minEntityThreshold, maxEntityThreshold, entityDataMapContainer.size());
    }
    
    
    private void workOnSampledEntities() {
        //get triples related to sampled entities
        for (Map.Entry<Integer, EntityData> entry : entityDataMapContainer.entrySet()) {
            Integer subjID = entry.getKey();
            EntityData entityData = entry.getValue();
            
            Set<String> entityTypes = new HashSet<>();
            for (Integer encodedClass : entityData.getClassTypes()) {
                entityTypes.add(encoder.decode(encodedClass));
            }
            String entity = nodeEncoder.decode(subjID).getLabel();
            String query = buildQuery(entity, entityTypes);
            System.out.println(query);
    
            // ?p ?o are binding variables
            for (BindingSet row : graphDBUtils.evaluateSelectQuery(query)) {
                String property = row.getValue("p").stringValue();
                String object = row.getValue("o").stringValue();
                int propID = encoder.encode(property);
                String objectType = extractObjectType(object);
                System.out.println("");
                
                
                // non-literal object
                
                
                
                // literal object
            }
    
        }
    }
    /**
     * A utility method to extract the literal object type
     *
     * @param literalIri : IRI for the literal object
     * @return String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
     */
    private String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    //helper methods
    private String buildQuery(String entity, Set<String> types) {
        StringBuilder query = new StringBuilder("PREFIX onto: <http://www.ontotext.com/> \nSELECT * from onto:explicit WHERE { \n");
        
        for (String type : types) {
            query.append("<").append(entity).append("> ").append(typePredicate).append(" <").append(type).append("> .\n");
        }
        query.append("<").append(entity).append("> ").append("?p ?o . \n }\n");
        return query.toString();
    }
    
}
