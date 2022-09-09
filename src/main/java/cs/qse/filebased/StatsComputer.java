package cs.qse.filebased;

import cs.Main;
import cs.qse.common.EntityData;
import cs.utils.Tuple2;
import cs.utils.Tuple3;
import org.semanticweb.yars.nx.Node;

import java.util.*;

/**
 * This class is used to compute stats like confidence, support, and cardinality constraints.
 */
public class StatsComputer {
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T)
    Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne; // Size O(P*T)
    Map<Integer, List<Integer>> sampledEntitiesPerClass;
    Boolean isSamplingOn = false;
    
    public StatsComputer() {this.propWithClassesHavingMaxCountOne = new HashMap<>();}
    
    /**
     * This method is used to compute support and confidence.
     * Additionally, while iterating over entities, we prepare a new Map propWithClassesHavingMaxCountOne to store all the properties (along with their classes) having max count = 1;
     */
    public void computeSupportConfidence(Map<Node, EntityData> entityDataHashMap, Map<Integer, Integer> classEntityCount) {
        //Compute Support
        entityDataHashMap.forEach((entity, entityData) -> {
            Set<Integer> instanceClasses = entityDataHashMap.get(entity).getClassTypes();
            if (instanceClasses != null) {
                for (Integer c : instanceClasses) {
                    for (Tuple2<Integer, Integer> propObjTuple : entityData.getPropertyConstraints()) {
                        Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                        SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                        if (sc == null) {
                            this.shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                        } else {
                            //SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                            Integer newSupp = sc.getSupport() + 1;
                            sc.setSupport(newSupp);
                            this.shapeTripletSupport.put(tuple3, sc);
                        }
                    }
                }
            }
            
            //here keep track of all the properties (along with their classes) having max count = 1;
            if (Main.extractMaxCardConstraints) {
                entityData.propertyConstraintsMap.forEach((property, propertyData) -> {
                    if (propertyData.count <= 1) {
                        propWithClassesHavingMaxCountOne.putIfAbsent(property, new HashSet<>());
                        assert instanceClasses != null;
                        propWithClassesHavingMaxCountOne.get(property).addAll(instanceClasses);
                    }
                });
            }
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : this.shapeTripletSupport.entrySet()) {
            SupportConfidence value = entry.getValue();
            double confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
    }
    
    /**
     * This method is used to compute support and confidence where nodes are encoded as integer
     * Additionally, while iterating over entities, we prepare a new Map propWithClassesHavingMaxCountOne to store all the properties (along with their classes) having max count = 1;
     */
    public void computeSupportConfidenceWithEncodedEntities(Map<Integer, EntityData> entityDataMapReservoir, Map<Integer, Integer> classEntityCount) {
        //Compute Support
        entityDataMapReservoir.forEach((entity, entityData) -> {
            Set<Integer> instanceClasses = entityDataMapReservoir.get(entity).getClassTypes();
            if (instanceClasses != null) {
                for (Integer c : instanceClasses) {
                    for (Tuple2<Integer, Integer> propObjTuple : entityData.getPropertyConstraints()) {
                        Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                        SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                        if (sc == null) {
                            this.shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                        } else {
                            //SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                            Integer newSupp = sc.getSupport() + 1;
                            sc.setSupport(newSupp);
                            this.shapeTripletSupport.put(tuple3, sc);
                        }
                    }
                }
            }
            
            //here keep track of all the properties (along with their classes) having max count = 1;
            if (Main.extractMaxCardConstraints) {
                entityData.propertyConstraintsMap.forEach((property, propertyData) -> {
                    if (propertyData.count <= 1) {
                        propWithClassesHavingMaxCountOne.putIfAbsent(property, new HashSet<>());
                        assert instanceClasses != null;
                        propWithClassesHavingMaxCountOne.get(property).addAll(instanceClasses);
                    }
                });
            }
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : this.shapeTripletSupport.entrySet()) {
            SupportConfidence value = entry.getValue();
            double confidence;
            if (isSamplingOn) {
                confidence = (double) value.getSupport() / sampledEntitiesPerClass.get(entry.getKey()._1).size();
            } else {
                confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            }
            value.setConfidence(confidence);
        }
    }
    
    //Setters
    public void setShapeTripletSupport(Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport) {
        this.shapeTripletSupport = shapeTripletSupport;
    }
    
    
    public void setPropWithClassesHavingMaxCountOne(Map<Integer, Set<Integer>> propWithClassesHavingMaxCountOne) {
        this.propWithClassesHavingMaxCountOne = propWithClassesHavingMaxCountOne;
    }
    
    
    //Getters
    public Map<Integer, Set<Integer>> getPropWithClassesHavingMaxCountOne() {
        return this.propWithClassesHavingMaxCountOne;
    }
    
    public Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> getShapeTripletSupport() {
        return this.shapeTripletSupport;
    }
    
    //Not used anymore
    public void computeSupportConfidence(Map<Node, HashSet<Tuple2<Integer, Integer>>> entityToPropertyConstraints, HashMap<Node, HashSet<Integer>> entityToClassTypes, HashMap<Integer, Integer> classEntityCount) {
        //Compute Support
        entityToPropertyConstraints.forEach((instance, propertyShapeSet) -> {
            HashSet<Integer> instanceClasses = entityToClassTypes.get(instance);
            if (instanceClasses != null) {
                for (Integer c : entityToClassTypes.get(instance)) {
                    for (Tuple2<Integer, Integer> propObjTuple : propertyShapeSet) {
                        Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                        if (this.shapeTripletSupport.containsKey(tuple3)) {
                            SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                            Integer newSupp = sc.getSupport() + 1;
                            sc.setSupport(newSupp);
                            this.shapeTripletSupport.put(tuple3, sc);
                        } else {
                            this.shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                        }
                    }
                }
            }
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : this.shapeTripletSupport.entrySet()) {
            SupportConfidence value = entry.getValue();
            double confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
    }
    
    public void setSampledEntityCount(Map<Integer, List<Integer>> sampledEntitiesPerClass) {
        this.sampledEntitiesPerClass = sampledEntitiesPerClass;
    }
    
    
    public void setSamplingOn(Boolean samplingOn) {
        isSamplingOn = samplingOn;
    }
}
