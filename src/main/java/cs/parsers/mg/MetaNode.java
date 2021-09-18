package cs.parsers.mg;

import cs.utils.NodeEncoder;
import orestes.bloomfilter.BloomFilter;
import org.roaringbitmap.RoaringBitmap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.util.HashMap;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class MetaNode {
    List<Integer> nodes;
    Integer parentNode;
    NodeEncoder encoder;
    Integer groupId;
    Integer metaNodeId;
    Node metaNode;
    BloomFilter<String> groupBloomFilter;
    RoaringBitmap groupRoaringBitMaps;
    
    public MetaNode(Integer groupId, List<Integer> group, int focusNode, NodeEncoder encoder, HashMap<Integer, BloomFilter<String>> ctiBf) {
        this.nodes = group;
        this.parentNode = focusNode;
        this.encoder = encoder;
        this.groupId = groupId;
        createNodeForGroup();
        this.groupBloomFilter = createUnionOfBloomFilters(ctiBf);
    }
    
    public MetaNode(Integer groupId, List<Integer> group, int focusNode, NodeEncoder encoder, HashMap<Integer, RoaringBitmap> ctiRbm, boolean rbm) {
        this.nodes = group;
        this.parentNode = focusNode;
        this.encoder = encoder;
        this.groupId = groupId;
        createNodeForGroup();
        this.groupRoaringBitMaps = createUnionOfRoaringBitMaps(ctiRbm);
    }
    
    private void createNodeForGroup() {
        try {
            //An identifier consists of the localName of the parent node and the group id, e.g., Thing_1, Thing_2, Thing_3
            String groupTriple = "<http://www.schema.mg.group.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + "<http://www.schema.mg.group.root#" + iri(encoder.decode(parentNode).getLabel()).getLocalName() + "_" + groupId + "> .";
            Node[] nodes = NxParser.parseNodes(groupTriple);
            this.metaNode = nodes[2];
            this.metaNodeId = this.encoder.encode(metaNode);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    private RoaringBitmap createUnionOfRoaringBitMaps(HashMap<Integer, RoaringBitmap> bitmapHashMap) {
        RoaringBitmap rbm = null;
        for (int i = 0, nodesSize = nodes.size(); i < nodesSize; i++) {
            if (i == 0) {
                rbm = bitmapHashMap.get(i);
            } else {
                
                for (int r : bitmapHashMap.get(i)) {
                    rbm.add(r);
                }
            }
        }
        return rbm;
    }
    
    private BloomFilter<String> createUnionOfBloomFilters(HashMap<Integer, BloomFilter<String>> ctiBf) {
        BloomFilter<String> bf = null;
        for (int i = 0, nodesSize = nodes.size(); i < nodesSize; i++) {
            if (i == 0) {
                bf = ctiBf.get(i);
            } else {
                bf.union(ctiBf.get(i));
            }
        }
        return bf;
    }
    
    // Getters
    public List<Integer> getNodes() {
        return nodes;
    }
    
    public Integer getMetaNodeId() {return metaNodeId;}
    
    public BloomFilter<String> getGroupBloomFilter() {return groupBloomFilter;}
    
    public RoaringBitmap getGroupRoaringBitMaps() {return groupRoaringBitMaps;}
}
