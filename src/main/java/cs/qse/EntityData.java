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
        this.classTypes = new HashSet<>(1000); //FIXME: Decide about the initial size limit
        this.propertyConstraints = new HashSet<>(1000);
        this.properties = new ArrayList<>(1000);
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
