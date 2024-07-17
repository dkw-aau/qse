package cs.qse.common;

import cs.Main;
import cs.utils.Tuple2;

import java.util.*;

/**
 *
 */
public class EntityData {
    public Set<Integer> classTypes; // O(T) number of types of this node
    public Map<Integer, PropertyData> propertyConstraintsMap; // Map from PropertyID -> PropertyData which consists of property's object types and count
    
    public EntityData() {
        this.classTypes = new HashSet<>();
        this.propertyConstraintsMap = new HashMap<>();
    }
    
    public Set<Integer> getClassTypes() {
        return classTypes;
    }
    
    // Called in StatsComputer.compute() method to compute support and confidence
    public Collection<Tuple2<Integer, Integer>> getPropertyConstraints() {
        List<Tuple2<Integer, Integer>> propertyConstraints = new ArrayList<>(this.propertyConstraintsMap.size() * 5 / 3);
        for (Map.Entry<Integer, PropertyData> pds : this.propertyConstraintsMap.entrySet()) {
            Tuple2<Integer, Integer> pcs;
            Integer propertyID = pds.getKey();
            PropertyData propertyData = pds.getValue();
            for (Integer classType : propertyData.objTypes) {
                pcs = new Tuple2<>(propertyID, classType);
                propertyConstraints.add(pcs);
            }
        }
        return propertyConstraints;
    }
    
    public void addPropertyConstraint(Integer propertyID, Integer classID) {
        PropertyData pd = this.propertyConstraintsMap.get(propertyID);
        if (pd == null) {
            pd = new PropertyData();
            this.propertyConstraintsMap.put(propertyID, pd);
        }
        pd.objTypes.add(classID);

        if(Main.saveCountInPropertyData) {
            pd.objTypesCount.put(classID, pd.objTypesCount.getOrDefault(classID, 0) + 1);
        }
    }
    
    public void addPropertyCardinality(Integer propertyID) {
        PropertyData pd = this.propertyConstraintsMap.get(propertyID);
        if (pd == null) {
            pd = new PropertyData();
            this.propertyConstraintsMap.put(propertyID, pd);
        }
        pd.count += 1;
    }

    /**
     * PropertyData Class
     */
    public static class PropertyData {
        public Set<Integer> objTypes = new HashSet<>(5); // these are object types
        public int count = 0; // number of times I've seen this property for this node
        public HashMap<Integer, Integer> objTypesCount = new HashMap<>(5); //saves objType and corresponding count
    }
}
