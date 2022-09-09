package cs.qse.filebased.sampling;

public class BinaryNode {
    public int id;
    public int scope;
    public BinaryNode left;
    public BinaryNode right;
    
    public BinaryNode(int id, int scope) {
        this.id = id;
        this.scope = scope;
        left = null;
        right = null;
    }
    
    
}
