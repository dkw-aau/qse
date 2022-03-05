package cs.qse.sampling;

import cs.qse.EntityData;
import cs.utils.Utils;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import org.semanticweb.yars.nx.Node;

import java.util.*;

public class DynamicBullyReservoirSampling implements ReservoirSampling {
    Map<Integer, EntityData> entityDataMapContainer;
    Map<Integer, List<Integer>> sampledEntitiesPerClass;
    Map<Integer, Integer> reservoirCapacityPerClass;
    NodeEncoder nodeEncoder;
    Encoder encoder;
    
    public DynamicBullyReservoirSampling(Map<Integer, EntityData> entityDataMapContainer, Map<Integer, List<Integer>> sampledEntitiesPerClass, Map<Integer, Integer> reservoirCapacityPerClass, NodeEncoder nodeEncoder, Encoder encoder) {
        this.entityDataMapContainer = entityDataMapContainer;
        this.sampledEntitiesPerClass = sampledEntitiesPerClass;
        this.reservoirCapacityPerClass = reservoirCapacityPerClass;
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
    
    @Override
    public void replace(int candidateIndex, Node[] nodes) {
        int objID = encoder.encode(nodes[2].getLabel());
        int currSize = sampledEntitiesPerClass.get(objID).size();
        if (candidateIndex < currSize) {
            int candidateNodeLeft = -1, candidateNodeRight = -1, scopeCandidateNodeLeft = 999999999, scopeCandidateNodeRight = 999999999;
            
            if (candidateIndex != 0) {
                candidateNodeLeft = sampledEntitiesPerClass.get(objID).get(candidateIndex - 1); // get the candidate node at the left of candidate index
                if (entityDataMapContainer.get(candidateNodeLeft) != null) {
                    scopeCandidateNodeLeft = entityDataMapContainer.get(candidateNodeLeft).getClassTypes().size();
                }
            }
            if (candidateIndex != currSize - 1) {
                candidateNodeRight = sampledEntitiesPerClass.get(objID).get(candidateIndex + 1); // get the candidate node at the right of candidate index
                if (entityDataMapContainer.get(candidateNodeRight) != null) {
                    scopeCandidateNodeRight = entityDataMapContainer.get(candidateNodeRight).getClassTypes().size();
                }
            }
            
            int candidateNode = sampledEntitiesPerClass.get(objID).get(candidateIndex); // get the candidate node at the candidate index
            
            if (entityDataMapContainer.get(candidateNode) != null) {
                int scopeCandidateNode = entityDataMapContainer.get(candidateNode).getClassTypes().size();
                
                BinaryNode node = new BinaryNode(candidateNode, scopeCandidateNode);
                node.left = new BinaryNode(candidateNodeLeft, scopeCandidateNodeLeft);
                node.right = new BinaryNode(candidateNodeRight, scopeCandidateNodeRight);
                
                BinaryNode min = Utils.getNodeWithMinimumScope(node, node.left, node.right);
                //System.out.println(min.id + " - " + min.scope);
                candidateNode = min.id;
                
                //Remove the candidate node from the classSampledEntityReservoir
                for (Integer obj : entityDataMapContainer.get(candidateNode).getClassTypes()) {
                    if (sampledEntitiesPerClass.containsKey(obj)) {
                        sampledEntitiesPerClass.get(obj).remove(Integer.valueOf(candidateNode));
                    }
                }
                
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
                for (Map.Entry<Integer, List<Integer>> entry : sampledEntitiesPerClass.entrySet()) {
                    Integer k = entry.getKey();
                    List<Integer> v = entry.getValue();
                    if (v.contains(candidateNode))
                        System.out.println("Class " + k + " : " + encoder.decode(k) + " has candidate " + candidateNode);
                }
            }
        }
    }
    
    public void resizeReservoir(int entitiesSeen, int entitiesInReservoir, Integer maxEntityThreshold, Integer targetSamplingPercentage, int objID) {
        //A
        //double newCapacityA = entitiesInReservoir * (double) (entitiesInReservoir / entitiesSeen) + entitiesInReservoir;
        //B
        //double newCapacityB = targetSamplingPercentage * entitiesInReservoir;
        //C
        //double newCapacityC = Utils.logWithBase2(entitiesSeen);
        //D
        double newCapacityD = ((targetSamplingPercentage*entitiesSeen) - entitiesInReservoir) + 1;
        
        double currentRatio = ((double) entitiesInReservoir / (double) entitiesSeen) * 100;
        if ((int) newCapacityD < maxEntityThreshold && currentRatio <= targetSamplingPercentage) {
            reservoirCapacityPerClass.put(objID, (int) newCapacityD);
        }
    }
}

