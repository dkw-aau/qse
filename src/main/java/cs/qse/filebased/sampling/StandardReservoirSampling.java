package cs.qse.filebased.sampling;

import cs.qse.common.EntityData;
import cs.qse.common.encoders.Encoder;
import cs.qse.common.encoders.NodeEncoder;
import org.semanticweb.yars.nx.Node;

import java.util.List;
import java.util.Map;


public class StandardReservoirSampling implements ReservoirSampling {
    Map<Integer, EntityData> entityDataMapContainer;
    Map<Integer, List<Integer>> sampledEntitiesPerClass;
    NodeEncoder nodeEncoder;
    Encoder encoder;
    
    public StandardReservoirSampling(Map<Integer, EntityData> entityDataMapContainer, Map<Integer, List<Integer>> sampledEntitiesPerClass, NodeEncoder nodeEncoder, Encoder encoder) {
        this.entityDataMapContainer = entityDataMapContainer;
        this.sampledEntitiesPerClass = sampledEntitiesPerClass;
        this.nodeEncoder = nodeEncoder;
        this.encoder = encoder;
    }
    
    @Override
    public void sample(Node[] nodes) {
        int subjID = nodeEncoder.encode(nodes[0]);
        int objID = encoder.encode(nodes[2].getLabel());
        EntityData entityData = entityDataMapContainer.get(subjID); // Track classes per entity
        if (entityData == null) {
            entityData = new EntityData();
        }
        entityData.getClassTypes().add(objID);
        entityDataMapContainer.put(subjID, entityData);
        sampledEntitiesPerClass.get(objID).add(subjID);
    }
    
    //  Here one by one consider all items from (k+1)th item to nth item.
    //      a) Generate a random number from 0 to i where i is the index of the current item in stream, i.e., lineCounter.
    //      Let the generated random number is candidateIndex.
    //	    b) If candidateIndex is in range 0 to number of entities sampled for the current class (obtained from classSampledEntityReservoir)
    //	    replace the candidateNode at candidateIndex in the reservoir with current node , additionally remove the candidate node from classSampledEntityReservoir
    
    @Override
    public void replace(int candidateIndex, Node[] nodes) {
        int objID = encoder.encode(nodes[2].getLabel());
        int currSize = sampledEntitiesPerClass.get(objID).size();
        
        //Rolling the dice again by generating another random number
        //if (candidateIndex > currSize) {candidateIndex = random.nextInt(lineCounter.get());}
        
        if (candidateIndex < currSize) {
            int candidateNode = sampledEntitiesPerClass.get(objID).get(candidateIndex); // get the candidate node at the candidate index
            
            if (entityDataMapContainer.get(candidateNode) != null) {
                //Remove the candidate node from the classSampledEntityReservoir
                entityDataMapContainer.get(candidateNode).getClassTypes().forEach(obj -> {
                    if (sampledEntitiesPerClass.containsKey(obj)) {
                        sampledEntitiesPerClass.get(obj).remove(Integer.valueOf(candidateNode));
                    }
                });
                
                entityDataMapContainer.remove(candidateNode); // Remove the candidate node from the entityDataMapContainer
                boolean status = nodeEncoder.remove(candidateNode);
                if (!status)
                    System.out.println("WARNING::Failed to remove the candidateNode: " + candidateNode);
                
                //Update the reservoir and container with the current focus node
                int subjID = nodeEncoder.encode(nodes[0]); // Encode the current focus node
                EntityData entityData = entityDataMapContainer.get(subjID);
                if (entityData == null) {
                    entityData = new EntityData();
                }
                entityData.getClassTypes().add(objID);
                entityDataMapContainer.put(subjID, entityData); // Add the focus node in the reservoir
                sampledEntitiesPerClass.get(objID).add(subjID); // Update the classSampledEntityReservoir with the current focus node for current class
            } else {
                System.out.println("WARNING::It's null for candidateNode " + candidateNode);
                sampledEntitiesPerClass.forEach((k, v) -> {
                    if (v.contains(candidateNode))
                        System.out.println("Class " + k + " : " + encoder.decode(k) + " has candidate " + candidateNode);
                });
            }
        }
    }
}

