package cs.qse;

import cs.utils.Tuple2;
import cs.utils.Tuple3;
import org.semanticweb.yars.nx.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ComputeStatistics {
    HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport;
    
    
    public ComputeStatistics(HashMap<Tuple3<Integer, Integer, Integer>, SC> shapeTripletSupport) {
        this.shapeTripletSupport = shapeTripletSupport;
    }
    
    public void compute(Map<Node, HashSet<Tuple2<Integer, Integer>>> instance2propertyShape, HashMap<Node, HashSet<Integer>> instanceToClass, HashMap<Integer, Integer> classInstanceCount) {
        //Compute Support
        instance2propertyShape.forEach((instance, propertyShapeSet) -> {
            HashSet<Integer> instanceClasses = instanceToClass.get(instance);
            if (instanceClasses != null) {
                for (Integer c : instanceToClass.get(instance)) {
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
            double confidence = (double) value.getSupport() / classInstanceCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
    }
    
    public HashMap<Tuple3<Integer, Integer, Integer>, SC> getShapeTripletSupport() {
        return shapeTripletSupport;
    }
}
