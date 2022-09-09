package cs.qse;

/**
 * This class is called Support Confidence (SuppConf) Computer Class, used to get or set the support/confidence of shape
 * constraints
 */
public class SupportConfidence {
    Integer support = 0;
    Double confidence = 0D;
    
    public SupportConfidence() {}
    
    public SupportConfidence(Integer support) {
        this.support = support;
    }
    
    public SupportConfidence(Integer s, Double c) {
        this.support = s;
        this.confidence = c;
    }
    
    public Integer getSupport() {
        return support;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setSupport(Integer support) {
        this.support = support;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
}
