package cs.qse;

import cs.utils.Tuple2;
import cs.utils.Tuple3;
import org.semanticweb.yars.nx.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsComputer {
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    HashMap<Integer, HashSet<Integer>> propToClassesHavingMaxCountGreaterThanOne = new HashMap<>();
    
    public StatsComputer(HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport) {
        this.shapeTripletSupport = shapeTripletSupport;
    }
    
    public void compute(HashMap<Node, EntityData> entityDataHashMap, HashMap<Integer, Integer> classEntityCount) {
        //Compute Support
        entityDataHashMap.forEach((instance, entityData) -> {
            HashSet<Integer> instanceClasses = entityDataHashMap.get(instance).getClassTypes();
            if (instanceClasses != null) {
                for (Integer c : entityDataHashMap.get(instance).getClassTypes()) {
                    for (Tuple2<Integer, Integer> propObjTuple : entityData.getPropertyConstraints()) {
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
            
            //identify properties having max count != 1
            List<Integer> duplicates = entityData.getProperties().stream().collect(Collectors.groupingBy(Function.identity()))
                    .entrySet().stream().filter(e -> e.getValue().size() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
            duplicates.forEach(prop -> {
                if (propToClassesHavingMaxCountGreaterThanOne.containsKey(prop)) {
                    propToClassesHavingMaxCountGreaterThanOne.get(prop).addAll(instanceClasses);
                } else {
                    propToClassesHavingMaxCountGreaterThanOne.put(prop, instanceClasses);
                }
            });
        });
        
        //Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SC> entry : this.shapeTripletSupport.entrySet()) {
            SC value = entry.getValue();
            double confidence = (double) value.getSupport() / classEntityCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
        
    }
    
    public HashMap<Integer, HashSet<Integer>> getPropToClassesHavingMaxCountGreaterThanOne() {
        return propToClassesHavingMaxCountGreaterThanOne;
    }
    
    public HashMap<Tuple3<Integer, Integer, Integer>, SC> getShapeTripletSupport() {
        return shapeTripletSupport;
    }
    
    
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
