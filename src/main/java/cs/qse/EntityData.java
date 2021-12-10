package cs.qse;

import cs.utils.Tuple2;
import org.eclipse.rdf4j.query.algebra.In;

import java.util.*;

/**
 *
 */
public class EntityData {
    Set<Integer> classTypes; // O(T) number of types of this node
    Map<Integer, PropertyData> propertyConstraintsMap; // Map from PropertyID ->

//    Set<Tuple2<Integer, Integer>> propertyConstraints; // O(P*T)
//    ArrayList<Integer> properties; // can contain duplicate, so a property can appear multiple times .... Size == Degree(N)
    
    EntityData() {
        this.classTypes = new HashSet<>();
        this.propertyConstraints = new HashSet<>();
        this.properties = new ArrayList<>();
    }
    
    public Set<Integer> getClassTypes() {
        return classTypes;
    }

    // Call it in pass #3
    public Collection<Tuple2<Integer, Integer>> getPropertyConstraints() {

        List<Tuple2<Integer,Integer>> propertyConstraints = new ArrayList<>( this.propertyConstraintsMap.size()*5/3);

        for(Map.Entry<Integer, PropertyData> pds: this.propertyConstraintsMap.entrySet()){
            Tuple2<Integer,Integer> pcs;
            Integer propertyID = pds.getKey();
            PropertyData propertyData = pds.getValue();
            for(Integer classType : propertyData.classes){
                pcs = new Tuple2<>(propertyID, classType);
                propertyConstraints.add(pcs);
            }
        }


        return propertyConstraints;


    }
    
    public List<Integer> getProperties() {
        return properties;
    }



    public void addPropertyConstraint( Integer propertyID, Integer classID){

        PropertyData pd = this.propertyConstraintsMap.get(propertyID);
        if(pd == null){
            pd= new PropertyData();
            this.propertyConstraintsMap.put(propertyID,pd);
        }
        pd.classes.add(classID);
    }

    public  void addPropertyCardinality(Integer propertyID){
        PropertyData pd = this.propertyConstraintsMap.get(propertyID);
        if(pd == null){
            pd= new PropertyData();
            this.propertyConstraintsMap.put(propertyID,pd);
        }
        pd.count+=1;

    }

    public static class PropertyData {

        Set<Integer> classes = new HashSet<>(5);
        int count = 0; // number of times I've seen this property for this node


    }

}
