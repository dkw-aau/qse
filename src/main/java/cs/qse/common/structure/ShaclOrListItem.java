package cs.qse.common.structure;

public class ShaclOrListItem {
    String nodeKind;
    String dataTypeOrClass;
    Integer support;
    Double confidence;
    Boolean pruneFlag = false;
    Boolean supportPruneFlag = false;
    Boolean confidencePruneFlag = false;
    
    public Boolean getSupportPruneFlag() {
        return supportPruneFlag;
    }
    
    public void setSupportPruneFlag(Boolean supportPruneFlag) {
        this.supportPruneFlag = supportPruneFlag;
    }
    
    public Boolean getConfidencePruneFlag() {
        return confidencePruneFlag;
    }
    
    public void setConfidencePruneFlag(Boolean confidencePruneFlag) {
        this.confidencePruneFlag = confidencePruneFlag;
    }
    
    public Boolean getPruneFlag() {return pruneFlag;}
    
    public void setPruneFlag(Boolean pruneFlag) {this.pruneFlag = pruneFlag;}
    
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
    
    public String getConfidenceInPercentage() {
        if(this.confidence!=null){
            int c = (int) (confidence * 100);
            return c + " %";
        } else {
            return "-";
        }
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    
}
