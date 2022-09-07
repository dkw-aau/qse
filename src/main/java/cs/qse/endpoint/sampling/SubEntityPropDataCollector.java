package cs.qse.endpoint.sampling;

import cs.Main;
import cs.qse.EntityData;
import cs.qse.SupportConfidence;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import cs.utils.Utils;
import cs.utils.encoders.ConcurrentEncoder;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.query.BindingSet;
import org.semanticweb.yars.nx.Node;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static cs.qse.endpoint.Utility.buildQuery;
import static cs.qse.endpoint.Utility.extractObjectType;

public class SubEntityPropDataCollector {
    private final GraphDBUtils graphDBUtils;
    Map<Integer, EntityData> subEntityDataMapContainer;
    String typePredicate;
    ConcurrentEncoder prevEncoder;
    //currEncoder;
    NodeEncoder prevNodeEncoder;
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes;
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    Map<Integer, Integer> sampledPropCount; // count of triples having predicate P across all entities in all reservoirs  |< _ , P , _ >| (the sampled entities)
    
    public SubEntityPropDataCollector(Integer expectedNumberOfClasses) {
        this.graphDBUtils = new GraphDBUtils();
        //init required maps
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.sampledPropCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        //this.currEncoder = new ConcurrentEncoder();
    }
    
    public void setFeatures(String typePredicate, ConcurrentEncoder prevEncoder, NodeEncoder prevNodeEncoder) {
        this.typePredicate = typePredicate;
        this.prevEncoder = prevEncoder;
        this.prevNodeEncoder = prevNodeEncoder;
    }
    
    
    // Which maps are updated here?
    // FIXME: What has already been encoded? Only entities, for example, <.../kashif> entity of type :Person is encoded with NodeEncoder. And :Person is encoded with Encoder.
    //  But properties of Kashif are not encoded.
    //  What about IRI object types of properties of <.../kashif> ? They might be encoded and might not be encoded. Be careful.
    public void job(Integer jobIndex, List<Integer> entities, Map<Integer, EntityData> entityDataMapContainer) {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started Job " + jobIndex + " in Thread : " + Thread.currentThread().getName());
        this.subEntityDataMapContainer = new HashMap<>();
        try {
            entities.forEach(entityID -> {
                EntityData entityData = entityDataMapContainer.get(entityID);
                subEntityDataMapContainer.put(entityID, entityData);
                Set<String> entityTypes = new HashSet<>();
                for (Integer entityTypeID : entityData.getClassTypes()) {
                    entityTypes.add(prevEncoder.decode(entityTypeID));
                }
                String entity = prevNodeEncoder.decode(entityID).getLabel();
                String query = buildQuery(entity, entityTypes, typePredicate); // query to get ?p ?o of entity
                
                // ?p ?o are binding variables
                for (BindingSet row : graphDBUtils.evaluateSelectQuery(query)) {
                    String prop = row.getValue("p").stringValue();
                    String obj = row.getValue("o").stringValue();
                    
                    int propID = prevEncoder.encode(prop);
                    
                    //String propIri = "<" + prop + ">";
                    Node objNode = Utils.IriToNode(obj);
                    String objType = extractObjectType(obj); // find out if the object is literal or IRI
                    
                    Set<Integer> objTypesIDs = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                    if (objType.equals("IRI")) {
                        EntityData currEntityData = entityDataMapContainer.get(prevNodeEncoder.getEncodedNode(objNode)); // this tries to figure out if the object is already encoded
                        if (currEntityData != null) {
                            objTypesIDs = currEntityData.getClassTypes();
                            for (Integer objTypeID : objTypesIDs) { // get classes of node2
                                prop2objTypeTuples.add(new Tuple2<>(propID, objTypeID));
                            }
                            addEntityToPropertyConstraints(prop2objTypeTuples, entityID);
                        } else {
                            objTypesIDs.add(-1);
                        }
                        
                    } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                        int objID;
                        if (prevEncoder.isEncoded(objType)) {
                            objID = prevEncoder.encode(objType); // it will return already encoded id
                        } else {
                            objID = prevEncoder.encode(objType); // it will encode and return the encoded it
                        }
                        
                        objTypesIDs.add(objID);
                        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                        addEntityToPropertyConstraints(prop2objTypeTuples, entityID);
                    }
                    
                    
                    for (Integer entityTypeID : entityData.getClassTypes()) {
                        Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.get(entityTypeID);
                        if (propToObjTypes == null) {
                            propToObjTypes = new HashMap<>();
                            classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
                        }
                        
                        Set<Integer> classObjTypes = propToObjTypes.get(propID);
                        if (classObjTypes == null) {
                            classObjTypes = new HashSet<>();
                            propToObjTypes.put(propID, classObjTypes);
                        }
                        classObjTypes.addAll(objTypesIDs);
                        propToObjTypes.put(propID, classObjTypes);
                        classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
                        
                        //Compute Support - <entityTypeID, propID, objTypesIDs
                        
                        objTypesIDs.forEach(objTypeID -> {
                            Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(entityTypeID, propID, objTypeID);
                            if (shapeTripletSupport.containsKey(tuple3)) {
                                Integer support = shapeTripletSupport.get(tuple3).getSupport();
                                support++;
                                shapeTripletSupport.put(tuple3, new SupportConfidence(support));
                            } else {
                                shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                            }
                        });
                        sampledPropCount.merge(propID, 1, Integer::sum);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime(" for Job() " + jobIndex + " ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    
    //A utility method to add property constraints of each entity in the 2nd pass
    private void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Integer subject) {
        EntityData currentEntityData = subEntityDataMapContainer.get(subject);
        if (currentEntityData == null) {
            currentEntityData = new EntityData();
        }
        //Add Property Constraint and Property cardinality
        for (Tuple2<Integer, Integer> tuple2 : prop2objTypeTuples) {
            currentEntityData.addPropertyConstraint(tuple2._1, tuple2._2);
            if (Main.extractMaxCardConstraints) {
                currentEntityData.addPropertyCardinality(tuple2._1);
            }
        }
        //Add entity data into the map
        subEntityDataMapContainer.put(subject, currentEntityData);
    }
}
