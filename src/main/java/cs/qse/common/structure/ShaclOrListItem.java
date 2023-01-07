package cs.qse.common.structure;

public class ShaclOrListItem {
    String nodeKind;
    String dataTypeOrClass;
    Integer support;
    Double confidence;
    
    public String getNodeKind() {
        return nodeKind;
    }
    
    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }
    
    public String getDataTypeOrClass() {
        return dataTypeOrClass;
    }
    
    public void setDataTypeOrClass(String dataTypeOrClass) {
        this.dataTypeOrClass = dataTypeOrClass;
    }
    
    public Integer getSupport() {
        return support;
    }
    
    public void setSupport(Integer support) {
        this.support = support;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    
}
