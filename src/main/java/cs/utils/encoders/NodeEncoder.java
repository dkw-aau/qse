package cs.utils.encoders;

import java.util.HashMap;

import org.semanticweb.yars.nx.Node;

/**
 * This class encodes the Node (org.semanticweb.yars.nx.Node) values into Integers and also provides decode functionality
 */
public class NodeEncoder {
    public int counter;
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
    
    public int getEncodedNode(Node val) {
        int toReturn;
        if (reverseTable.get(val) == null) {
            toReturn = -9999;
        } else {
            toReturn = reverseTable.get(val);
        }
        return toReturn;
    }
    
    public boolean remove(int val) {
        boolean returnVal = true;
        if (table.containsKey(val)) {
            this.reverseTable.remove(this.table.get(val));
            this.table.remove(val);
        } else {
            returnVal = false;
        }
        return returnVal;
    }
    
    public Node decode(int val) {
        return this.table.get(val);
    }
    
    public HashMap<Integer, Node> getTable() {
        return this.table;
    }
    
    public HashMap<Node, Integer> getReverseTable() {
        return this.reverseTable;
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
    
    public boolean isNodeExists(Node node) {
        return reverseTable.get(node) != null;
    }
}
