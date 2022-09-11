package cs.qse.common.encoders;

public interface Encoder {
    
    int encode(String val);
    
    public String decode(int val);
    
}
