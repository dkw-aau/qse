package cs.utils.encoders;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class encodes the String values into Integers and also provides decode functionality
 */
public class ConcurrentEncoder {
    int counter;
    ConcurrentHashMap<Integer, String> table;
    ConcurrentHashMap<String, Integer> reverseTable;
    
    public ConcurrentEncoder() {
        this.counter = -1;
        this.table = new ConcurrentHashMap<>();
        this.reverseTable = new ConcurrentHashMap<>();
    }
    
    public int encode(String val) {
        if (reverseTable.containsKey(val)) {
            return reverseTable.get(val);
        } else {
            this.counter++;
            table.put(counter, val);
            reverseTable.put(val, counter);
            return counter;
        }
    }
    
    public boolean isEncoded(String val) {
        return reverseTable.containsKey(val);
    }
    
    
    public ConcurrentHashMap<Integer, String> getTable() {
        return table;
    }
    
    public String decode(int val) {
        return this.table.get(val);
    }
    
    public ConcurrentHashMap<String, Integer> getRevTable() {
        return reverseTable;
    }
}
