package cs.qse.sampling;

import org.semanticweb.yars.nx.Node;

public interface ReservoirSampling {
    
    void sample(Node[] nodes);
    
    void replace(int candidateIndex, Node[] nodes);
}
