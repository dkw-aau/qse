package cs.qse;

import cs.utils.Tuple2;
import cs.utils.Tuple3;
import org.semanticweb.yars.nx.Node;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsComputer {
    Map<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    Map<Integer, Set<Integer>> propToClassesHavingMaxCountGreaterThanOne = new HashMap<>();
    
    public StatsComputer(Map<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport) {
        this.shapeTripletSupport = shapeTripletSupport;
    }
    
    /**
     * This method is used to compute support and confidence...
     */
    public void compute(Map<Node, EntityData> entityDataHashMap, Map<Integer, Integer> classEntityCount) {
        //Compute Support
        entityDataHashMap.forEach((entity, entityData) -> {
            Set<Integer> instanceClasses = entityDataHashMap.get(entity).getClassTypes();
            if (instanceClasses != null) {
                for (Integer c : entityDataHashMap.get(entity).getClassTypes()) {
                    for (Tuple2<Integer, Integer> propObjTuple : entityData.getPropertyConstraints()) {
                        Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                        if (this.shapeTripletSupport.containsKey(tuple3)) { //todo: Optimize it
                            SC sc = this.shapeTripletSupport.get(tuple3);
                            Integer newSupp = sc.getSupport() + 1;
                            sc.setSupport(newSupp);
                            this.shapeTripletSupport.put(tuple3, sc);
                        } else {
                            this.shapeTripletSupport.put(tuple3, new SC(1));
                        }
                    }
                }
            }
            //todo , fix max count feature
            //identify properties having max count != 1
            entityData.propertyConstraintsMap.forEach((property, propertyData) -> {
                if (propertyData.count > 1) {
                    if (propToClassesHavingMaxCountGreaterThanOne.containsKey(property)) {
                        propToClassesHavingMaxCountGreaterThanOne.get(property).addAll(propertyData.objTypes);
                    } else {
                        propToClassesHavingMaxCountGreaterThanOne.put(property, instanceClasses);
                    }
                }
            });
            
            /*List<Integer> duplicates = entityData.getProperties().stream().collect(Collectors.groupingBy(Function.identity()))
                    .entrySet().stream().filter(e -> e.getValue().size() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
            duplicates.forEach(prop -> {
                if (propToClassesHavingMaxCountGreaterThanOne.containsKey(prop)) {
                    propToClassesHavingMaxCountGreaterThanOne.get(prop).addAll(instanceClasses);
                } else {
                    propToClassesHavingMaxCountGreaterThanOne.put(prop, instanceClasses);
                }
            });
            */
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SC> entry : this.shapeTripletSupport.entrySet()) {
            SC value = entry.getValue();
            double confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
        
    }
    
    public Map<Integer, Set<Integer>> getPropToClassesHavingMaxCountGreaterThanOne() {
        return propToClassesHavingMaxCountGreaterThanOne;
    }
    
    public Map<Tuple3<Integer, Integer, Integer>, SC> getShapeTripletSupport() {
        return shapeTripletSupport;
    }
    
    /**
     * Not used anymore
     */
    public void compute(Map<Node, HashSet<Tuple2<Integer, Integer>>> entityToPropertyConstraints, HashMap<Node, HashSet<Integer>> entityToClassTypes, HashMap<Integer, Integer> classEntityCount) {
        //Compute Support
        entityToPropertyConstraints.forEach((instance, propertyShapeSet) -> {
            HashSet<Integer> instanceClasses = entityToClassTypes.get(instance);
            if (instanceClasses != null) {
                for (Integer c : entityToClassTypes.get(instance)) {
                    for (Tuple2<Integer, Integer> propObjTuple : propertyShapeSet) {
                        Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                        if (this.shapeTripletSupport.containsKey(tuple3)) {
                            SC sc = this.shapeTripletSupport.get(tuple3);
                            Integer newSupp = sc.getSupport() + 1;
                            sc.setSupport(newSupp);
                            this.shapeTripletSupport.put(tuple3, sc);
                        } else {
                            this.shapeTripletSupport.put(tuple3, new SC(1));
                        }
                    }
                }
            }
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SC> entry : this.shapeTripletSupport.entrySet()) {
            SC value = entry.getValue();
            double confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
    }
    
}
