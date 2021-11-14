package cs.qse;

import cs.utils.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EntityData {
    HashSet<Integer> classTypes;
    HashSet<Tuple2<Integer, Integer>> propertyConstraints;
    ArrayList<Integer> properties;
    
    EntityData() {
        this.classTypes = new HashSet<>();
        this.propertyConstraints = new HashSet<>();
        this.properties = new ArrayList<>();
    }
    
    public HashSet<Integer> getClassTypes() {
        return classTypes;
    }
    
    public HashSet<Tuple2<Integer, Integer>> getPropertyConstraints() {
        return propertyConstraints;
    }
    
    public List<Integer> getProperties() {
        return properties;
    }
}
