package cs.utils;

import org.semanticweb.yars.nx.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LRUCache {
    private LinkedHashMap<Node, List<Node>> cacheMap;
    
    public LRUCache(int capacity) {
        cacheMap = new LinkedHashMap<Node, List<Node>>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > capacity;
            }
        };
    }
    
    // This method works in O(1)
    public List<Node> get(Node key) {
        return cacheMap.get(key);
    }
    
    // This method works in O(1)
    public void put(Node key, List<Node> value) {
        cacheMap.put(key, value);
    }
    
    public boolean containsKey(Node node) {
        return cacheMap.get(node) != null;
    }
}