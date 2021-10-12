package cs.qse;

/**
 * Support Confidence (SC) Computer Class
 */
public class SC {
    Integer support = 0;
    Double confidence = 0D;
    
    public SC() {}
    
    public SC(Integer support) {
        this.support = support;
    }
    
    public SC(Integer s, Double c) {
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
