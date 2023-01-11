package cs.qse.common.structure;

import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class PS {
    IRI iri;
    String path;
    String nodeKind;
    
    String dataTypeOrClass;
    Integer support;
    Double confidence;
    List<ShaclOrListItem> shaclOrListItems;
    
    Boolean hasOrList = false;
    Boolean pruneFlag = false;
    
    Boolean supportPruneFlag = false;
    Boolean confidencePruneFlag = false;
    
    
    public Boolean getSupportPruneFlag() {return supportPruneFlag;}
    
    public void setSupportPruneFlag(Boolean supportPruneFlag) {this.supportPruneFlag = supportPruneFlag;}
    
    public Boolean getConfidencePruneFlag() {return confidencePruneFlag;}
    
    public void setConfidencePruneFlag(Boolean confidencePruneFlag) {this.confidencePruneFlag = confidencePruneFlag;}
    
    
    public String getLocalNameFromIri() {
        return iri.getLocalName();
    }
    
    public Boolean getPruneFlag() {
        return pruneFlag;
    }
    
    public void setPruneFlag(Boolean pruneFlag) {
        this.pruneFlag = pruneFlag;
    }
    
    public IRI getIri() {
        return iri;
    }
    
    public void setIri(IRI iri) {
        this.iri = iri;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
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
    
    public Boolean getHasOrList() {
        return hasOrList;
    }
    
    public void setHasOrList(Boolean hasOrList) {
        this.hasOrList = hasOrList;
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
    
    public List<ShaclOrListItem> getShaclOrListItems() {
        return shaclOrListItems;
    }
    
    public void setShaclOrListItems(List<ShaclOrListItem> shaclOrListItems) {
        this.shaclOrListItems = shaclOrListItems;
    }
    
}


