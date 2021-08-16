package cs.utils;

import java.util.HashMap;

import org.semanticweb.yars.nx.Node;

public class NodeEncoder {
    int counter;
    HashMap<Integer, Node> table;
    HashMap<Node, Integer> reverseTable;
    
    public NodeEncoder() {
        this.counter = -1;
        this.table = new HashMap<>();
        this.reverseTable = new HashMap<>();
    }
    
    public int encode(Node val) {
        if (reverseTable.containsKey(val)) {
            return reverseTable.get(val);
        } else {
            this.counter++;
            table.put(counter, val);
            reverseTable.put(val, counter);
            return counter;
        }
    }
    
    public Node decode(int val) {
        return this.table.get(val);
    }
    
    public HashMap<Integer, Node> getTable() {
        return table;
    }
}
