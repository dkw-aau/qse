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
    
    public NodeEncoder(int counter, HashMap<Integer, Node> table, HashMap<Node, Integer> reverseTable) {
        this.counter = counter;
        this.table = table;
        this.reverseTable = reverseTable;
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
    
    public HashMap<Node, Integer> getReverseTable() {
        return reverseTable;
    }
    
    public void setCounter(int counter) {
        this.counter = counter;
    }
    
    public void setTable(HashMap<Integer, Node> table) {
        this.table = table;
    }
    
    public void setReverseTable(HashMap<Node, Integer> reverseTable) {
        this.reverseTable = reverseTable;
    }
}
