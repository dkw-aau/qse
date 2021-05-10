package cs.utils;

import java.util.HashMap;

public class Encoder {
    int counter;
    HashMap<Integer, String> table;
    
    public Encoder() {
        this.counter = -1;
        this.table = new HashMap<>();
    }
    
    public int encode(String val) {
        counter++;
        table.put(counter, val);
        return counter;
    }
    
    public String decode(int val) {
        return this.table.get(val);
    }
}
